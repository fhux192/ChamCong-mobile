package com.example.appcpp;

public class CompanyData {
    private String companyName;
    private double latitude;
    private double longitude;

    // Bắt buộc cần constructor rỗng để Realtime Database parse
    public CompanyData() {
    }

    public CompanyData(String companyName, double latitude, double longitude) {
        this.companyName = companyName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getter/Setter
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
