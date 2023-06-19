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
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import org.apache.commons.io.IOUtils;
import org.ic4j.agent.Waiter;
import org.ic4j.agent.annotations.Agent;
import org.ic4j.agent.annotations.Canister;
import org.ic4j.agent.annotations.EffectiveCanister;
import org.ic4j.agent.annotations.IDLFile;
import org.ic4j.agent.annotations.Identity;
import org.ic4j.agent.annotations.IdentityType;
import org.ic4j.agent.annotations.Properties;
import org.ic4j.agent.annotations.Transport;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.types.Mode;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Agent(transport = @Transport(url = "http://10.0.2.2:4943"), identity = @Identity(type = IdentityType.SECP256K1))
@Canister("aaaaa-aa")
@EffectiveCanister("x5pps-pqaaa-aaaab-qadbq-cai")
@IDLFile("ic.did")
@Properties(loadIDL = true)

public final class ManagementModule extends ICModule {
	static int WAITER_TIMEOUT = 300;
	static int WAITER_SLEEP = 5;

	static Waiter waiter = Waiter.create(WAITER_TIMEOUT, WAITER_SLEEP);

	public ManagementModule(ReactApplicationContext context, String identityFile) throws URISyntaxException, NoSuchAlgorithmException {
		super(context);
		this.isLocal = true;

		this.init(null,null,null,identityFile,waiter);
	}

	public ManagementModule(ReactApplicationContext context,boolean isLocal, String location, String identityFile) throws URISyntaxException, NoSuchAlgorithmException {
		super(context);
		this.isLocal = isLocal;

		this.init(location,null,null,identityFile,waiter);
	}

	@Override
	public String getName() {
		return "ManagementModule";
	}

	@ReactMethod
	public void createCanister(ReadableMap settings, Double senderCanisterVersion,  Promise promise) {
		WritableMap canisterStatusRequest = Arguments.createMap();

		canisterStatusRequest.putMap("settings", settings);

		if(senderCanisterVersion == 0)
			canisterStatusRequest.putNull("sender_canister_version");
		else
			canisterStatusRequest.putDouble("sender_canister_version",senderCanisterVersion);

		this.call(promise,"create_canister",ReadableMap.class,null,canisterStatusRequest);
	}

	@ReactMethod
	public void updateSettings(String canisterId, ReadableMap settings,Double senderCanisterVersion, Promise promise) {
		WritableMap updateSettingsRequest = Arguments.createMap();

		updateSettingsRequest.putString("canister_id", canisterId);
		updateSettingsRequest.putMap("settings", settings);

		if(senderCanisterVersion == 0)
			updateSettingsRequest.putNull("sender_canister_version");
		else
			updateSettingsRequest.putDouble("sender_canister_version",senderCanisterVersion);

		this.call(promise,"update_settings",Void.class,null,updateSettingsRequest);
	}

	@ReactMethod
	public void installCode(String canisterId, String mode, ReadableArray wasmModule, String arg,Double senderCanisterVersion, Promise promise) {
		String idlArg = "";

		if(arg != null)
		{
			IDLArgs idlArgs = IDLArgs.fromIDL(arg);

			idlArg = Base64.getEncoder().encodeToString(idlArgs.toBytes());
		}

		WritableMap installCodeRequest = Arguments.createMap();

		installCodeRequest.putString("canister_id", canisterId);
		installCodeRequest.putString("mode",mode);
		installCodeRequest.putArray("wasm_module", wasmModule);
		installCodeRequest.putString("arg", idlArg);

		if(senderCanisterVersion == 0)
			installCodeRequest.putNull("sender_canister_version");
		else
			installCodeRequest.putDouble("sender_canister_version",senderCanisterVersion);

		this.call(promise,"install_code",Void.class,null, installCodeRequest);
	}

	@ReactMethod
	public void installCode(String canisterId, String mode, String wasmModule, String arg, Double senderCanisterVersion, Promise promise) {
		String idlArg = "";

		if(arg != null)
		{
			IDLArgs idlArgs = IDLArgs.fromIDL(arg);

			idlArg = Base64.getEncoder().encodeToString(idlArgs.toBytes());
		}

		WritableMap installCodeRequest = Arguments.createMap();

		installCodeRequest.putString("canister_id", canisterId);
		installCodeRequest.putString("mode",mode);
		installCodeRequest.putString("wasm_module", wasmModule);
		installCodeRequest.putString("arg", idlArg);

		if(senderCanisterVersion == 0)
			installCodeRequest.putNull("sender_canister_version");
		else
			installCodeRequest.putDouble("sender_canister_version",senderCanisterVersion);

		this.call(promise,"install_code",Void.class,null, installCodeRequest);
	}

