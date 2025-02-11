package com.example.timekeeping;

public class CompanyData {

    // =============== FIELDS ===============
    private String id;
    private String companyName;

    // =============== CONSTRUCTORS ===============
    public CompanyData() {
    }

    public CompanyData(String id, String companyName) {
        this.id = id;
        this.companyName = companyName;
    }

    // =============== GETTERS & SETTERS ===============
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
