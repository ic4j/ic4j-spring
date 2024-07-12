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
		super.init( LoanProvider.class,null, null, null, null);
	}

    
	public String getName()
    {
        return super.call("getName");
    }
    
	 public LoanOfferRequest[] getRequests() {
    	return super.call("getRequests");
    }
    
	 public LoanOffer[] getOffers() {
		 return super.call("getOffers");
    }    

	
	@Async
	public void addRequest(LoanOfferRequest request) {
		super.call("addRequest",request);				
	}
	
	@Async
	public void addOffer(BigInteger applicationId, Double apr) {
		super.call("getOffers", applicationId, apr );	
	}
	

}
