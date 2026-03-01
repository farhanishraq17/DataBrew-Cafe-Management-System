package com.databrew.cafe.dao;

import com.databrew.cafe.model.Attendance;
import com.databrew.cafe.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDao {

    public List<Attendance> findByEmployee(long employeeId) throws SQLException {
        String sql = "SELECT id, employee_id, shift_id, work_date, check_in, check_out, status FROM attendance WHERE employee_id=? ORDER BY work_date DESC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Attendance> list = new ArrayList<>();
                while (rs.next())
                    list.add(map(rs));
                return list;
            }
        }
    }

    public List<Attendance> findAll() throws SQLException {
        String sql = "SELECT a.id, a.employee_id, a.shift_id, a.work_date, a.check_in, a.check_out, a.status " +
                "FROM attendance a ORDER BY a.work_date DESC, a.check_in DESC LIMIT 500";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<Attendance> list = new ArrayList<>();
            while (rs.next())
                list.add(map(rs));
            return list;
        }
    }

    public List<Attendance> findByDateRange(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT a.id, a.employee_id, a.shift_id, a.work_date, a.check_in, a.check_out, a.status " +
                "FROM attendance a WHERE a.work_date BETWEEN ? AND ? ORDER BY a.work_date DESC LIMIT 500";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                List<Attendance> list = new ArrayList<>();
                while (rs.next())
                    list.add(map(rs));
                return list;
            }
        }
    }

    public void upsertAssignment(long employeeId, long shiftId, LocalDate workDate) throws SQLException {
        String sql = "INSERT INTO attendance (employee_id, shift_id, work_date, status) VALUES (?,?,?, 'PRESENT') " +
                "ON DUPLICATE KEY UPDATE shift_id=VALUES(shift_id)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            ps.setLong(2, shiftId);
            ps.setDate(3, Date.valueOf(workDate));
            ps.executeUpdate();
        }
    }

    public void checkIn(long attendanceId) throws SQLException {
        String sql = "UPDATE attendance SET check_in = NOW() WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attendanceId);
            ps.executeUpdate();
        }
    }

    public void checkOut(long attendanceId) throws SQLException {
        String sql = "UPDATE attendance SET check_out = NOW() WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attendanceId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(long attendanceId, String status) throws SQLException {
        String sql = "UPDATE attendance SET status=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, attendanceId);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM attendance WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Attendance map(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setId(rs.getLong("id"));
        a.setEmployeeId(rs.getLong("employee_id"));
        a.setShiftId(rs.getLong("shift_id"));
        a.setWorkDate(rs.getDate("work_date").toLocalDate());
        a.setCheckIn(rs.getTimestamp("check_in") == null ? null : rs.getTimestamp("check_in").toLocalDateTime());
        a.setCheckOut(rs.getTimestamp("check_out") == null ? null : rs.getTimestamp("check_out").toLocalDateTime());
        a.setStatus(rs.getString("status"));
        return a;
    }
}
