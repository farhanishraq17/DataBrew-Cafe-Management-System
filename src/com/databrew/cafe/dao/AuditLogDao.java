package com.databrew.cafe.dao;

import com.databrew.cafe.model.AuditLog;
import com.databrew.cafe.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDao {

    public List<AuditLog> findAll() throws SQLException {
        String sql = "SELECT al.id, al.user_id, u.username, al.action, al.entity, al.entity_id, al.details, al.created_at "
                + "FROM audit_logs al LEFT JOIN users u ON u.id = al.user_id "
                + "ORDER BY al.created_at DESC LIMIT 500";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return mapList(rs);
        }
    }

    public List<AuditLog> findByDateRange(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT al.id, al.user_id, u.username, al.action, al.entity, al.entity_id, al.details, al.created_at "
                + "FROM audit_logs al LEFT JOIN users u ON u.id = al.user_id "
                + "WHERE DATE(al.created_at) BETWEEN ? AND ? "
                + "ORDER BY al.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    public List<AuditLog> search(String keyword) throws SQLException {
        String sql = "SELECT al.id, al.user_id, u.username, al.action, al.entity, al.entity_id, al.details, al.created_at "
                + "FROM audit_logs al LEFT JOIN users u ON u.id = al.user_id "
                + "WHERE al.action LIKE ? OR al.entity LIKE ? OR al.details LIKE ? OR u.username LIKE ? "
                + "ORDER BY al.created_at DESC LIMIT 500";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            String term = "%" + keyword + "%";
            ps.setString(1, term);
            ps.setString(2, term);
            ps.setString(3, term);
            ps.setString(4, term);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    private List<AuditLog> mapList(ResultSet rs) throws SQLException {
        List<AuditLog> list = new ArrayList<>();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    private AuditLog map(ResultSet rs) throws SQLException {
        AuditLog a = new AuditLog();
        a.setId(rs.getLong("id"));
        long uid = rs.getLong("user_id");
        a.setUserId(rs.wasNull() ? null : uid);
        a.setUsername(rs.getString("username"));
        a.setAction(rs.getString("action"));
        a.setEntity(rs.getString("entity"));
        long eid = rs.getLong("entity_id");
        a.setEntityId(rs.wasNull() ? null : eid);
        a.setDetails(rs.getString("details"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null)
            a.setCreatedAt(ts.toLocalDateTime());
        return a;
    }
}
