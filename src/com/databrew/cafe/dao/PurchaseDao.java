package com.databrew.cafe.dao;

import com.databrew.cafe.model.Purchase;
import com.databrew.cafe.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDao {

    private static final String SELECT_ALL = "SELECT p.id, p.supplier_id, s.name AS supplier_name, "
            + "p.ingredient_id, ing.name AS ingredient_name, "
            + "p.quantity, p.cost, p.purchased_at "
            + "FROM purchases p "
            + "JOIN suppliers s ON s.id = p.supplier_id "
            + "JOIN ingredients ing ON ing.id = p.ingredient_id "
            + "ORDER BY p.purchased_at DESC";

    public List<Purchase> findAll() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
                ResultSet rs = ps.executeQuery()) {
            List<Purchase> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    }

    /** Insert purchase and auto-increase inventory stock. */
    public long insertAndAdjustStock(Purchase p) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insert purchase
                String sql = "INSERT INTO purchases (supplier_id, ingredient_id, quantity, cost) VALUES (?,?,?,?)";
                long purchaseId;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, p.getSupplierId());
                    ps.setLong(2, p.getIngredientId());
                    ps.setDouble(3, p.getQuantity());
                    ps.setDouble(4, p.getCost());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next())
                            purchaseId = rs.getLong(1);
                        else
                            throw new SQLException("Failed to get purchase ID");
                    }
                }
                // Adjust inventory
                String adjSql = "UPDATE inventory SET quantity = quantity + ? WHERE ingredient_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(adjSql)) {
                    ps.setDouble(1, p.getQuantity());
                    ps.setLong(2, p.getIngredientId());
                    ps.executeUpdate();
                }
                conn.commit();
                return purchaseId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM purchases WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Purchase map(ResultSet rs) throws SQLException {
        Purchase p = new Purchase();
        p.setId(rs.getLong("id"));
        p.setSupplierId(rs.getLong("supplier_id"));
        p.setSupplierName(rs.getString("supplier_name"));
        p.setIngredientId(rs.getLong("ingredient_id"));
        p.setIngredientName(rs.getString("ingredient_name"));
        p.setQuantity(rs.getDouble("quantity"));
        p.setCost(rs.getDouble("cost"));
        Timestamp ts = rs.getTimestamp("purchased_at");
        if (ts != null)
            p.setPurchasedAt(ts.toLocalDateTime());
        return p;
    }
}
