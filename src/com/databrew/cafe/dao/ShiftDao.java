package com.databrew.cafe.dao;

import com.databrew.cafe.model.Shift;
import com.databrew.cafe.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShiftDao {

    public List<Shift> findAll() throws SQLException {
        String sql = "SELECT id, name, start_time, end_time FROM shifts ORDER BY start_time";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<Shift> list = new ArrayList<>();
            while (rs.next())
                list.add(map(rs));
            return list;
        }
    }

    public Shift findById(long id) throws SQLException {
        String sql = "SELECT id, name, start_time, end_time FROM shifts WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public long insert(Shift s) throws SQLException {
        String sql = "INSERT INTO shifts (name, start_time, end_time) VALUES (?,?,?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getStartTime());
            ps.setString(3, s.getEndTime());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getLong(1);
            }
            throw new SQLException("Insert shift failed");
        }
    }

    public void update(Shift s) throws SQLException {
        String sql = "UPDATE shifts SET name=?, start_time=?, end_time=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getStartTime());
            ps.setString(3, s.getEndTime());
            ps.setLong(4, s.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM shifts WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Shift map(ResultSet rs) throws SQLException {
        Shift s = new Shift();
        s.setId(rs.getLong("id"));
        s.setName(rs.getString("name"));
        s.setStartTime(rs.getString("start_time"));
        s.setEndTime(rs.getString("end_time"));
        return s;
    }
}
