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

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;

import org.ic4j.candid.CandidError;
import org.ic4j.candid.ObjectSerializer;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Principal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;


public class ReactSerializer implements ObjectSerializer {
	Optional<IDLType> idlType = Optional.empty();

	public static ReactSerializer create(IDLType idlType) {
		ReactSerializer deserializer = new ReactSerializer();
		deserializer.idlType = Optional.ofNullable(idlType);
		return deserializer;

	}

	public static ReactSerializer create() {
		ReactSerializer deserializer = new ReactSerializer();
		return deserializer;
	}
	
	public void setIDLType(IDLType idlType)
	{
		this.idlType = Optional.ofNullable(idlType);
	}	

	@Override
	public IDLValue serialize(Object value) {
		return this.getIDLValue(this.idlType, value);
	}

	IDLValue getPrimitiveIDLValue(Type type, Object value) {
		IDLValue result = IDLValue.create(null);

		if (value == null)
			return result;

		BigDecimal number = new BigDecimal(0);

		if(value instanceof Double)
			number = new BigDecimal((Double) value, MathContext.DECIMAL64);
	
		switch (type) {
		case BOOL:
			result = IDLValue.create(value, type);
			break;
		case INT:
			result = IDLValue.create(number.toBigIntegerExact(), type);
			break;
		case INT8:
			result = IDLValue.create(number.byteValueExact(), type);
			break;
		case INT16:
			result = IDLValue.create(number.shortValueExact(), type);
			break;
		case INT32:
			result = IDLValue.create(number.intValueExact(), type);
			break;
		case INT64:
			result = IDLValue.create(number.longValueExact(), type);
			break;
		case NAT:
			result = IDLValue.create(number.toBigIntegerExact(), type);
			break;
		case NAT8:
			result = IDLValue.create(number.byteValueExact(), type);
			break;
		case NAT16:
			result = IDLValue.create(number.shortValueExact(), type);
			break;
		case NAT32:
			result = IDLValue.create(number.intValueExact(), type);
			break;
		case NAT64:
			result = IDLValue.create(number.longValueExact(), type);
			break;
		case FLOAT32:
			result = IDLValue.create(((Double) value).floatValue(), type);
			break;
		case FLOAT64:
			result = IDLValue.create((Double) value, type);
			break;
		case TEXT:
			result = IDLValue.create(value.toString(), type);
			break;
		case PRINCIPAL:
			result = IDLValue.create(Principal.fromString(value.toString()));
			break;
		case EMPTY:
			result = IDLValue.create(null, type);
		case NULL:
			result = IDLValue.create(null, type);
			break;
		}

		return result;
	}

	Type getType(Object value) {
		if (value == null)
			return Type.NULL;

		if(value instanceof Boolean)	
			return Type.BOOL;

		if(value instanceof String)	
			return Type.TEXT;	

		if(value instanceof Double)	
			return Type.FLOAT64;			

		if (value instanceof ReadableArray)
			return Type.VEC;

		if (value instanceof ReadableMap)
			return Type.RECORD;

		return Type.NULL;
	}

	Object getReadableArrayItem(ReadableArray readableArray, int i)
	{
		ReadableType type = readableArray.getType(i);

		Object item = null;

		switch (type) {
		  case Null:
			item = null;
			break;
		  case Boolean:
		  	item = readableArray.getBoolean(i);
			break;
		  case Number:
		  	item = readableArray.getDouble(i);
			break;
		  case String:
		  	item = readableArray.getString(i);
			break;
		  case Map:
		  	item = readableArray.getMap(i);
			break;
		  case Array:
		  	item = readableArray.getArray(i);
			break;
		}

		return item;
	}

	Object getReadableMapItem(ReadableMap readableMap, String key)
	{
		ReadableType type = readableMap.getType(key);

		Object item = null;

		switch (type) {
			case Null:
				item = null;
				break;
			case Boolean:
				item = readableMap.getBoolean(key);
				break;
			case Number:
				item = readableMap.getDouble(key);
				break;
			case String:
				item = readableMap.getString(key);
				break;
			case Map:
				item = readableMap.getMap(key);
				break;
			case Array:
				item = readableMap.getArray(key);
				break;
		}

		return item;
	}

