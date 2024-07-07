/*
 * Copyright 2024 Exilor Inc.
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

package org.ic4j.spring;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.ic4j.agent.Agent;
import org.ic4j.agent.AgentBuilder;
import org.ic4j.agent.FuncProxy;
import org.ic4j.agent.ProxyBuilder;
import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.annotations.Canister;
import org.ic4j.agent.annotations.EffectiveCanister;
import org.ic4j.agent.annotations.Transport;
import org.ic4j.agent.http.ReplicaApacheHttpTransport;
import org.ic4j.agent.identity.AnonymousIdentity;
import org.ic4j.agent.identity.BasicIdentity;
import org.ic4j.agent.identity.Identity;
import org.ic4j.agent.identity.Prime256v1Identity;
import org.ic4j.agent.identity.Secp256k1Identity;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;


public abstract class Service {
	@Autowired
	protected Environment env;
	
	Identity identity;

	boolean loadIDL = false;

	protected boolean isLocal = false;

	boolean disableRangeCheck = false;

	Path idlFile;

	IDLType serviceType;	

	protected ProxyBuilder proxyBuilder;
	
	private Class interfaceClass;
	

	final ResourceLoader resourceLoader;

	public Service(ResourceLoader resourceLoader) {
	        this.resourceLoader = resourceLoader;
	}
	
	protected void init(Class<?> clazz, String location, String canisterId, String effectiveCanisterId, Identity identity) throws URISyntaxException, IOException {
		if(clazz == null)
            throw new IllegalArgumentException("Interface class is required");
		
		this.interfaceClass = clazz;
		
		File fileDirectory = resourceLoader.getResource("file:").getFile();
		
		this.identity = identity;
		
		if (clazz.isAnnotationPresent(org.ic4j.agent.annotations.Agent.class)) {
			org.ic4j.agent.annotations.Agent agentAnnotation = clazz
					.getAnnotation(org.ic4j.agent.annotations.Agent.class);

			this.isLocal = agentAnnotation.fetchRootKey();
			
			Transport transportAnnotation = agentAnnotation.transport();		

			if (location == null)
				location = transportAnnotation.url();

			
			if (this.identity == null) {
				org.ic4j.agent.annotations.Identity identityAnnotation = agentAnnotation.identity();

				Path path = null;
				

				switch (identityAnnotation.type()) {
				case ANONYMOUS:
					identity = new AnonymousIdentity();
					break;
				case BASIC:
					path = Paths.get(fileDirectory.getAbsolutePath(), identityAnnotation.pem_file());

					identity = BasicIdentity.fromPEMFile(path);
					break;
				case SECP256K1:
					path = Paths.get(fileDirectory.getAbsolutePath(), identityAnnotation.pem_file());

					identity = Secp256k1Identity.fromPEMFile(path);
					break;
				case PRIME256V1:
					path = Paths.get(fileDirectory.getAbsolutePath(), identityAnnotation.pem_file());

					identity = Prime256v1Identity.fromPEMFile(path);
					break;
				default:
					identity = new AnonymousIdentity();
					break;					
				}
			}
		}
		
		ReplicaTransport transport = ReplicaApacheHttpTransport.create(location);

		Agent agent = new AgentBuilder().transport(transport).identity(identity).build();

		Principal canister = null;
		Principal effectiveCanister = null;

		if (effectiveCanisterId != null)
			effectiveCanister = Principal.fromString(effectiveCanisterId);

		if (effectiveCanister == null) {
			if (clazz.isAnnotationPresent(EffectiveCanister.class)) {
				EffectiveCanister effectiveCanisterAnnotation = clazz.getAnnotation(EffectiveCanister.class);

				effectiveCanister = Principal.fromString(effectiveCanisterAnnotation.value());
			}
		}

		if (canisterId == null) {
			if (clazz.isAnnotationPresent(Canister.class)) {
				Canister canisterAnnotation = clazz.getAnnotation(Canister.class);

				canister = Principal.fromString(canisterAnnotation.value());

				if (effectiveCanister == null)
					effectiveCanister = canister.clone();
			}
		} else
			canister = Principal.fromString(canisterId);

		org.ic4j.types.Service service = new org.ic4j.types.Service(canister);

		if (clazz.isAnnotationPresent(org.ic4j.agent.annotations.IDLFile.class)) {
			String idlFileName = clazz.getAnnotation(org.ic4j.agent.annotations.IDLFile.class).value();

			InputStream inputStream = clazz.getClassLoader().getResourceAsStream(idlFileName);
			Path idlFile = Paths.get(fileDirectory + File.separator  + idlFileName);


			this.idlFile = idlFile;
		}
		if (clazz.isAnnotationPresent(org.ic4j.agent.annotations.Properties.class)) {
			org.ic4j.agent.annotations.Properties properties = clazz
					.getAnnotation(org.ic4j.agent.annotations.Properties.class);

			if (properties.disableRangeCheck())
				this.disableRangeCheck = true;
			if (properties.loadIDL())
				this.loadIDL = true;
		}

		if (this.isLocal)
			agent.fetchRootKey();

		agent.setVerify(this.disableRangeCheck);

		this.proxyBuilder = ProxyBuilder.create(agent).effectiveCanisterId(effectiveCanister)
				.idlFile(this.idlFile).loadIDL(this.loadIDL);


	}		

	protected <T> T call( String methodName, Object... args) {
		try {
			Func funcValue = new Func(methodName);

			FuncProxy<T> funcProxy = this.proxyBuilder.getFuncProxy(funcValue, this.interfaceClass);

			T response = funcProxy.call(args);

			return response;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}	

}