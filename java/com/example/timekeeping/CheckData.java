package com.example.timekeeping;

import java.util.HashMap;
import java.util.Map;

public class CheckData {

    // =============== FIELDS ===============
    private String userId;
    private String name;
    private String location;
    private long timestamp;
    private String companyName;

    // =============== CONSTRUCTORS ===============
    public CheckData() {}

    public CheckData(String userId, String name, String location, long timestamp, String companyName) {
        this.userId = userId;
        this.name = name;
        this.location = location;
        this.timestamp = timestamp;
        this.companyName = companyName;
    }

    // =============== GETTERS & SETTERS ===============
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    // =============== OVERRIDE EQUALS & HASHCODE ===============
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof CheckData))
            return false;
        CheckData other = (CheckData) obj;
        return userId != null && userId.equals(other.userId) &&
                name != null && name.equals(other.name) &&
                location != null && location.equals(other.location) &&
                timestamp == other.timestamp &&
                companyName != null && companyName.equals(other.companyName);
    }

    @Override
    public int hashCode() {
        int result = (userId != null) ? userId.hashCode() : 0;
        result = 31 * result + ((name != null) ? name.hashCode() : 0);
        result = 31 * result + ((location != null) ? location.hashCode() : 0);
        result = 31 * result + Long.hashCode(timestamp);
        result = 31 * result + ((companyName != null) ? companyName.hashCode() : 0);
        return result;
    }
}
