package org.ic4j.spring.test;

import java.math.BigInteger;
import java.util.List;

import org.ic4j.agent.annotations.QUERY;
import org.ic4j.agent.annotations.ResponseClass;
import org.ic4j.agent.annotations.Transport;
import org.ic4j.agent.annotations.UPDATE;
import org.ic4j.agent.annotations.Agent;
import org.ic4j.agent.annotations.Argument;
import org.ic4j.agent.annotations.Canister;
import org.ic4j.agent.annotations.EffectiveCanister;
import org.ic4j.agent.annotations.Identity;
import org.ic4j.agent.annotations.IdentityType;
import org.ic4j.candid.types.Type;

@Agent(identity = @Identity(type = IdentityType.BASIC, pem_file = "Ed25519_identity.pem"),fetchRootKey=true, transport = @Transport(url = "http://127.0.0.1:4943/"))
@Canister("by6od-j4aaa-aaaaa-qaadq-cai")
@EffectiveCanister("by6od-j4aaa-aaaaa-qaadq-cai")
public interface LoanProvider
{
    @QUERY
    public String getName();

    @QUERY
    public LoanOfferRequest[] getRequests();
    
    @QUERY
    public LoanOffer[] getOffers();    
    
    @UPDATE
    public void addRequest(@Argument(Type.VARIANT) LoanOfferRequest request);   

    @UPDATE
    public void addOffer(@Argument(Type.NAT) BigInteger applicationId, Double apr);
}