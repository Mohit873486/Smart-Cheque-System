package com.chequeprint.model;

public class User {
    private int id;
    private String name;
    private String email;
    private String phone;
    private String company;
    private String address;

    public User() {}

    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }
    public String getName()             { return name; }
    public void setName(String n)       { this.name = n; }
    public String getEmail()            { return email; }
    public void setEmail(String e)      { this.email = e; }
    public String getPhone()            { return phone; }
    public void setPhone(String p)      { this.phone = p; }
    public String getCompany()          { return company; }
    public void setCompany(String c)    { this.company = c; }
    public String getAddress()          { return address; }
    public void setAddress(String a)    { this.address = a; }
}

