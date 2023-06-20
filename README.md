## IC4J React Native Library 

To utilize the IC4J React Native Library, please refer to the instructions provided in the React documentation.

<a https://reactnative.dev/docs/native-modules-android">
https://reactnative.dev/docs/native-modules-android
</a>

To add dependencies, modify the build.gradle file.

```
implementation 'commons-codec:commons-codec:1.15'
implementation 'commons-io:commons-io:2.11.0'
implementation 'org.bouncycastle:bcprov-jdk15on:1.70'
implementation 'org.bouncycastle:bcpkix-jdk15on:1.70'
implementation("org.ic4j:ic4j-agent:0.6.19.3") {
     exclude group: 'org.apache.httpcomponents.client5', module: 'httpclient5'
}
implementation 'org.ic4j:ic4j-candid:0.6.19.3'
implementation 'org.slf4j:slf4j-api:2.0.6'

implementation 'org.ic4j:ic4j-reactnative:0.6.19-RC2'
```

To create a React Module that calls an Internet Computer canister, you can extend the ICModule superclass. To define the canister URL and ID, you have two options: either use a Java annotation or specify them in the module constructor.

The React method then invokes the 'call' method of the ICModule, passing the React promise, the canister method name, the return type, the method type (a null value defines UPDATE, to use QUERY use new Mode[]{Mode.QUERY}), and the method arguments.

```
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;

import org.ic4j.agent.annotations.Agent;
import org.ic4j.agent.annotations.Canister;
import org.ic4j.agent.annotations.Identity;
import org.ic4j.agent.annotations.IdentityType;
import org.ic4j.agent.annotations.Transport;
import org.ic4j.react.ICModule;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

@Agent(transport = @Transport(url = "https://m7sm4-2iaaa-aaaab-qabra-cai.ic0.app/"), identity = @Identity(type = IdentityType.ANONYMOUS))
@Canister("zwbmv-jyaaa-aaaab-qacaa-cai")
public class HelloModule extends ICModule {

   public HelloModule(ReactApplicationContext context) throws URISyntaxException, NoSuchAlgorithmException {
      super(context, null,null,null, null);
   }

   public HelloModule(ReactApplicationContext context, String location, String canisterId, String identity) throws URISyntaxException, NoSuchAlgorithmException {
      super(context, location,canisterId,null, identity);
   }

   @Override
   public String getName() {
      return "HelloModule";
   }
   
   @ReactMethod
   public void greet(String name, Promise promise) {
      this.call(promise, "greet", String.class, null, name);
   } 
}      

```

In your React code, you can then call the 'greet' method of the HelloModule.

```
const response = await HelloModule.greet(text );
console.log(`Response is ${response}`);
```


# Downloads / Accessing Binaries

To add Java IC4J React Native Library to your Java project use Maven or Gradle import from Maven Central.

<a href="https://search.maven.org/artifact/ic4j/ic4j-reactnative/0.6.19-RC2/jar">
https://search.maven.org/artifact/ic4j/ic4j-reactnative/0.6.19-RC2/jar
</a>

```
<dependency>
  <groupId>org.ic4j</groupId>
  <artifactId>ic4j-reactnative</artifactId>
  <version>0.6.19-RC2</version>
</dependency>
```

```
implementation 'org.ic4j:ic4j-reactnative:0.6.19-RC2'
```


# Build

You need JDK 8+ to build IC4J React Native Library .
