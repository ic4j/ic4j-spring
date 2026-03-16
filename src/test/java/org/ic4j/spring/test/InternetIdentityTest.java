package org.ic4j.spring.test;

import java.nio.file.Paths;

import org.ic4j.agent.identity.BasicIdentity;
import org.ic4j.internetidentity.DeviceData;
import org.ic4j.spring.InternetIdentityService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
public class InternetIdentityTest {
	@Autowired
	protected Environment env;

	private InternetIdentityService internetIdentityService;
	
    @Test
    public void test()
    {
    	BasicIdentity identity = BasicIdentity.fromPEMFile(Paths.get("Ed25519_identity.pem"));
    	
    	internetIdentityService = new InternetIdentityService(identity, env);
    	DeviceData[] deviceData = internetIdentityService.lookup(10001l);
    	Assertions.assertEquals(3, deviceData.length);	
    	
		Assertions.assertDoesNotThrow(() -> internetIdentityService.stats());

    }
}
