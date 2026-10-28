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
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/student/*")
public class StudentServlet extends HttpServlet {

    private final RoomDAO roomDAO = new RoomDAO();
    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final FeeDAO feeDAO = new FeeDAO();
    private final EventDAO eventDAO = new EventDAO();
    private final LostFoundDAO lostFoundDAO = new LostFoundDAO();
    private final ApplicationDAO applicationDAO = new ApplicationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Not authenticated.\"}");
            return;
        }

        String studentId = user.getUserID();
        String path = req.getPathInfo();

        try {
            if (path == null || "/dashboard".equals(path) || "/".equals(path)) {
                Map<String, Object> data = new HashMap<>();
                data.put("student", user);

                // Room & Roommates
                Map<String, Object> room = roomDAO.getStudentRoom(studentId);
                data.put("room", room);
                if (room != null && room.get("roomID") != null) {
                    data.put("roommates", roomDAO.getRoommates(studentId, (String) room.get("roomID")));
                } else {
                    data.put("roommates", java.util.Collections.emptyList());
                }

                // Application
                data.put("application", applicationDAO.getByStudentId(studentId));

                // Complaints
                data.put("complaints", complaintDAO.getComplaintsByStudent(studentId));

                // Fees
                data.put("fees", feeDAO.getFeesByStudent(studentId));

                // Events
                data.put("events", eventDAO.getAllEvents());

                // Lost & Found
                data.put("lostFound", lostFoundDAO.getAllReports());

                resp.getWriter().write(JsonUtil.toJson(data));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"success\":false,\"message\":\"Unknown student endpoint.\"}");
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
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Not authenticated.\"}");
            return;
        }

        String studentId = user.getUserID();
        String path = req.getPathInfo();
        Map<String, Object> result = new HashMap<>();

        try {
            if ("/complaint".equals(path)) {
                String category = req.getParameter("category");
                String description = req.getParameter("description");
                if (category == null || description == null || description.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    result.put("success", false);
                    result.put("message", "Category and description are required.");
                } else {
                    boolean ok = complaintDAO.submitComplaint(studentId, category, description);
                    result.put("success", ok);
                    result.put("message", ok ? "Complaint submitted successfully." : "Failed to submit complaint.");
                }
            } else if ("/roommate-preference".equals(path)) {
                String allocationId = req.getParameter("allocationId");
                String details = req.getParameter("details");
                if (details == null || details.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    result.put("success", false);
                    result.put("message", "Preference details are required.");
                } else {
                    if (allocationId == null || allocationId.isEmpty()) {
                        Map<String, Object> room = roomDAO.getStudentRoom(studentId);
                        if (room != null) {
                            allocationId = (String) room.get("allocationID");
                        }
                    }
                    if (allocationId == null) {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        result.put("success", false);
                        result.put("message", "You must have an active room allocation before submitting roommate preferences.");
                    } else {
                        boolean ok = roomDAO.submitRoommatePreference(allocationId, details);
                        result.put("success", ok);
                        result.put("message", ok ? "Roommate preference submitted successfully." : "Submission failed.");
                    }
                }
            } else if ("/lost-found".equals(path)) {
                String itemName = req.getParameter("itemName");
                String description = req.getParameter("description");
                String status = req.getParameter("status"); // 'Lost' or 'found'
                if (itemName == null || description == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    result.put("success", false);
                    result.put("message", "Item name and description are required.");
                } else {
                    boolean ok = lostFoundDAO.reportItem(studentId, itemName, description, status);
                    result.put("success", ok);
                    result.put("message", ok ? "Item report submitted." : "Failed to submit item report.");
                }
            } else if ("/pay-fee".equals(path)) {
                String feeType = req.getParameter("feeType");
                String amountStr = req.getParameter("amount");
                if (feeType == null || amountStr == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    result.put("success", false);
                    result.put("message", "Fee type and amount are required.");
                } else {
                    BigDecimal amount = new BigDecimal(amountStr);
                    boolean ok = feeDAO.recordPayment(studentId, feeType, amount, "Success");
                    result.put("success", ok);
                    result.put("message", ok ? "Payment of ₹" + amount + " recorded successfully." : "Payment recording failed.");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                result.put("success", false);
                result.put("message", "Action not recognized.");
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
}
