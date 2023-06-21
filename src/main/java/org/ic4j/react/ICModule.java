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
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ic4j.agent.Agent;
import org.ic4j.agent.AgentBuilder;
import org.ic4j.agent.FuncProxy;
import org.ic4j.agent.ProxyBuilder;
import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.Response;
import org.ic4j.agent.ServiceProxy;
import org.ic4j.agent.Waiter;
import org.ic4j.agent.annotations.Canister;
import org.ic4j.agent.annotations.EffectiveCanister;
import org.ic4j.agent.annotations.Transport;
import org.ic4j.agent.http.ReplicaOkHttpTransport;
import org.ic4j.agent.identity.AnonymousIdentity;
import org.ic4j.agent.identity.BasicIdentity;
import org.ic4j.agent.identity.Identity;
import org.ic4j.agent.identity.Secp256k1Identity;
import org.ic4j.candid.ObjectSerializer;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Mode;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.ic4j.types.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.List;

public abstract class ICModule extends ReactContextBaseJavaModule {
	ServiceProxy serviceProxy;
	Agent agent;

	Identity identity;

	boolean loadIDL = false;

	protected boolean isLocal = false;

	boolean disableRangeCheck = false;

	Path idlFile;

	IDLType serviceType;

	{
		if (Security.getProvider("BC") != null)
			Security.removeProvider("BC");

		Security.addProvider(new BouncyCastleProvider());
	}

	public ICModule(ReactApplicationContext context) {
		super(context);
	}
	
	public ICModule(ReactApplicationContext context, Identity identity) {
		super(context);
		this.identity = identity;
	}	

	protected ICModule(ReactApplicationContext context, String location, String canisterId, String effectiveCanisterId,
			String identityFile) throws URISyntaxException, NoSuchAlgorithmException {
		super(context);
		this.init(location, canisterId, effectiveCanisterId, identityFile, null);
	}

	protected ICModule(ReactApplicationContext context, String location, String canisterId, String effectiveCanisterId,
			String identityFile, Waiter waiter) throws URISyntaxException, NoSuchAlgorithmException {
		super(context);
		this.init(location, canisterId, effectiveCanisterId, identityFile, waiter);
	}

	@ReactMethod
	public void connect(String location, String canisterId, String effectiveCanisterId, String identityFileName,
			Promise promise) {
		try {
			this.init(location, canisterId, effectiveCanisterId, identityFileName, null);
			promise.resolve(null);
		} catch (URISyntaxException e) {
			promise.reject(e);
		}
	}

