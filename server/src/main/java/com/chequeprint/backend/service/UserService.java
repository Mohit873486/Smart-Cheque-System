package com.chequeprint.backend.service;

import com.chequeprint.backend.entity.User;
import com.chequeprint.backend.repository.UserRepository;
import com.chequeprint.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository repository;
    private final AuditLogService auditLogService;

    @Autowired
    public UserService(UserRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    public List<User> getAllUsers() {
        return repository.findAll();
    }

    public Optional<User> getUserById(int id) {
        return repository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return repository.findByUsername(username);
    }

    public User createUser(User user) {
        validateNewUser(user);
        
        // Hash password before saving
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
        user.setPassword(hashedPassword);
        
        User saved = repository.save(user);
        auditLogService.record(null, "users", saved.getId(), "INSERT", "Created user: " + saved.getUsername());
        return saved;
    }

    public User updateUser(int id, User updatedUser) {
        return repository.findById(id)
                .map(existing -> {
                    // Check username uniqueness if modified
                    if (!existing.getUsername().equals(updatedUser.getUsername())) {
                        Optional<User> duplicate = repository.findByUsername(updatedUser.getUsername());
                        if (duplicate.isPresent()) {
                            throw new IllegalArgumentException("Username '" + updatedUser.getUsername() + "' already exists.");
                        }
                    }

                    existing.setUsername(updatedUser.getUsername());
                    existing.setName(updatedUser.getName());
                    existing.setEmail(updatedUser.getEmail());
                    existing.setRole(updatedUser.getRole());
                    existing.setStatus(updatedUser.getStatus());
                    existing.setPhone(updatedUser.getPhone());
                    existing.setCompany(updatedUser.getCompany());
                    existing.setAddress(updatedUser.getAddress());
                    existing.setGstNumber(updatedUser.getGstNumber());

                    // If a new password is provided, hash and update it
                    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
                        if (updatedUser.getPassword().length() < 6) {
                            throw new IllegalArgumentException("Password must be at least 6 characters long.");
                        }
                        String hashedPassword = BCrypt.hashpw(updatedUser.getPassword(), BCrypt.gensalt(12));
                        existing.setPassword(hashedPassword);
                    }

                    User saved = repository.save(existing);
                    auditLogService.record(null, "users", saved.getId(), "UPDATE", "Updated user profile: " + saved.getUsername());
                    return saved;
                })
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    public void deleteUser(int id) {
        Optional<User> existing = repository.findById(id);
        if (existing.isEmpty()) {
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }
        repository.deleteById(id);
        auditLogService.record(null, "users", id, "DELETE", "Deleted user: " + existing.get().getUsername());
    }

    public void changePassword(int id, String currentPassword, String newPassword) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (!BCrypt.checkpw(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        user.setPassword(hashedPassword);
        repository.save(user);

        auditLogService.record(null, "users", user.getId(), "UPDATE", "Changed password for user: " + user.getUsername());
    }

    public List<com.chequeprint.backend.entity.AuditLog> getUserActivity(int id) {
        return auditLogService.getHistoryByUserId(id);
    }

    private void validateNewUser(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        Optional<User> duplicate = repository.findByUsername(user.getUsername());
        if (duplicate.isPresent()) {
            throw new IllegalArgumentException("Username '" + user.getUsername() + "' already exists.");
        }
    }
}
