package com.example.timekeeping;

public class ManagerUserData {

    // =============== FIELDS ===============
    private String id;
    private String name;
    private String email;
    private String role;
    private String company;
    private String status; // THÊM TRƯỜNG STATUS

    // =============== CONSTRUCTORS ===============
    public ManagerUserData() {}

    public ManagerUserData(String id, String name, String email, String role, String company, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.company = company;
        this.status = status;
    }

    // =============== GETTERS & SETTERS ===============
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
