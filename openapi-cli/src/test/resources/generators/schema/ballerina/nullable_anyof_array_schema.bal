import ballerina/constraint;
import ballerina/http;

public type Customers_customer_body record {
    # The customer's address.
    Customer_address|string? address?;
    # An integer amount in %s that represents the customer's current balance, which affect the customer's future invoices. A negative amount represents a credit that decreases the amount due on an invoice; a positive amount increases the amount due on an invoice.
    int balance?;
};

public type Customer_address record {
    @constraint:String {maxLength: 5000}
    string city?;
    @constraint:String {maxLength: 5000}
    string country?;
    @constraint:String {maxLength: 5000}
    string line1?;
    @constraint:String {maxLength: 5000}
    string line2?;
    @constraint:String {maxLength: 5000}
    string postal_code?;
    @constraint:String {maxLength: 5000}
    string state?;
};

public type Customer record {
    # The customer's address.
    Customer_address[]|string? address?;
    string name?;
};

public type OkCustomer record{|
    *http:Ok;
    Customer body;
    map<string|string[]> headers;
|};