	protected void init(String location, String canisterId, String effectiveCanisterId, String identityFile,
			Waiter waiter) throws URISyntaxException {

		ReactApplicationContext context = this.getReactApplicationContext();

		Identity identity;

		if (this.identity != null)
			identity = this.identity;
		else
			identity = new AnonymousIdentity();

		if (this.getClass().isAnnotationPresent(org.ic4j.agent.annotations.Agent.class)) {
			org.ic4j.agent.annotations.Agent agentAnnotation = this.getClass()
					.getAnnotation(org.ic4j.agent.annotations.Agent.class);

			Transport transportAnnotation = agentAnnotation.transport();

			if (location == null)
				location = transportAnnotation.url();

			if (this.identity == null) {
				org.ic4j.agent.annotations.Identity identityAnnotation = agentAnnotation.identity();

				Path path = null;

				File fileDirectory = context.getDataDir();

				if (identityFile != null) {
					path = Paths.get(fileDirectory + File.separator + identityFile);
				}

				switch (identityAnnotation.type()) {
				case ANONYMOUS:
					identity = new AnonymousIdentity();
					break;
				case BASIC:
					if (identityFile == null)
						path = Paths.get(fileDirectory.getAbsolutePath(), identityAnnotation.pem_file());

					identity = BasicIdentity.fromPEMFile(path);
					break;
				case SECP256K1:
					if (identityFile == null)
						path = Paths.get(fileDirectory.getAbsolutePath(), identityAnnotation.pem_file());

					identity = Secp256k1Identity.fromPEMFile(path);
					break;
				}
			}
		}
		ReplicaTransport transport = ReplicaOkHttpTransport.create(location);

		this.agent = new AgentBuilder().transport(transport).identity(identity).build();

		Principal canister = null;
		Principal effectiveCanister = null;

		if (effectiveCanisterId != null)
			effectiveCanister = Principal.fromString(effectiveCanisterId);

		if (effectiveCanister == null) {
			if (this.getClass().isAnnotationPresent(EffectiveCanister.class)) {
				EffectiveCanister effectiveCanisterAnnotation = this.getClass().getAnnotation(EffectiveCanister.class);

				effectiveCanister = Principal.fromString(effectiveCanisterAnnotation.value());
			}
		}

		if (canisterId == null) {
			if (this.getClass().isAnnotationPresent(Canister.class)) {
				Canister canisterAnnotation = this.getClass().getAnnotation(Canister.class);

				canister = Principal.fromString(canisterAnnotation.value());

				if (effectiveCanister == null)
					effectiveCanister = canister.clone();
			}
		} else
			canister = Principal.fromString(canisterId);

		Service service = new Service(canister);

		if (this.getClass().isAnnotationPresent(org.ic4j.agent.annotations.IDLFile.class)) {
			String idlFileName = this.getClass().getAnnotation(org.ic4j.agent.annotations.IDLFile.class).value();

			try {
				InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(idlFileName);
				Path idlCopy = Paths.get(context.getCacheDir() + File.separator + idlFileName);
				Files.copy(inputStream, idlCopy, StandardCopyOption.REPLACE_EXISTING);

				inputStream.close();

				this.idlFile = idlCopy;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		if (this.getClass().isAnnotationPresent(org.ic4j.agent.annotations.Properties.class)) {
			org.ic4j.agent.annotations.Properties properties = this.getClass()
					.getAnnotation(org.ic4j.agent.annotations.Properties.class);

			if (properties.disableRangeCheck())
				this.disableRangeCheck = true;
			if (properties.loadIDL())
				this.loadIDL = true;
		}

		if (this.isLocal)
			this.agent.fetchRootKey();

		this.agent.setVerify(this.disableRangeCheck);

		ProxyBuilder proxyBuilder = ProxyBuilder.create(agent).effectiveCanisterId(effectiveCanister)
				.idlFile(this.idlFile).loadIDL(this.loadIDL).waiter(waiter);

		this.serviceProxy = proxyBuilder.getServiceProxy(service);

	}
	
	protected <T> void update(Promise promise, String methodName, Class<T> clazz, Object... args) {	
		this.call(promise, methodName, clazz, null, args);
	}
	
	protected <T> void query(Promise promise, String methodName, Class<T> clazz, Object... args) {	
		this.call(promise, methodName, clazz, new Mode[]{Mode.QUERY}, args);
	}	
	
	protected <T> void oneway(Promise promise, String methodName,  Object... args) {	
		this.call(promise, methodName, Void.class, new Mode[]{Mode.ONEWAY}, args);
	}	

	protected <T> void call(Promise promise, String methodName, Class<T> clazz, Mode[] modes, Object... args) {
		try {
			Func funcValue = new Func(methodName);

			FuncProxy<T> funcProxy = this.serviceProxy.getFuncProxy(funcValue, modes);

			ObjectSerializer[] serializers = new ObjectSerializer[args.length];

			for (int i = 0; i < args.length; i++)
				serializers[i] = ReactSerializer.create();

			funcProxy.setSerializers(serializers);

			ReactDeserializer deserializer = ReactDeserializer.create();

			if (clazz != null) {
				if (IDLArgs.class.isAssignableFrom(clazz)) {
					funcProxy.setResponseClass(IDLArgs.class);

					IDLArgs outArgs = (IDLArgs) funcProxy.call(args);

					WritableArray outArray = Arguments.createArray();

					if (outArgs != null) {
						List<IDLValue> outValues = outArgs.getArgs();

						for (IDLValue outValue : outValues) {
							Object item = outValue.getValue(ReactDeserializer.create(outValue.getIDLType()),
									Object.class);
							deserializer.pushWritableArrayItem(outArray, item);
						}

						promise.resolve(outArray);
					} else
						promise.resolve(null);

					return;
				} else {
					funcProxy.setDeserializer(deserializer);

					if (Void.class.isAssignableFrom(clazz))
						funcProxy.setResponseClass(Response.class);
					else
						funcProxy.setResponseClass(clazz);
				}
			}

			T response = funcProxy.call(args);

			if (Void.class.isAssignableFrom(clazz))
				promise.resolve(null);
			else {
				if (promise != null) {
					if ((clazz == null))
						promise.resolve(null);
					else
						promise.resolve(response);

				}
			}
		} catch (Exception e) {
			if (promise != null)
				promise.reject(e.getMessage(), e);
		}
	}
}