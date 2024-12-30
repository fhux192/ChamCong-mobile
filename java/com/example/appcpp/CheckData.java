// CheckData.java

package com.example.appcpp;

public class CheckData {
    private String userId;
    private String name;
    private String location;
    private long timestamp;

    // Default constructor required for calls to DataSnapshot.getValue(CheckData.class)
    public CheckData() {
    }

    public CheckData(String userId, String name, String location, long timestamp) {
        this.userId = userId;
        this.name = name;
        this.location = location;
        this.timestamp = timestamp;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
