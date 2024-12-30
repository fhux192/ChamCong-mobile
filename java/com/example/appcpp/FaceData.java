// FaceData.java

package com.example.appcpp;

import java.util.ArrayList;
import java.util.List;

public class FaceData {
    private String userId;
    private String name;
    private List<List<Float>> embeddings; // List of embeddings

    public FaceData() {
        // Empty constructor for Firebase
    }

    public FaceData(String userId, String name, List<List<Float>> embeddings) {
        this.userId = userId;
        this.name = name;
        this.embeddings = embeddings;
    }

    // Getters and setters

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

    public List<List<Float>> getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(List<List<Float>> embeddings) {
        this.embeddings = embeddings;
    }
}