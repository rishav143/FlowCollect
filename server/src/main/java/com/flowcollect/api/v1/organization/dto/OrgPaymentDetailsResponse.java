package com.flowcollect.api.v1.organization.dto;

public class OrgPaymentDetailsResponse {

    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String iban;
    private String swiftCode;
    private String routingNumber;
    private String ifscCode;
    private String upiId;
    private String paypalEmail;
    private String wiseEmail;
    private String additionalNote;

    public String getBankName()          { return bankName; }
    public String getAccountHolderName() { return accountHolderName; }
    public String getAccountNumber()     { return accountNumber; }
    public String getIban()              { return iban; }
    public String getSwiftCode()         { return swiftCode; }
    public String getRoutingNumber()     { return routingNumber; }
    public String getIfscCode()          { return ifscCode; }
    public String getUpiId()             { return upiId; }
    public String getPaypalEmail()       { return paypalEmail; }
    public String getWiseEmail()         { return wiseEmail; }
    public String getAdditionalNote()    { return additionalNote; }

    public void setBankName(String v)          { this.bankName = v; }
    public void setAccountHolderName(String v) { this.accountHolderName = v; }
    public void setAccountNumber(String v)     { this.accountNumber = v; }
    public void setIban(String v)              { this.iban = v; }
    public void setSwiftCode(String v)         { this.swiftCode = v; }
    public void setRoutingNumber(String v)     { this.routingNumber = v; }
    public void setIfscCode(String v)          { this.ifscCode = v; }
    public void setUpiId(String v)             { this.upiId = v; }
    public void setPaypalEmail(String v)       { this.paypalEmail = v; }
    public void setWiseEmail(String v)         { this.wiseEmail = v; }
    public void setAdditionalNote(String v)    { this.additionalNote = v; }
}
