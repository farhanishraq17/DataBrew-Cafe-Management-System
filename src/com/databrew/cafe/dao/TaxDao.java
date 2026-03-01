package com.databrew.cafe.dao;

import com.databrew.cafe.model.Tax;
import com.databrew.cafe.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaxDao {

    public List<Tax> findAll() throws SQLException {
        String sql = "SELECT id, name, rate FROM taxes ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<Tax> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    }

    public Tax findById(long id) throws SQLException {
        String sql = "SELECT id, name, rate FROM taxes WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public long insert(Tax t) throws SQLException {
        String sql = "INSERT INTO taxes (name, rate) VALUES (?,?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getName());
            ps.setDouble(2, t.getRate());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getLong(1);
            }
            throw new SQLException("Insert tax failed");
        }
    }

    public void update(Tax t) throws SQLException {
        String sql = "UPDATE taxes SET name=?, rate=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getName());
            ps.setDouble(2, t.getRate());
            ps.setLong(3, t.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM taxes WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Tax map(ResultSet rs) throws SQLException {
        Tax t = new Tax();
        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setRate(rs.getDouble("rate"));
        return t;
    }
}
