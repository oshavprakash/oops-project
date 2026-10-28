package com.hostelconnect.servlet;

import com.hostelconnect.dao.*;
import com.hostelconnect.util.DBConnection;
import com.hostelconnect.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {

    private final FeeDAO feeDAO = new FeeDAO();
    private final EventDAO eventDAO = new EventDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final ComplaintDAO complaintDAO = new ComplaintDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getPathInfo();
        try {
            if (path == null || "/dashboard".equals(path) || "/".equals(path)) {
                Map<String, Object> data = new HashMap<>();

                // Users list
                data.put("users", getAllUsers());

                // Fee monitor
                data.put("fees", feeDAO.getAllFees());

                // Rooms overview
                data.put("rooms", roomDAO.getAllRooms());

                // Complaints overview
                data.put("complaints", complaintDAO.getAllComplaints());

                // Events
                data.put("events", eventDAO.getAllEvents());

                resp.getWriter().write(JsonUtil.toJson(data));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"success\":false,\"message\":\"Unknown admin endpoint.\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Database error: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getPathInfo();
        Map<String, Object> result = new HashMap<>();

        try {
            if ("/event".equals(path)) {
                String title = req.getParameter("title");
                String dateStr = req.getParameter("date");
                String description = req.getParameter("description");
                String venue = req.getParameter("venue");
                String organizer = req.getParameter("organizer");

                if (title == null || dateStr == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    result.put("success", false);
                    result.put("message", "Title and date are required.");
                } else {
                    Date date = Date.valueOf(dateStr);
                    boolean ok = eventDAO.addEvent(title, date, description, venue, organizer);
                    result.put("success", ok);
                    result.put("message", ok ? "Event published successfully." : "Failed to publish event.");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                result.put("success", false);
                result.put("message", "Unknown admin action.");
            }
            resp.getWriter().write(JsonUtil.toJson(result));
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("success", false);
            result.put("message", "Database error: " + e.getMessage());
            resp.getWriter().write(JsonUtil.toJson(result));
        }
    }

    private List<Map<String, Object>> getAllUsers() throws SQLException {
        String sql = "SELECT userID, name, email, role FROM users ORDER BY role ASC, name ASC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("userID", rs.getString("userID"));
                map.put("name", rs.getString("name"));
                map.put("email", rs.getString("email"));
                map.put("role", rs.getString("role"));
                list.add(map);
            }
        }
        return list;
    }
}
