package com.hostelconnect.dao;

import com.hostelconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDAO {

    /**
     * Get all events and announcements from Events table.
     */
    public List<Map<String, Object>> getAllEvents() throws SQLException {
        String sql = "SELECT event_id, event_name, description, event_date, event_time, venue, organized_by, created_at " +
                     "FROM Events ORDER BY event_date DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("eventID", rs.getInt("event_id"));
                map.put("title", rs.getString("event_name"));
                map.put("description", rs.getString("description"));
                map.put("date", rs.getDate("event_date") != null ? rs.getDate("event_date").toString() : "");
                map.put("time", rs.getTime("event_time") != null ? rs.getTime("event_time").toString() : "");
                map.put("venue", rs.getString("venue"));
                map.put("organizedBy", rs.getString("organized_by"));
                list.add(map);
            }
        }
        return list;
    }

    /**
     * Create an event or announcement (Admin function).
     */
    public boolean addEvent(String title, Date date, String description, String venue, String organizer) throws SQLException {
        String sql = "INSERT INTO Events (event_name, description, event_date, event_time, venue, organized_by) " +
                     "VALUES (?, ?, ?, '10:00:00', ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setDate(3, date);
            ps.setString(4, venue != null && !venue.isEmpty() ? venue : "Hostel Common Area");
            ps.setString(5, organizer != null && !organizer.isEmpty() ? organizer : "Hostel Administration");
            return ps.executeUpdate() > 0;
        }
    }
}
