import Buffer "mo:base/Buffer";
import Time "mo:base/Time";

persistent actor class LoanProvider(nameInput : Text) = this {
  public type LoanOfferRequest = {
    applicationid: Nat;
    amount: Float;
    term: Nat16;   
    rating: Nat16;
    zipcode: Text;
    created: Int;    
  };

   // Loan Offer
    public type LoanOffer = {
        applicationid: Nat;
        apr: Float;
        created: Int;
    }; 

      transient var name : Text = nameInput;

      transient var requests : Buffer.Buffer<LoanOfferRequest> = Buffer.Buffer(0);
      transient var offers : Buffer.Buffer<LoanOffer> = Buffer.Buffer(0);

      public shared query func getName() : async Text {
        return name;
      };

      public shared func addRequest(request : LoanOfferRequest) : async () {
        requests.add(request);
    };

      public shared func addOffer(applicationId : Nat, apr : Float) : async () {
        offers.add({
          applicationid = applicationId;
          apr = apr;
          created = Time.now();
        });
    };  

	public shared query (_) func getRequests() : async [LoanOfferRequest] {
  		return Buffer.toArray(requests);
    }; 

	public shared query (_) func getOffers() : async [LoanOffer] {
  		return Buffer.toArray(offers);
    };    

};