package com.databrew.cafe.dao;

import com.databrew.cafe.model.User;
import com.databrew.cafe.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT u.id, u.username, u.email, u.password_hash, u.full_name, u.is_active, " +
                "GROUP_CONCAT(r.name ORDER BY r.name SEPARATOR ', ') AS roles " +
                "FROM users u LEFT JOIN user_roles ur ON ur.user_id = u.id " +
                "LEFT JOIN roles r ON r.id = ur.role_id WHERE u.username = ? GROUP BY u.id";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT u.id, u.username, u.email, u.password_hash, u.full_name, u.is_active, " +
                "GROUP_CONCAT(r.name ORDER BY r.name SEPARATOR ', ') AS roles " +
                "FROM users u LEFT JOIN user_roles ur ON ur.user_id = u.id " +
                "LEFT JOIN roles r ON r.id = ur.role_id GROUP BY u.id ORDER BY u.username";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (rs.next())
                users.add(map(rs));
            return users;
        }
    }

    public User findById(long id) throws SQLException {
        String sql = "SELECT u.id, u.username, u.email, u.password_hash, u.full_name, u.is_active, " +
                "GROUP_CONCAT(r.name ORDER BY r.name SEPARATOR ', ') AS roles " +
                "FROM users u LEFT JOIN user_roles ur ON ur.user_id = u.id " +
                "LEFT JOIN roles r ON r.id = ur.role_id WHERE u.id = ? GROUP BY u.id";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public long insert(User u, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, full_name, is_active) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getEmail());
            ps.setString(3, passwordHash);
            ps.setString(4, u.getFullName());
            ps.setBoolean(5, u.isActive());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getLong(1);
            }
            throw new SQLException("Insert user failed");
        }
    }

    public void update(User u) throws SQLException {
        String sql = "UPDATE users SET username=?, email=?, full_name=?, is_active=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getFullName());
            ps.setBoolean(4, u.isActive());
            ps.setLong(5, u.getId());
            ps.executeUpdate();
        }
    }

    public void updatePassword(long userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // ---- Role management ----

    public void assignRole(long userId, long roleId) throws SQLException {
        String sql = "INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?,?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, roleId);
            ps.executeUpdate();
        }
    }

    public void removeAllRoles(long userId) throws SQLException {
        String sql = "DELETE FROM user_roles WHERE user_id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    public List<String> getUserRoles(long userId) throws SQLException {
        String sql = "SELECT r.name FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> roles = new ArrayList<>();
                while (rs.next())
                    roles.add(rs.getString("name"));
                return roles;
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setActive(rs.getBoolean("is_active"));
        return u;
    }
}
