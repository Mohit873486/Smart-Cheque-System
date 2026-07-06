package com.chequeprint.backend.dto;

public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private long expiresIn;
    private UserDto user;

    public LoginResponse() {}

    public LoginResponse(String token, long expiresIn, UserDto user) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }

    public static class UserDto {
        private int id;
        private String username;
        private String name;
        private String email;
        private String role;

        public UserDto() {}

        public UserDto(int id, String username, String name, String email, String role) {
            this.id = id;
            this.username = username;
            this.name = name;
            this.email = email;
            this.role = role;
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
    }
}
