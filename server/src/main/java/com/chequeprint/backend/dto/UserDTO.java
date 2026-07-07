package com.chequeprint.backend.dto;

public class UserDTO {
    private int id;
    private String username;
    private String name;
    private String email;
    private String role;
    private String status;
    private String phone;
    private String company;
    private String address;
    private String gstNumber;

    public UserDTO() {}

    public UserDTO(int id, String username, String name, String email, String role, String status, String phone, String company, String address, String gstNumber) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.phone = phone;
        this.company = company;
        this.address = address;
        this.gstNumber = gstNumber;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }
}
