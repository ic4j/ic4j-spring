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

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.agent.Agent;
import org.ic4j.agent.AgentBuilder;
import org.ic4j.agent.ProxyBuilder;
import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.http.ReplicaApacheHttpTransport;
import org.ic4j.agent.identity.Identity;
import org.ic4j.management.CanisterSettings;
import org.ic4j.management.CanisterStatusRequest;
import org.ic4j.management.CanisterStatusResponse;
import org.ic4j.management.CreateCanisterRequest;
import org.ic4j.management.DeleteCanisterRequest;
import org.ic4j.management.DepositCyclesRequest;
import org.ic4j.management.InstallCodeRequest;
import org.ic4j.management.ManagementError;
import org.ic4j.management.ManagementProxy;
import org.ic4j.management.Mode;
import org.ic4j.management.ProvisionalCreateCanisterWithCyclesRequest;
import org.ic4j.management.ProvisionalTopUpCanisterRequest;
import org.ic4j.management.StartCanisterRequest;
import org.ic4j.management.StopCanisterRequest;
import org.ic4j.management.UninstallCodeRequest;
import org.ic4j.management.UpdateSettingsRequest;
import org.ic4j.types.Principal;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
@Configuration
public final class ManagementService {	
	
	ManagementProxy managementProxy;
	
	public ManagementService(Identity identity,Principal managementCanister, Principal effectiveCanister, Environment env) throws ManagementError, URISyntaxException {
		
		String icLocation = env.getProperty("ic.location");

        boolean isLocal = true;

        try {
            isLocal = Boolean.parseBoolean(env.getProperty("ic.local"));
        } catch (Exception e) {
        }
        
        ReplicaTransport transport = ReplicaApacheHttpTransport.create(icLocation);
        
		Agent agent = new AgentBuilder().transport(transport).identity(identity).build();
		
		if(isLocal) 
            agent.fetchRootKey();

		this.managementProxy = ProxyBuilder
				.create(agent, managementCanister)
				.effectiveCanisterId(effectiveCanister)
				.getProxy(ManagementProxy.class);

	}

	@Async
	public CompletableFuture<Principal> createCanister(Optional<CanisterSettings> settings) {
		return this.createCanister(settings, Optional.empty());
	}

	 
	@Async
	public CompletableFuture<Principal> createCanister(Optional<CanisterSettings> settings, Optional<BigInteger> senderCanisterVersion) {
		CompletableFuture<Principal> response = new CompletableFuture<Principal>(); 
		
		CreateCanisterRequest createCanisterRequest = new CreateCanisterRequest();
		createCanisterRequest.settings = settings;
		createCanisterRequest.senderCanisterVersion = senderCanisterVersion;
		managementProxy.createCanister(createCanisterRequest).whenComplete((createCanisterResponse, ex) -> {
			if (ex == null) 
				if (createCanisterResponse != null) 
						response.complete(createCanisterResponse.canisterId);
				else
					response.completeExceptionally(new ManagementError("Empty Response"));
			else
				response.completeExceptionally(new ManagementError(ex));

		});
		
		return response;
	}

	@Async
	public void updateSettings(Principal canisterId, CanisterSettings settings) {
		this.updateSettings(canisterId, settings, Optional.empty());
	}
	
	@Async
	public void updateSettings(Principal canisterId, CanisterSettings settings, Optional<BigInteger> senderCanisterVersion) {
		UpdateSettingsRequest updateSettingsRequest = new UpdateSettingsRequest();
		updateSettingsRequest.canisterId = canisterId;
		updateSettingsRequest.settings = settings;
		updateSettingsRequest.senderCanisterVersion = senderCanisterVersion;
		
		managementProxy.updateSettings(updateSettingsRequest);
	}

	@Async
	public void installCode(Principal canisterId, Mode mode, byte[] wasmModule, byte[] arg) {
		this.installCode(canisterId, mode, wasmModule, arg, Optional.empty());
	}
	
