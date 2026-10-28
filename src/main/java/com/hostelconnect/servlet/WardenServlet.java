package com.hostelconnect.servlet;

import com.hostelconnect.dao.*;
import com.hostelconnect.model.User;
import com.hostelconnect.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/warden/*")
public class WardenServlet extends HttpServlet {

    private final ApplicationDAO applicationDAO = new ApplicationDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getPathInfo();
        try {
            if (path == null || "/dashboard".equals(path) || "/".equals(path)) {
                Map<String, Object> data = new HashMap<>();
                data.put("applications", applicationDAO.getAllApplications());
                data.put("rooms", roomDAO.getAllRooms());
                data.put("complaints", complaintDAO.getAllComplaints());
                data.put("inventory", inventoryDAO.getAllInventory());
                data.put("roommatePreferences", roomDAO.getAllRoommatePreferences());
                resp.getWriter().write(JsonUtil.toJson(data));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"success\":false,\"message\":\"Unknown warden endpoint.\"}");
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

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        String wardenId = (user != null) ? user.getUserID() : "warden123";

        String path = req.getPathInfo();
        Map<String, Object> result = new HashMap<>();

        try {
            if ("/approve-application".equals(path)) {
                String appId = req.getParameter("applicationId");
                boolean ok = applicationDAO.updateStatus(appId, "approved");
                result.put("success", ok);
                result.put("message", ok ? "Student application approved." : "Failed to approve application.");
            } else if ("/reject-application".equals(path)) {
                String appId = req.getParameter("applicationId");
                boolean ok = applicationDAO.updateStatus(appId, "rejected");
                result.put("success", ok);
                result.put("message", ok ? "Student application rejected." : "Failed to reject application.");
            } else if ("/allocate-room".equals(path)) {
                String studentId = req.getParameter("studentId");
                String roomId = req.getParameter("roomId");
                boolean ok = roomDAO.allocateRoom(studentId, roomId);
                result.put("success", ok);
                result.put("message", ok ? "Room allocated successfully." : "Allocation failed (room may be full or student already allocated).");
            } else if ("/update-complaint".equals(path)) {
                String complaintIdStr = req.getParameter("complaintId");
                String status = req.getParameter("status");
                String remarks = req.getParameter("remarks");
                int complaintId = Integer.parseInt(complaintIdStr);
                boolean ok = complaintDAO.updateStatus(complaintId, status, remarks);
                result.put("success", ok);
                result.put("message", ok ? "Complaint status updated." : "Failed to update complaint.");
            } else if ("/inventory".equals(path)) {
                String action = req.getParameter("action");
                if ("add".equalsIgnoreCase(action)) {
                    String itemName = req.getParameter("itemName");
                    String category = req.getParameter("category");
                    int quantity = Integer.parseInt(req.getParameter("quantity"));
                    String status = req.getParameter("status");
                    boolean ok = inventoryDAO.addInventory(wardenId, itemName, category, quantity, status);
                    result.put("success", ok);
                    result.put("message", ok ? "Inventory item added." : "Failed to add inventory.");
                } else if ("update".equalsIgnoreCase(action)) {
                    String itemId = req.getParameter("itemId");
                    int quantity = Integer.parseInt(req.getParameter("quantity"));
                    String status = req.getParameter("status");
                    boolean ok = inventoryDAO.updateInventory(itemId, quantity, status);
                    result.put("success", ok);
                    result.put("message", ok ? "Inventory updated." : "Failed to update inventory.");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                result.put("success", false);
                result.put("message", "Unknown warden action.");
            }
            resp.getWriter().write(JsonUtil.toJson(result));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            resp.getWriter().write(JsonUtil.toJson(result));
        }
    }
}
