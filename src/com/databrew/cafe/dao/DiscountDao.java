package com.databrew.cafe.dao;

import com.databrew.cafe.model.Discount;
import com.databrew.cafe.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DiscountDao {

    public List<Discount> findAll() throws SQLException {
        String sql = "SELECT id, name, type, value, applies_to FROM discounts ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<Discount> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    }

    public List<Discount> findByCustomerType(String customerType) throws SQLException {
        String sql = "SELECT id, name, type, value, applies_to FROM discounts WHERE applies_to=? OR applies_to='GENERAL' ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerType);
            try (ResultSet rs = ps.executeQuery()) {
                List<Discount> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public long insert(Discount d) throws SQLException {
        String sql = "INSERT INTO discounts (name, type, value, applies_to) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getType());
            ps.setDouble(3, d.getValue());
            ps.setString(4, d.getAppliesTo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getLong(1);
            }
            throw new SQLException("Insert discount failed");
        }
    }

    public void update(Discount d) throws SQLException {
        String sql = "UPDATE discounts SET name=?, type=?, value=?, applies_to=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getType());
            ps.setDouble(3, d.getValue());
            ps.setString(4, d.getAppliesTo());
            ps.setLong(5, d.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM discounts WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Discount map(ResultSet rs) throws SQLException {
        Discount d = new Discount();
        d.setId(rs.getLong("id"));
        d.setName(rs.getString("name"));
        d.setType(rs.getString("type"));
        d.setValue(rs.getDouble("value"));
        d.setAppliesTo(rs.getString("applies_to"));
        return d;
    }
}