	@Async
	public void installCode(Principal canisterId, Mode mode, byte[] wasmModule, byte[] arg, Optional<BigInteger> senderCanisterVersion) {
		InstallCodeRequest installCodeRequest = new InstallCodeRequest();
		installCodeRequest.canisterId = canisterId;
		installCodeRequest.mode = mode;
		installCodeRequest.wasmModule = wasmModule;
		installCodeRequest.arg = arg;
		if(installCodeRequest.arg == null)
			installCodeRequest.arg = ArrayUtils.EMPTY_BYTE_ARRAY;
		installCodeRequest.senderCanisterVersion = senderCanisterVersion;
		managementProxy.installCode(installCodeRequest);
	}

	@Async
	public void uninstallCode(Principal canisterId) {
		this.uninstallCode(canisterId, Optional.empty());
	}
	
	@Async
	public void uninstallCode(Principal canisterId, Optional<BigInteger> senderCanisterVersion) {
		UninstallCodeRequest uninstallCodeRequest = new UninstallCodeRequest();
		uninstallCodeRequest.canisterId = canisterId;
		uninstallCodeRequest.senderCanisterVersion = senderCanisterVersion;
		managementProxy.uninstallCode(uninstallCodeRequest);
	}	

	@Async
	public void deleteCanister(Principal canisterId) {
		DeleteCanisterRequest deleteCanisterRequest = new DeleteCanisterRequest();
		deleteCanisterRequest.canisterId = canisterId;
		managementProxy.deleteCanister(deleteCanisterRequest);
	}
	
	@Async
	public void startCanister(Principal canisterId) {
		StartCanisterRequest startCanisterRequest = new StartCanisterRequest();
		startCanisterRequest.canisterId = canisterId;
		managementProxy.startCanister(startCanisterRequest);
	}
	
	@Async
	public void stopCanister(Principal canisterId) {
		StopCanisterRequest stopCanisterRequest = new StopCanisterRequest();
		stopCanisterRequest.canisterId = canisterId;
		managementProxy.stopCanister(stopCanisterRequest);
	}
	
	@Async
	public void depositCycles(Principal canisterId) {
		DepositCyclesRequest depositCyclesRequest = new DepositCyclesRequest();
		depositCyclesRequest.canisterId = canisterId;
		managementProxy.depositCycles(depositCyclesRequest);
	}	
	
	@Async
	public CompletableFuture<CanisterStatusResponse> canisterStatus(Principal canisterId) {
		CanisterStatusRequest canisterStatusRequest = new CanisterStatusRequest();
		canisterStatusRequest.canisterId = canisterId;
		return managementProxy.canisterStatus(canisterStatusRequest);
	}	
	
	@Async
	public CompletableFuture<byte[]> rawRand() {
		return managementProxy.rawRand();
	}	
	
	@Async
	public CompletableFuture<Principal> provisionalCreateCanisterWithCycles(Optional<CanisterSettings> settings, Optional<BigInteger> amount) {
		CompletableFuture<Principal> response = new CompletableFuture<Principal>(); 
		
		ProvisionalCreateCanisterWithCyclesRequest provisionalCreateCanisterWithCyclesRequest = new ProvisionalCreateCanisterWithCyclesRequest();
		provisionalCreateCanisterWithCyclesRequest.settings = settings;
		provisionalCreateCanisterWithCyclesRequest.amount = amount;
		managementProxy.provisionalCreateCanisterWithCycles(provisionalCreateCanisterWithCyclesRequest).whenComplete((createCanisterResponse, ex) -> {
			if (ex == null) 
				if (createCanisterResponse != null) 
						response.complete(createCanisterResponse.canisterId);
				else
					response.completeExceptionally(new ManagementError("Empty Response"));
			else
				response.completeExceptionally(new ManagementError(ex));

		});
		
		return response;
		
	}
	
	@Async
	public void provisionalTopUpCanister(Principal canisterId, BigInteger amount) {
		ProvisionalTopUpCanisterRequest provisionalTopUpCanisterRequest = new ProvisionalTopUpCanisterRequest();
		provisionalTopUpCanisterRequest.canisterId = canisterId;
		provisionalTopUpCanisterRequest.amount = amount;
		managementProxy.provisionalTopUpCanister(provisionalTopUpCanisterRequest);
	}	
}
