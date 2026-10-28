package com.hostelconnect.dao;

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

public class RoomDAO {

    /**
     * Get all rooms with hostel information.
     */
    public List<Map<String, Object>> getAllRooms() throws SQLException {
        String sql = "SELECT r.roomID, r.hostelID, h.name as hostelName, r.roomType, r.capacity, r.occupancy, r.status " +
                     "FROM Rooms r LEFT JOIN Hostels h ON r.hostelID = h.hostelID";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("roomID", rs.getString("roomID"));
                map.put("hostelID", rs.getString("hostelID"));
                map.put("hostelName", rs.getString("hostelName"));
                map.put("roomType", rs.getString("roomType"));
                map.put("capacity", rs.getInt("capacity"));
                map.put("occupancy", rs.getInt("occupancy"));
                map.put("status", rs.getString("status"));
                list.add(map);
            }
        }
        return list;
    }

    /**
     * Get student's room allocation details.
     */
    public Map<String, Object> getStudentRoom(String studentId) throws SQLException {
        String sql = "SELECT ra.allocationID, ra.roomID, ra.allocationDate, r.roomType, r.capacity, r.occupancy, h.name as hostelName " +
                     "FROM RoomAllocations ra " +
                     "JOIN Rooms r ON ra.roomID = r.roomID " +
                     "LEFT JOIN Hostels h ON r.hostelID = h.hostelID " +
                     "WHERE ra.studentID = ? ORDER BY ra.allocationDate DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("allocationID", rs.getString("allocationID"));
                    map.put("roomID", rs.getString("roomID"));
                    map.put("allocationDate", rs.getDate("allocationDate") != null ? rs.getDate("allocationDate").toString() : "");
                    map.put("roomType", rs.getString("roomType"));
                    map.put("capacity", rs.getInt("capacity"));
                    map.put("occupancy", rs.getInt("occupancy"));
                    map.put("hostelName", rs.getString("hostelName"));
                    return map;
                }
            }
        }
        return null;
    }

    /**
     * Get roommates for a student sharing the same room.
     */
    public List<Map<String, Object>> getRoommates(String studentId, String roomId) throws SQLException {
        String sql = "SELECT s.studentID, s.name, s.email " +
                     "FROM RoomAllocations ra " +
                     "JOIN students s ON ra.studentID = s.studentID " +
                     "WHERE ra.roomID = ? AND ra.studentID != ?";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setString(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("studentID", rs.getString("studentID"));
                    map.put("name", rs.getString("name"));
                    map.put("email", rs.getString("email"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    /**
     * Allocate a room to a student inside a transaction.
     */
    public boolean allocateRoom(String studentId, String roomId) throws SQLException {
        String checkSql = "SELECT allocationID FROM RoomAllocations WHERE studentID = ?";
        String insertAllocSql = "INSERT INTO RoomAllocations (allocationID, studentID, roomID, allocationDate) VALUES (?, ?, ?, ?)";
        String updateRoomSql = "UPDATE Rooms SET occupancy = occupancy + 1, " +
                               "status = CASE WHEN occupancy + 1 >= capacity THEN 'full' ELSE 'Available' END " +
                               "WHERE roomID = ? AND occupancy < capacity";

        String allocId = UserDAO.generateId("ALC", 7);

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Check if already allocated
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, studentId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Update room occupancy
            try (PreparedStatement updatePs = conn.prepareStatement(updateRoomSql)) {
                updatePs.setString(1, roomId);
                int updated = updatePs.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // Insert allocation record
            try (PreparedStatement allocPs = conn.prepareStatement(insertAllocSql)) {
                allocPs.setString(1, allocId);
                allocPs.setString(2, studentId);
                allocPs.setString(3, roomId);
                allocPs.setDate(4, Date.valueOf(LocalDate.now()));
                allocPs.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Submit roommate preference / change request.
     */
    public boolean submitRoommatePreference(String allocationId, String details) throws SQLException {
        String sql = "INSERT INTO RoommatePreferences (preferenceID, allocationID, preferenceDetails) VALUES (?, ?, ?)";
        String prefId = UserDAO.generateId("PREF", 6);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefId);
            ps.setString(2, allocationId);
            ps.setString(3, details);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get all roommate preferences / change requests for warden review.
     */
    public List<Map<String, Object>> getAllRoommatePreferences() throws SQLException {
        String sql = "SELECT rp.preferenceID, rp.allocationID, rp.preferenceDetails, ra.studentID, s.name, ra.roomID " +
                     "FROM RoommatePreferences rp " +
                     "JOIN RoomAllocations ra ON rp.allocationID = ra.allocationID " +
                     "JOIN students s ON ra.studentID = s.studentID";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("preferenceID", rs.getString("preferenceID"));
                map.put("allocationID", rs.getString("allocationID"));
                map.put("preferenceDetails", rs.getString("preferenceDetails"));
                map.put("studentID", rs.getString("studentID"));
                map.put("studentName", rs.getString("name"));
                map.put("roomID", rs.getString("roomID"));
                list.add(map);
            }
        }
        return list;
    }
}
