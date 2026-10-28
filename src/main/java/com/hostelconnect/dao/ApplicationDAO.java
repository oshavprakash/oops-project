package com.hostelconnect.dao;

import com.hostelconnect.model.Application;
import com.hostelconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationDAO {

    /**
     * Retrieves all student applications with student details from students table.
     */
    public List<Map<String, Object>> getAllApplications() throws SQLException {
        String sql = "SELECT a.applicationID, a.studentID, s.name, s.email, a.status, a.appliedDate, a.preferenceDescription " +
                     "FROM Applications a " +
                     "JOIN students s ON a.studentID = s.studentID " +
                     "ORDER BY a.appliedDate DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("applicationID", rs.getString("applicationID"));
                map.put("studentID", rs.getString("studentID"));
                map.put("studentName", rs.getString("name"));
                map.put("studentEmail", rs.getString("email"));
                map.put("status", rs.getString("status"));
                map.put("appliedDate", rs.getDate("appliedDate") != null ? rs.getDate("appliedDate").toString() : "");
                map.put("preferenceDescription", rs.getString("preferenceDescription"));
                list.add(map);
            }
        }
        return list;
    }

    /**
     * Updates an application status ('approved' or 'rejected').
     */
    public boolean updateStatus(String applicationId, String newStatus) throws SQLException {
        String sql = "UPDATE Applications SET status = ? WHERE applicationID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, applicationId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Gets application by student ID.
     */
    public Application getByStudentId(String studentId) throws SQLException {
        String sql = "SELECT applicationID, studentID, status, appliedDate, preferenceDescription " +
                     "FROM Applications WHERE studentID = ? ORDER BY appliedDate DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Application(
                        rs.getString("applicationID"),
                        rs.getString("studentID"),
                        rs.getString("status"),
                        rs.getDate("appliedDate"),
                        rs.getString("preferenceDescription")
                    );
                }
            }
        }
        return null;
    }
}
