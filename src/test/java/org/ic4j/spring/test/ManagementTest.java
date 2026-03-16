package org.ic4j.spring.test;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.ic4j.agent.identity.Identity;
import org.ic4j.agent.identity.Secp256k1Identity;
import org.ic4j.management.CanisterStatusResponse;
import org.ic4j.management.ManagementError;
import org.ic4j.spring.ManagementService;
import org.ic4j.types.Principal;
import org.ic4j.types.PrincipalError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
public class ManagementTest {
	@Autowired
	protected Environment env;

	private ManagementService managementService;
	
    @Test
    public void test()
    {				
		IntegrationTestSupport.assumeLocalReplicaAvailable();

		java.nio.file.Path identityPath = Paths.get("/Users/roman/.config/dfx/identity/default/identity.pem");
		Assertions.assertTrue(Files.exists(identityPath), "DFX default identity PEM is required for ManagementTest");

		Identity identity = Secp256k1Identity.fromPEMFile(identityPath);
		String effectiveCanisterId = env.getProperty("ic.managementEffectiveCanister", "x5pps-pqaaa-aaaab-qadbq-cai");
     	
    	try {
			managementService = new ManagementService(identity, Principal.managementCanister(), Principal.fromString(effectiveCanisterId), env);
			Principal canisterId = managementService.provisionalCreateCanisterWithCycles(Optional.empty(), Optional.empty()).get();

			Assertions.assertNotNull(canisterId);
			
			CanisterStatusResponse status = managementService.canisterStatus(canisterId).get();
			
			Assertions.assertNotNull(status);
			
			Assertions.assertEquals("running", status.status.name());
			
			managementService.deleteCanister(canisterId);
		} catch (ManagementError | URISyntaxException | PrincipalError | InterruptedException | ExecutionException e) {
			Assertions.fail(e.getMessage());
		}


    }
}