	@ReactMethod
	public void installCodeFromClassLoader(String canisterId, String mode, String wasmModuleFile, String arg,Double senderCanisterVersion, Promise promise) {
		try {
			byte[] sourceBytes = new byte[0];
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(wasmModuleFile);
			sourceBytes = IOUtils.toByteArray(inputStream);

			String encodedString = Base64.getEncoder().encodeToString(sourceBytes);

			this.installCode(canisterId,mode,encodedString,arg,senderCanisterVersion,promise);
		} catch (IOException e) {
			promise.reject(e);
		}
	}

	@ReactMethod
	public void installCodeFromFile(String canisterId, String mode, String wasmModuleFile, String arg,Double senderCanisterVersion, Promise promise) {
		try {
			byte[] sourceBytes = new byte[0];
			Path path = Paths.get(this.getReactApplicationContextIfActiveOrWarn().getDataDir() + File.separator + wasmModuleFile);

			InputStream inputStream = new DataInputStream(new FileInputStream(path.toFile()));

			sourceBytes = IOUtils.toByteArray(inputStream);

			String encodedString = Base64.getEncoder().encodeToString(sourceBytes);

			this.installCode(canisterId,mode,encodedString,arg,senderCanisterVersion,promise);
		} catch (Exception e) {
			promise.reject(e);
		}
	}

	@ReactMethod
	public void uninstallCode(String canisterId, Promise promise) {
		WritableMap uninstallCodeRequest = Arguments.createMap();

		uninstallCodeRequest.putString("canister_id", canisterId);

		uninstallCodeRequest.putNull("sender_canister_version");

		this.call(promise,"uninstall_code",Void.class,null, uninstallCodeRequest);
	}

	@ReactMethod
	public void deleteCanister(String canisterId, Promise promise) {
		WritableMap deleteCanisterRequest = Arguments.createMap();

		deleteCanisterRequest.putString("canister_id", canisterId);

		this.call(promise,"delete_canister",Void.class,null, deleteCanisterRequest);
	}

	@ReactMethod
	public void startCanister(String canisterId,  Promise promise) {
		WritableMap startCanisterRequest = Arguments.createMap();

		startCanisterRequest.putString("canister_id", canisterId);

		this.call(promise,"start_canister",Void.class,null, startCanisterRequest);
	}

	@ReactMethod
	public void stopCanister(String canisterId, Promise promise) {
		WritableMap stopCanisterRequest = Arguments.createMap();

		stopCanisterRequest.putString("canister_id", canisterId);

		this.call(promise,"stop_canister",Void.class,null, stopCanisterRequest);
	}
	
	public void depositCycles(String canisterId, Promise promise) {
		WritableMap depositCyclesRequest = Arguments.createMap();

		depositCyclesRequest.putString("canister_id", canisterId);

		this.call(promise,"deposit_cycles",Void.class,null, depositCyclesRequest);
	}

	@ReactMethod
	public void canisterStatus(String canisterId, Promise promise) {
		WritableMap canisterStatusRequest = Arguments.createMap();

		canisterStatusRequest.putString("canister_id", canisterId);

		this.call(promise,"canister_status",ReadableMap.class,null,canisterStatusRequest);
	}

	@ReactMethod
	public void rawRand(Promise promise) {
		this.call(promise,"raw_rand",ReadableArray.class,null);
	}


	@ReactMethod
	public void provisionalCreateCanisterWithCycles(ReadableMap settings, Double amount, String specifiedId, Promise promise) {
		WritableMap provisionalCreateCanisterWithCyclesRequest = Arguments.createMap();

		if(amount == 0)
			provisionalCreateCanisterWithCyclesRequest.putNull("amount");
		else
			provisionalCreateCanisterWithCyclesRequest.putDouble("amount", amount);

		provisionalCreateCanisterWithCyclesRequest.putMap("settings", settings);
		provisionalCreateCanisterWithCyclesRequest.putString("specified_id", specifiedId);

		this.call(promise,"provisional_create_canister_with_cycles",String.class,null,provisionalCreateCanisterWithCyclesRequest);
		
	}

	@ReactMethod
	public void provisionalTopUpCanister(String canisterId, Double amount, Promise promise) {
		WritableMap provisionalTopUpCanisterRequest = Arguments.createMap();

		provisionalTopUpCanisterRequest.putString("canister_id", canisterId);
		provisionalTopUpCanisterRequest.putDouble("amount", amount);

		this.call(promise,"provisional_top_up_canister",Void.class,null,provisionalTopUpCanisterRequest);
	}


}
