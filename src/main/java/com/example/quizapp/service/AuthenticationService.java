package com.example.quizapp.service;

import com.example.quizapp.model.*;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Service
public class AuthenticationService {

    private final DataSource dataSource;

    public AuthenticationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // handles user registration for both admin and student accounts
    public boolean register(String username, String password, String role) {

        // check if the username already exists
        String checkSql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
             
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    return false; // username exists
                }
            }
            
            String hash = sha256(password); // store password securely
            
            String insertSql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, hash);
                insertStmt.setString(3, role.toUpperCase());
                insertStmt.executeUpdate();
            }
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // authenticates a user based on username, password and the expected role
    public User authenticate(String username, String password, String role) {
        String hash = sha256(password);
        
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ? AND role = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, username);
            stmt.setString(2, hash);
            stmt.setString(3, role.toUpperCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // allows login without specifying role, mostly for admin-only flows
    public User authenticateWithoutRole(String username, String password) {
        String hash = sha256(password);
        
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ? AND role = 'ADMIN'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, username);
            stmt.setString(2, hash);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private User mapRowToUser(ResultSet rs) throws Exception {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String passHash = rs.getString("password_hash");
        String role = rs.getString("role");
        
        if ("ADMIN".equalsIgnoreCase(role)) {
            return new Admin(id, username, passHash, role);
        } else {
            return new Student(id, username, passHash, role);
        }
    }
    
    public static String sha256(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return s;
        }
    }

}
