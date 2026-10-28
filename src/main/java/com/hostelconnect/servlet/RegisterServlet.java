package com.hostelconnect.servlet;

import com.hostelconnect.dao.UserDAO;
import com.hostelconnect.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String preference = req.getParameter("preference");

        Map<String, Object> result = new HashMap<>();

        if (username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("message", "All fields are required.");
            resp.getWriter().write(JsonUtil.toJson(result));
            return;
        }

        username = username.trim();
        email = email.trim();

        if (username.length() > 10) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("message", "Username / ID must be 10 characters or less to fit database constraints.");
            resp.getWriter().write(JsonUtil.toJson(result));
            return;
        }

        if (password.length() < 8) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("message", "Password must be at least 8 characters long.");
            resp.getWriter().write(JsonUtil.toJson(result));
            return;
        }

        try {
            if (userDAO.existsById(username)) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                result.put("success", false);
                result.put("message", "Username / Student ID is already taken. Please choose another.");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            if (userDAO.existsByEmail(email)) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                result.put("success", false);
                result.put("message", "An account with this email address is already registered.");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            boolean registered = userDAO.registerStudent(username, username, email, password, preference);
            if (registered) {
                result.put("success", true);
                result.put("message", "Registration successful! Your application is PENDING approval by the warden.");
                resp.getWriter().write(JsonUtil.toJson(result));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                result.put("success", false);
                result.put("message", "Failed to register student. Please try again.");
                resp.getWriter().write(JsonUtil.toJson(result));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("success", false);
            result.put("message", "Database error occurred during registration. Please check server logs.");
            resp.getWriter().write(JsonUtil.toJson(result));
        }
    }
}
