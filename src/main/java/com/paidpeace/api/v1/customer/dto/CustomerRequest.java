package com.paidpeace.api.v1.customer.dto;


public class CustomerRequest {
    private String name;
    private String email;
    private String phone;
    private String address;
    private String companyName;

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
