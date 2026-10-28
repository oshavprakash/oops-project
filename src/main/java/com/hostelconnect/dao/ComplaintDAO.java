package com.hostelconnect.dao;

import com.hostelconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplaintDAO {

    /**
     * Submit a new complaint by a student.
     * Uses actual table columns: complaint_type, description, complaint_date, status.
     */
    public boolean submitComplaint(String studentId, String category, String description) throws SQLException {
        String sql = "INSERT INTO Complaints (student_id, complaint_type, description, complaint_date, status) " +
                     "VALUES (?, ?, ?, ?, 'Pending')";

        int numericStudentId = parseStudentId(studentId);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numericStudentId);
            ps.setString(2, category);
            ps.setString(3, description);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get complaints submitted by a student.
     */
    public List<Map<String, Object>> getComplaintsByStudent(String studentId) throws SQLException {
        String sql = "SELECT complaint_id, student_id, complaint_type, description, complaint_date, status, admin_remarks " +
                     "FROM Complaints WHERE student_id = ? ORDER BY complaint_date DESC";
        int numericStudentId = parseStudentId(studentId);
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numericStudentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("complaintID", rs.getInt("complaint_id"));
                    map.put("category", rs.getString("complaint_type"));
                    map.put("description", rs.getString("description"));
                    map.put("date", rs.getTimestamp("complaint_date") != null ? rs.getTimestamp("complaint_date").toString() : "");
                    map.put("status", rs.getString("status"));
                    map.put("remarks", rs.getString("admin_remarks"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    /**
     * Get all complaints (for Warden / Admin dashboard).
     */
    public List<Map<String, Object>> getAllComplaints() throws SQLException {
        String sql = "SELECT complaint_id, student_id, complaint_type, description, complaint_date, status, admin_remarks " +
                     "FROM Complaints ORDER BY complaint_date DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("complaintID", rs.getInt("complaint_id"));
                map.put("studentID", String.valueOf(rs.getInt("student_id")));
                map.put("category", rs.getString("complaint_type"));
                map.put("description", rs.getString("description"));
                map.put("date", rs.getTimestamp("complaint_date") != null ? rs.getTimestamp("complaint_date").toString() : "");
                map.put("status", rs.getString("status"));
                map.put("remarks", rs.getString("admin_remarks"));
                list.add(map);
            }
        }
        return list;
    }

    /**
     * Update complaint status and remarks.
     */
    public boolean updateStatus(int complaintId, String newStatus, String remarks) throws SQLException {
        String sql = "UPDATE Complaints SET status = ?, admin_remarks = ? WHERE complaint_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, remarks != null ? remarks : "Updated by hostel office");
            ps.setInt(3, complaintId);
            return ps.executeUpdate() > 0;
        }
    }

    private int parseStudentId(String idStr) {
        if (idStr == null) return 1;
        try {
            return Integer.parseInt(idStr.replaceAll("\\D+", ""));
        } catch (Exception e) {
            return Math.abs(idStr.hashCode()) % 100000;
        }
    }
}
