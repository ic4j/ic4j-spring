package org.ic4j.spring.test;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Configuration
@PropertySource("classpath:application.properties")
public class LoanProviderService extends org.ic4j.spring.Service implements LoanProvider {

	public LoanProviderService(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}


	@PostConstruct
	public void init() throws URISyntaxException, IOException {
		String location = env.getProperty("ic.location");
		String canisterId = env.getProperty("ic.canister");
		if (canisterId == null) {
			String canisterName = env.getProperty("ic.canisterName");
			if (canisterName != null) {
				canisterId = IntegrationTestSupport.resolveLocalCanisterId(canisterName).orElse(null);
			}
		}
		String effectiveCanisterId = env.getProperty("ic.effectiveCanister");
		if (effectiveCanisterId == null) {
			effectiveCanisterId = canisterId;
		}

		super.init(LoanProvider.class, location, canisterId, effectiveCanisterId, null);
	}

	@Override
	public String getName()
    {
        return super.call("getName");
    }

	@Override
	public LoanOfferRequest[] getRequests() {
    	return super.call("getRequests");
    }

	@Override
	public LoanOffer[] getOffers() {
		 return super.call("getOffers");
    }    

	
	@Override
	@Async
	public void addRequest(LoanOfferRequest request) {
		super.call("addRequest",request);				
	}
	
	@Override
	@Async
	public void addOffer(BigInteger applicationId, Double apr) {
		super.call("getOffers", applicationId, apr );	
	}
	

}
