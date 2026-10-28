package com.hostelconnect.servlet;

import com.hostelconnect.dao.UserDAO;
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

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        Map<String, Object> result = new HashMap<>();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("message", "Username and password are required.");
            resp.getWriter().write(JsonUtil.toJson(result));
            return;
        }

        username = username.trim();

        try {
            User user = userDAO.authenticate(username, password);
            if (user == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                result.put("success", false);
                result.put("message", "Invalid username/email or password.");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            // Role and approval verification
            String role = user.getRole();
            if ("Student".equalsIgnoreCase(role)) {
                String appStatus = userDAO.getStudentApplicationStatus(user.getUserID());
                if (appStatus == null || "Pending".equalsIgnoreCase(appStatus)) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    result.put("success", false);
                    result.put("pending", true);
                    result.put("message", "Your registration is currently PENDING approval by the warden. You cannot access dashboard features until approved.");
                    resp.getWriter().write(JsonUtil.toJson(result));
                    return;
                } else if ("rejected".equalsIgnoreCase(appStatus)) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    result.put("success", false);
                    result.put("rejected", true);
                    result.put("message", "Your hostel registration application was rejected. Please contact the hostel administration.");
                    resp.getWriter().write(JsonUtil.toJson(result));
                    return;
                }
            }

            // Create secure session
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("role", user.getRole());
            session.setAttribute("userId", user.getUserID());
            session.setAttribute("userName", user.getName());

            result.put("success", true);
            result.put("role", user.getRole());
            result.put("userId", user.getUserID());
            result.put("userName", user.getName());

            if ("Student".equalsIgnoreCase(role)) {
                result.put("redirect", "Student.html");
            } else if ("Warden".equalsIgnoreCase(role)) {
                result.put("redirect", "warden.html");
            } else if ("Administrator".equalsIgnoreCase(role)) {
                result.put("redirect", "admin.html");
            } else {
                result.put("redirect", "homepage.html");
            }

            resp.getWriter().write(JsonUtil.toJson(result));
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("success", false);
            result.put("message", "Database error occurred during login. Please try again.");
            resp.getWriter().write(JsonUtil.toJson(result));
        }
    }
}
