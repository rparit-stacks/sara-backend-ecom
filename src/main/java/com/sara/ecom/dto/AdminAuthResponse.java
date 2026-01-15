package com.sara.ecom.dto;

public class AdminAuthResponse {
    private String token;
    private AdminDto admin;
    
    public AdminAuthResponse(String token, AdminDto admin) {
        this.token = token;
        this.admin = admin;
    }
    
    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public AdminDto getAdmin() { return admin; }
    public void setAdmin(AdminDto admin) { this.admin = admin; }
}
