package com.example.timekeeping;

import java.util.List;

public class MainModel {

    // =============== FIELDS ===============
    private String name;
    private List<Float> embedding;

    // =============== CONSTRUCTORS ===============
    public MainModel() {
    }

    public MainModel(String name, List<Float> embedding) {
        this.name = name;
        this.embedding = embedding;
    }

    // =============== GETTERS & SETTERS ===============
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }
}
