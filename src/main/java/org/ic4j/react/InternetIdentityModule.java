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

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.ic4j.agent.Waiter;
import org.ic4j.agent.annotations.Agent;
import org.ic4j.agent.annotations.Canister;
import org.ic4j.agent.annotations.IDLFile;
import org.ic4j.agent.annotations.Identity;
import org.ic4j.agent.annotations.IdentityType;
import org.ic4j.agent.annotations.Properties;
import org.ic4j.agent.annotations.Transport;
import org.ic4j.agent.identity.BasicIdentity;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.types.Mode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Agent(transport = @Transport(url = "https://icp-api.io"), identity = @Identity(type = IdentityType.BASIC))
@Canister("rdmx6-jaaaa-aaaaa-aaadq-cai")
@IDLFile("internet_identity.did")
@Properties(loadIDL = true, disableRangeCheck = false)

public final class InternetIdentityModule extends ICModule {
	static int WAITER_TIMEOUT = 60;
	static int WAITER_SLEEP = 5;

	static Waiter waiter = Waiter.create(WAITER_TIMEOUT, WAITER_SLEEP);

	Path identityFile;

	public InternetIdentityModule(ReactApplicationContext context) throws URISyntaxException, NoSuchAlgorithmException {
		super(context);
	}

	public InternetIdentityModule(ReactApplicationContext context, boolean isLocal) throws URISyntaxException, NoSuchAlgorithmException, IOException {
		super(context);

		this.isLocal = isLocal;
	}


	@Override
	public String getName() {
		return "InternetIdentityModule";
	}

	@ReactMethod
	public void generatePublicKey(Promise promise) {
		try {
			KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();

			String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
			promise.resolve(publicKey);
		} catch (NoSuchAlgorithmException e) {
			promise.reject(e);
		}
	}

	@ReactMethod
	public void createIdentityFile(String identityFileName, Promise promise)  {

		try {
			KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
			PemObject pemObject = new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded());

			String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

			File fileDirectory = this.getReactApplicationContext().getDataDir();

			this.identityFile = Paths.get(fileDirectory + File.separator + identityFileName);

			PemWriter pemWriter = new PemWriter(new FileWriter(identityFile.toFile()));
			pemWriter.writeObject(pemObject);
			pemWriter.close();

			promise.resolve(publicKey);
		} catch (NoSuchAlgorithmException | IOException e) {
			promise.reject(e);
		}
	}

	@ReactMethod
	public void getPublicKey(Promise promise) {
		if(this.identityFile != null)
		{
			BasicIdentity identity = BasicIdentity.fromPEMFile(this.identityFile);

			String publicKey = Base64.getEncoder().encodeToString(identity.derEncodedPublickey);

			promise.resolve(publicKey);
		}
		else
			promise.reject(new Exception("Cannot find identity file"));

	}

	@ReactMethod
	public void connect(String location, String canisterId, String identityFileName,  Promise promise) {
		try {
			this.init( location, canisterId,null, identityFileName, waiter);
			promise.resolve(null);
		} catch (URISyntaxException e) {
			promise.reject(e);
		}
	}

	@ReactMethod
	public void lookup(Double userNumber, Promise promise) {
		this.call(promise,"lookup", ReadableArray.class, new Mode[]{Mode.QUERY}, userNumber);
	}

	@ReactMethod
	public void stats(Promise promise) {
		this.call(promise,"stats", ReadableMap.class, new Mode[]{Mode.QUERY});
	}

	@ReactMethod
	public void createChallenge(Promise promise) {
		this.call(promise,"create_challenge", ReadableMap.class,null);
	}

	@ReactMethod
	public void register(ReadableMap device, ReadableMap challengeResult, Promise promise) {
		this.call(promise, "register", ReadableMap.class, null,device, challengeResult);
	}

	@ReactMethod
	public void getAnchorInfo(Double userNumber, Promise promise) {
		this.call(promise, "get_anchor_info", ReadableMap.class, null,userNumber);
	}

	@ReactMethod
	public void getPrincipal(Double userNumber,String frontendHostname,  Promise promise) {
		this.call(promise,"get_principal", String.class, new Mode[]{Mode.QUERY},userNumber, frontendHostname);
	}


	@ReactMethod
	public void enterDeviceRegistrationMode(Double userNumber,  Promise promise)
	{
		this.call(promise, "enter_device_registration_mode", Double.class, null,userNumber);
	}

	@ReactMethod
	public void exitDeviceRegistrationMode(Double userNumber,  Promise promise)
	{
		this.call(promise, "exit_device_registration_mode", Void.class, null,userNumber);
	}

	@ReactMethod
	public void addTentativeDevice(Double userNumber,ReadableMap deviceData,  Promise promise)
	{
		this.call(promise, "add_tentative_device", ReadableMap.class, null,userNumber, deviceData);
	}

	@ReactMethod
	public void verifyTentativeDevice(Double userNumber, String verificationCode,  Promise promise)
	{
		this.call(promise, "verify_tentative_device", ReadableMap.class, null,userNumber, verificationCode);
	}

	@ReactMethod
	public void prepareDelegation(Double userNumber, String frontendHostname, String sessionKey, Double maxTimeToLive,  Promise promise)
	{
		if(maxTimeToLive == 0)
			maxTimeToLive = null;

		this.call(promise, "prepare_delegation", IDLArgs.class, null,userNumber, frontendHostname, sessionKey, maxTimeToLive);
	}

	@ReactMethod
	public void getDelegation(Double userNumber,String frontendHostname, String sessionKey, Double timestamp,  Promise promise) {
		this.call(promise,"get_delegation", ReadableMap.class, new Mode[]{Mode.QUERY},userNumber, frontendHostname, sessionKey, timestamp);
	}

	@ReactMethod
	public void add(Double userNumber, ReadableMap deviceData, Promise promise) {
		this.call(promise, "add", Void.class, null,userNumber, deviceData);
	}

	@ReactMethod
	public void update(Double userNumber, String deviceKey, ReadableMap deviceData, Promise promise) {
		this.call(promise, "update", Void.class, null,userNumber, deviceKey, deviceData);
	}

	@ReactMethod
	public void remove(Double userNumber, String deviceKey,  Promise promise) {
		this.call(promise, "remove", Void.class, null,userNumber, deviceKey);
	}
}
