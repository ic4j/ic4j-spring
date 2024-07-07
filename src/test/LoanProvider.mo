import Principal "mo:base/Principal";
import Buffer "mo:base/Buffer";
import Option "mo:base/Option";
import Time "mo:base/Time";

actor class LoanProvider() = this {
    // Loan Application
  public type LoanApplication = {
    id: Nat;
    firstname: Text;
    lastname: Text;
    zipcode: Text;
    ssn: Text;
    amount: Float;
    term: Nat16;
    created: Int;
  };

 // Credit Check Request
  public type CreditRequest = {
    userid: Principal;   
    firstname: Text;
    lastname: Text;
    zipcode: Text;
    ssn: Text;
    created: Int;   
  };

   // Credit Check
  public type Credit = {
    userid: Principal;   
    rating: Nat16;
    created: Int;
  };

   // Loan Offer Request
  public type LoanOfferRequest = {
    userid: Principal;   
    applicationid: Nat;
    amount: Float;
    term: Nat16;   
    rating: Nat16;
    zipcode: Text;
    created: Int;    
  };

   // Loan Offer
    public type LoanOffer = {
        providerid: Principal;
        providername: Text;
        userid: Principal;   
        applicationid: Nat;
        apr: Float;
        created: Int;
    }; 

    var name : ?Text = null;

    var requests : Buffer.Buffer<LoanOfferRequest> = Buffer.Buffer(0);
    var offers : Buffer.Buffer<LoanOffer> = Buffer.Buffer(0);

    public shared (msg) func init(input : Text) {
        name := Option.make(input);
        requests := Buffer.Buffer(0);
        offers := Buffer.Buffer(0);
    };

    public shared query func getName() : async ?Text{
        return name;
    };    

    public func addRequest(request : LoanOfferRequest){
        requests.add(request);
    };

    public shared (msg) func addOffer(applicationId : Nat, apr : Float){
    };  

    public shared query (msg) func getRequests() : async [LoanOfferRequest] {
        return requests.toArray();
    }; 

    public shared query (msg) func getOffers() : async [LoanOffer] {
        return offers.toArray();
    };    

};