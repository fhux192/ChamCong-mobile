// FaceData.java

package com.example.timekeeping;

import java.util.List;

public class FaceData {
    private String userId;
    private String name;
    private String companyName;
    private List<List<Float>> embeddings;
    private double officeLatitude;
    private double officeLongitude;

    public FaceData() {
    }

    public FaceData(String userId, String name, String companyName, List<List<Float>> embeddings, double officeLatitude, double officeLongitude) {
        this.userId = userId;
        this.name = name;
        this.companyName = companyName;
        this.embeddings = embeddings;
        this.officeLatitude = officeLatitude;
        this.officeLongitude = officeLongitude;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public List<List<Float>> getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(List<List<Float>> embeddings) {
        this.embeddings = embeddings;
    }

    public double getOfficeLatitude() {
        return officeLatitude;
    }

    public void setOfficeLatitude(double officeLatitude) {
        this.officeLatitude = officeLatitude;
    }

    public double getOfficeLongitude() {
        return officeLongitude;
    }

    public void setOfficeLongitude(double officeLongitude) {
        this.officeLongitude = officeLongitude;
    }
}
