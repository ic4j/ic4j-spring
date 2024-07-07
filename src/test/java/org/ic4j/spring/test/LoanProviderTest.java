package org.ic4j.spring.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = TestConfig.class)
public class LoanProviderTest {
	@Autowired
    private LoanProviderService loanProviderService;
	
    @Test
    public void test()
    {
    	String name = loanProviderService.getName();
    	Assertions.assertEquals("United Loan", name);	
    	
    	LoanOfferRequest request = new LoanOfferRequest();
    	
    	request.amount = 1000.0;
    	request.applicationId = java.math.BigInteger.valueOf(1);
    	request.rating= 700;
    	request.zipcode = "12345";
    	request.term = 12;
    	request.created = System.currentTimeMillis();
    	
		loanProviderService.addRequest(request);
		
		LoanOfferRequest[] requests = loanProviderService.getRequests();
		
		Assertions.assertEquals(2, requests.length);
    }
}
