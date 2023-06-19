/*
 * Copyright 2023 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.ic4j.react;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.ic4j.candid.ByteUtils;
import org.ic4j.candid.CandidError;
import org.ic4j.candid.ObjectDeserializer;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Principal;

public class ReactDeserializer implements ObjectDeserializer {
	Optional<IDLType> idlType = Optional.empty();

	public static ReactDeserializer create(IDLType idlType) {
		ReactDeserializer deserializer = new ReactDeserializer();
		deserializer.idlType = Optional.ofNullable(idlType);

		return deserializer;
	}

	public static ReactDeserializer create() {
		ReactDeserializer deserializer = new ReactDeserializer();
		return deserializer;
	}
	
	public void setIDLType(IDLType idlType) {
		this.idlType = Optional.ofNullable(idlType);
	}
	
	public Class<?> getDefaultResponseClass() {
		return Object.class;
	}	

	@Override
	public <T> T deserialize(IDLValue value, Class<T> clazz) {
		if (clazz != null) {
				if(!this.idlType.isPresent())
					this.idlType = Optional.ofNullable(ReactSerializer.getIDLType(clazz));

				return (T) this.getValue(value.getIDLType(), this.idlType, value.getValue(), clazz);
		} else
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Class is not defined");
	}

	Object getPrimitiveValue(Type type, Object value) {
		Object result = null;

		if (value == null)
			return null;

		BigDecimal number = new BigDecimal(0);

			if(value instanceof Number) {
				if(value instanceof BigDecimal)
					number = (BigDecimal)value;
				else if(value instanceof BigInteger)
					number = new BigDecimal((BigInteger)value);
				else if(value instanceof Byte)
					number = new BigDecimal((Byte)value);
				else if(value instanceof Short)
					number = new BigDecimal((Short)value);
				else if(value instanceof Integer)
					number = new BigDecimal((Integer)value);
				else if(value instanceof Long)
					number = new BigDecimal((Long)value);
				else if(value instanceof Double)
					number = new BigDecimal((Double)value);
				else if(value instanceof Float)
					number = new BigDecimal((Float)value);
			}

		switch (type) {
		case BOOL:
			result = (Boolean) value;
			break;
		case INT:
		case INT8:
		case INT16:
		case INT32:
		case INT64:
		case NAT:
		case NAT8:
		case NAT16:
		case NAT32:
		case NAT64:
		case FLOAT32:
		case FLOAT64:
			result = number.doubleValue();
			break;
		case TEXT:
			result = (String) value;
			break;
		case EMPTY:
			result = null;
			break;
		case PRINCIPAL:
			Principal principal = (Principal) value;
			result = principal.toString();
			break;
		}

		return result;
	}

	void pushWritableArrayItem(WritableArray writableArray, Object value)
	{
		if (value == null) {
			writableArray.pushNull();
			return;
		}
		if(value instanceof Boolean) {
			writableArray.pushBoolean((Boolean) value);
			return;
		}
		if(value instanceof String) {
			writableArray.pushString((String) value);
			return;
		}
		if(value instanceof Double) {
			writableArray.pushDouble((Double) value);
			return;
		}
		if (value instanceof ReadableArray) {
			writableArray.pushArray((ReadableArray) value);
			return;
		}
		if (value instanceof ReadableMap)
			writableArray.pushMap((ReadableMap) value);
	}

	void putWritableMapItem(WritableMap writableMap,String key,  Object value)
	{
		if (value == null) {
			writableMap.putNull(key);
			return;
		}
		if(value instanceof Boolean) {
			writableMap.putBoolean(key, (Boolean) value);
			return;
		}
		if(value instanceof String) {
			writableMap.putString(key, (String) value);
			return;
		}
		if(value instanceof Double) {
			writableMap.putDouble(key, (Double) value);
			return;
		}
		if (value instanceof ReadableArray) {
			writableMap.putArray(key,(ReadableArray) value);
			return;
		}
		if (value instanceof ReadableMap)
			writableMap.putMap(key, (ReadableMap) value);
	}

	<T> T getValue(IDLType idlType, Optional<IDLType> expectedIdlType, Object value, Class clazz) {
		if (value == null)
			return null;

		T result = null;

		Type type = Type.NULL;

		if (expectedIdlType.isPresent()) {
			type = expectedIdlType.get().getType();
			if (idlType != null)
				idlType = expectedIdlType.get();
		}
		else if(idlType != null)
			type = idlType.getType();

		if (type.isPrimitive())
			return (T) this.getPrimitiveValue(type, value);

		// handle VEC
		if (type == Type.VEC) {
			IDLType expectedInnerIDLType = null;
			IDLType innerIdlType = idlType.getInnerType();

			if (expectedIdlType.isPresent()) {
				expectedInnerIDLType = expectedIdlType.get().getInnerType();
				innerIdlType = expectedInnerIDLType;
			}

			// handle byte array
			if ( innerIdlType.getType() == Type.NAT8) {
				if (value instanceof Byte[])
					value = bytesToPrimitives((Byte[]) value);

				if(ReadableArray.class.isAssignableFrom(clazz)) {
					int[] intArrayValue = ByteUtils.toUnsignedIntegerArray((byte[]) value);

					WritableArray writableArray = Arguments.createArray();

					for (int item : intArrayValue) {
						Object itemValue = this.getValue(idlType.getInnerType(), Optional.ofNullable(expectedInnerIDLType), item, Double.class);
						this.pushWritableArrayItem(writableArray, itemValue);
					}

					return (T) writableArray;
				}
				else
					return (T) Base64.getEncoder().encodeToString((byte[]) value);
			}
			else {
				WritableArray writableArray = Arguments.createArray();

				Object[] arrayValue = (Object[]) value;

				for (Object item : arrayValue) {
					Object itemValue = this.getValue(idlType.getInnerType(), Optional.ofNullable(expectedInnerIDLType), item, Object.class);
					this.pushWritableArrayItem(writableArray, itemValue);
				}

				return (T) writableArray;
			}
		}

		// handle OPT
		if (type == Type.OPT) {
			Optional optionalValue = (Optional) value;

			if (optionalValue.isPresent()) {
				IDLType expectedInnerIDLType = null;

				if (expectedIdlType.isPresent())
					expectedInnerIDLType = expectedIdlType.get().getInnerType();

				return this.getValue(idlType.getInnerType(), Optional.ofNullable(expectedInnerIDLType),
						optionalValue.get(), clazz);
			} else
				return null;
		}

		if (type == Type.RECORD || type == Type.VARIANT) {
			Map<Label, Object> valueMap = (Map<Label, Object>) value;

			Map<Label, IDLType> typeMap = idlType.getTypeMap();

			Map<Label, IDLType> expectedTypeMap = new TreeMap<Label, IDLType>();

			if (expectedIdlType.isPresent() && expectedIdlType.get().getTypeMap() != null)
				expectedTypeMap = expectedIdlType.get().getTypeMap();

			Set<Label> labels = valueMap.keySet();

			Map<Long, Label> expectedLabels = new TreeMap<Long, Label>();

			for (Label entry : expectedTypeMap.keySet())
				expectedLabels.put(entry.getId(), entry);

			if(type == Type.VARIANT)
			{
				if(!valueMap.isEmpty())
				{
					String fieldName;

					Label label = valueMap.keySet().iterator().next();

					if (expectedTypeMap.containsKey(label)) {
						Label expectedLabel = expectedLabels.get(label.getId());

						fieldName = expectedLabel.getValue().toString();

						Object variantValue = valueMap.get(label);

						if(variantValue == null)
							return (T) fieldName;
						else
						{
							IDLType itemIdlType = typeMap.get(label);
							IDLType expectedItemIdlType = expectedTypeMap.get(label);

							WritableMap writableMap = Arguments.createMap();
							Object itemValue = this.getValue(itemIdlType, Optional.ofNullable(expectedItemIdlType),
									valueMap.get(label), Object.class);

							this.putWritableMapItem(writableMap,fieldName, itemValue);

							return (T) writableMap;
						}
					}
					else
					{
						Object variantValue = valueMap.get(label);

						if(variantValue == null)
							return (T) label.toString();
						else
						{
							IDLType itemIdlType = typeMap.get(label);

							WritableMap writableMap = Arguments.createMap();
							Object itemValue = this.getValue(itemIdlType, Optional.ofNullable(null),
									valueMap.get(label), Object.class);

							this.putWritableMapItem(writableMap,label.toString(), itemValue);

							return (T) writableMap;
						}
					}
				}
				else
					return null;
			}

			WritableMap writableMap = Arguments.createMap();

			for (Label label : labels) {

				String fieldName;

				IDLType itemIdlType = typeMap.get(label);

				IDLType expectedItemIdlType = null;

				if (expectedTypeMap.containsKey(label)) {
					expectedItemIdlType = expectedTypeMap.get(label);

					Label expectedLabel = expectedLabels.get(label.getId());

					fieldName = expectedLabel.getValue().toString();
				} else
					fieldName = label.getValue().toString();

				Object itemValue = this.getValue(itemIdlType, Optional.ofNullable(expectedItemIdlType),
						valueMap.get(label), Object.class);


				this.putWritableMapItem(writableMap,fieldName, itemValue);
			}

			return (T) writableMap;
		}
		throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Cannot convert type " + type.name());
	}


	byte[] bytesToPrimitives(Byte[] oBytes)
	{
		byte[] bytes = new byte[oBytes.length];
		for(int i = 0; i < oBytes.length; i++){
			bytes[i] = oBytes[i];
		}
		return bytes;
	}
}
