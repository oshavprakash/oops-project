package com.hostelconnect.servlet;

import com.hostelconnect.model.User;
import com.hostelconnect.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth/me")
public class AuthMeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        Map<String, Object> result = new HashMap<>();

        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            result.put("loggedIn", true);
            result.put("userId", user.getUserID());
            result.put("name", user.getName());
            result.put("email", user.getEmail());
            result.put("role", user.getRole());
        } else {
            result.put("loggedIn", false);
        }

        resp.getWriter().write(JsonUtil.toJson(result));
    }
}
