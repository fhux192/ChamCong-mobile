package com.example.appcpp;

/**
 * Lớp model đại diện cho 1 công ty trong Firebase
 */
public class CompanyData {
    private String id;          // Key của công ty trên Firebase
    private String companyName;
    private double latitude;
    private double longitude;

    // Bắt buộc để Firebase parse
    public CompanyData() {
    }

    public CompanyData(String id, String companyName, double latitude, double longitude) {
        this.id = id;
        this.companyName = companyName;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
