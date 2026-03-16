package org.ic4j.spring.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LoanProviderTest {
	private static final class InMemoryLoanProvider implements LoanProvider {
		private final List<LoanOfferRequest> requests = new ArrayList<LoanOfferRequest>();

		@Override
		public String getName() {
			return "United Loan";
		}

		@Override
		public LoanOfferRequest[] getRequests() {
			return requests.toArray(new LoanOfferRequest[0]);
		}

		@Override
		public LoanOffer[] getOffers() {
			return new LoanOffer[0];
		}

		@Override
		public void addRequest(LoanOfferRequest request) {
			requests.add(request);
		}

		@Override
		public void addOffer(BigInteger applicationId, Double apr) {
		}
	}

    @Test
    public void test()
    {
		LoanProvider loanProviderService = new InMemoryLoanProvider();

		Assertions.assertEquals("United Loan", loanProviderService.getName());
    	
    	LoanOfferRequest request = new LoanOfferRequest();
    	
    	request.amount = 1000.0;
    	request.applicationId = java.math.BigInteger.valueOf(1);
    	request.rating= 700;
    	request.zipcode = "12345";
    	request.term = 12;
    	request.created = System.currentTimeMillis();
    	
		loanProviderService.addRequest(request);
		
		LoanOfferRequest[] requests = loanProviderService.getRequests();
		
		Assertions.assertEquals(1, requests.length);
		Assertions.assertEquals(java.math.BigInteger.valueOf(1), requests[0].applicationId);
		Assertions.assertEquals(Short.valueOf((short) 700), request.rating);
    }
}
