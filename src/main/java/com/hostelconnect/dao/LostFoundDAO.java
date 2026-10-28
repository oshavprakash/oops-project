package com.hostelconnect.dao;

import com.hostelconnect.model.LostFound;
import com.hostelconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LostFoundDAO {

    /**
     * Get all lost and found reports with reporter name.
     */
    public List<Map<String, Object>> getAllReports() throws SQLException {
        String sql = "SELECT lf.itemID, lf.studentID, s.name as studentName, lf.itemName, lf.description, lf.date, lf.status " +
                     "FROM LostFound lf " +
                     "LEFT JOIN students s ON lf.studentID = s.studentID " +
                     "ORDER BY lf.date DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("itemID", rs.getString("itemID"));
                map.put("studentID", rs.getString("studentID"));
                map.put("studentName", rs.getString("studentName"));
                map.put("itemName", rs.getString("itemName"));
                map.put("description", rs.getString("description"));
                map.put("date", rs.getDate("date") != null ? rs.getDate("date").toString() : "");
                map.put("status", rs.getString("status"));
                list.add(map);
            }
        }
        return list;
    }

    /**
     * Report a lost or found item.
     */
    public boolean reportItem(String studentId, String itemName, String description, String status) throws SQLException {
        String sql = "INSERT INTO LostFound (itemID, studentID, itemName, description, date, status) VALUES (?, ?, ?, ?, ?, ?)";
        String itemId = UserDAO.generateId("LST", 7);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemId);
            ps.setString(2, studentId);
            ps.setString(3, itemName);
            ps.setString(4, description);
            ps.setDate(5, Date.valueOf(LocalDate.now()));
            ps.setString(6, status != null ? status : "Lost");
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Update item status (e.g. 'claimed').
     */
    public boolean updateStatus(String itemId, String status) throws SQLException {
        String sql = "UPDATE LostFound SET status = ? WHERE itemID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, itemId);
            return ps.executeUpdate() > 0;
        }
    }
}