	IDLValue getIDLValue(Optional<IDLType> expectedIdlType, Object value) {
		Type type;
		if (expectedIdlType.isPresent())
			type = expectedIdlType.get().getType();
		else
			type = this.getType(value);

		// handle Optional
		if (expectedIdlType.isPresent() &&  type == Type.OPT) {
			IDLValue innerValue = this.getIDLValue(Optional.ofNullable(expectedIdlType.get().getInnerType()), value);
			return IDLValue.create(Optional.ofNullable(innerValue.getValue()), expectedIdlType.get());
		}

		// handle null values
		if (value == null)
			return IDLValue.create(value, Type.NULL);

		if(type == Type.NULL || type == Type.EMPTY)
			return IDLValue.create(null, type);
			
		// handle primitives
		if (value instanceof Boolean || value instanceof Double )
			return this.getPrimitiveIDLValue(type, value);

		if ((type != Type.VEC && type != Type.VARIANT) && value instanceof String)
			return this.getPrimitiveIDLValue(type, value);

		// handle arrays
		if (type == Type.VEC) {
			IDLType innerIdlType = IDLType.createType(Type.NULL);

			if (expectedIdlType.isPresent())
				innerIdlType = expectedIdlType.get().getInnerType();

			if ( value instanceof String) {
				byte[] byteArrayValue = Base64.getDecoder().decode((String)value);
				return IDLValue.create(this.primitivesToBytes(byteArrayValue), IDLType.createType(type, innerIdlType));
			}

			if (value instanceof ReadableArray) {
				ReadableArray readableArray = (ReadableArray) value;
				Object[] arrayValue = new Object[readableArray.size()];

				for (int i = 0; i < readableArray.size(); i++) {
					IDLValue item = this.getIDLValue(Optional.ofNullable(innerIdlType), getReadableArrayItem(readableArray,i));

					arrayValue[i] = item.getValue();
					if (!expectedIdlType.isPresent())
						innerIdlType = item.getIDLType();
				}

				IDLType idlType;

				if (expectedIdlType.isPresent())
					idlType = expectedIdlType.get();
				else
					idlType = IDLType.createType(Type.VEC, innerIdlType);

				return IDLValue.create(arrayValue, idlType);
			}

			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
					"Cannot convert class " + value.getClass().getName() + " to VEC");

		}

		// handle Objects
		if (type == Type.RECORD || type == Type.VARIANT) {
			Map<Label, Object> valueMap = new TreeMap<Label, Object>();
			Map<Label, IDLType> typeMap = new TreeMap<Label, IDLType>();
			Map<Label, IDLType> expectedTypeMap = new TreeMap<Label, IDLType>();
			
			if (expectedIdlType.isPresent())
				expectedTypeMap = expectedIdlType.get().getTypeMap();
			
			if(value instanceof ReadableMap)
			{
				ReadableMap readableMap = (ReadableMap) value;
				ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
				while(iterator.hasNextKey()) {
					String key = iterator.nextKey();

					Object item = this.getReadableMapItem(readableMap,key);

					IDLType expectedItemIdlType;
	
					if (expectedTypeMap != null && expectedIdlType.isPresent())
						expectedItemIdlType = expectedTypeMap.get(Label.createNamedLabel(key));
					else
						expectedItemIdlType = IDLType.createType(this.getType(item));
	
					if (expectedItemIdlType == null)
						continue;
	
					IDLValue itemIdlValue = this.getIDLValue(Optional.ofNullable(expectedItemIdlType), item);
	
					typeMap.put(Label.createNamedLabel((String) key), itemIdlValue.getIDLType());
					valueMap.put(Label.createNamedLabel((String) key), itemIdlValue.getValue());
				}
			}else if(value instanceof String && type == Type.VARIANT)
			{
				typeMap.put(Label.createNamedLabel((String) value), IDLType.createType(Type.VARIANT));
				valueMap.put(Label.createNamedLabel((String) value), null);
			}

			IDLType idlType = IDLType.createType(type, typeMap);
			IDLValue idlValue = IDLValue.create(valueMap, idlType);

			return idlValue;
		}
		
		if (type == Type.OPT)
		{
			if (expectedIdlType.isPresent())
			{
				if(value instanceof ReadableArray && ((ReadableArray)value).size() == 0)
					return IDLValue.create(Optional.empty(), expectedIdlType.get());
				
				IDLValue itemIdlValue = this.getIDLValue(Optional.ofNullable(expectedIdlType.get().getInnerType()), value);
				
				return IDLValue.create(Optional.ofNullable(itemIdlValue.getValue()), expectedIdlType.get());
			}
			else
			{
				if(value == null)
					return IDLValue.create(Optional.empty(), IDLType.createType(Type.OPT));
				
				if(value instanceof ReadableArray && ((ReadableArray)value).size() == 0)
					return IDLValue.create(Optional.empty(), IDLType.createType(Type.OPT));
				
				IDLValue itemIdlValue = this.getIDLValue(Optional.ofNullable( IDLType.createType(Type.OPT)), value);
				
				
				return IDLValue.create(Optional.ofNullable(itemIdlValue.getValue()), IDLType.createType(Type.OPT));							
			}							
		}

		throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Cannot convert type " + type.name());

	}

	static IDLType getIDLType(Class valueClass) {
		if(valueClass == null)
			return IDLType.createType(Type.NULL);

		if(Boolean.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.BOOL);

		if(String.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.TEXT);

		if(Double.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.FLOAT64);

		if(ReadableArray.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.VEC);

		if(ReadableMap.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.RECORD);

		return IDLType.createType(Type.NULL);
	}

	Byte[] primitivesToBytes(byte[] bytes)
	{
		Byte[] oBytes = new Byte[bytes.length];
		for(int i = 0; i < bytes.length; i++){
			oBytes[i] = bytes[i];
		}
		return oBytes;
	}
}
