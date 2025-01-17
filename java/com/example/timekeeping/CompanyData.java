package com.example.timekeeping;

/**
 * Lớp model đại diện cho 1 công ty trong Firebase
 */
public class CompanyData {
    private String id;          // Key của công ty trên Firebase
    private String companyName;

    // Bắt buộc để Firebase parse
    public CompanyData() {
    }

    public CompanyData(String id, String companyName) {
        this.id = id;
        this.companyName = companyName;
    }

    // Getter & Setter
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
