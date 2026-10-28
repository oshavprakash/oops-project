package com.hostelconnect.filter;

import com.hostelconnect.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(urlPatterns = {
    "/Student.html", "/api/student/*",
    "/warden.html", "/api/warden/*",
    "/admin.html", "/api/admin/*"
})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        String role = (session != null) ? (String) session.getAttribute("role") : null;

        String uri = req.getRequestURI();
        boolean isApi = uri.contains("/api/");

        if (user == null || role == null) {
            if (isApi) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"success\":false,\"message\":\"Unauthorized. Please log in.\"}");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login1.html");
            }
            return;
        }

        // Role authorization check
        if (uri.contains("admin") && !"Administrator".equalsIgnoreCase(role)) {
            sendForbidden(resp, isApi, "Access denied: Administrator privileges required.");
            return;
        }

        if (uri.contains("warden") && !"Warden".equalsIgnoreCase(role) && !"Administrator".equalsIgnoreCase(role)) {
            sendForbidden(resp, isApi, "Access denied: Warden privileges required.");
            return;
        }

        if (uri.contains("Student") || uri.contains("/api/student/")) {
            if (!"Student".equalsIgnoreCase(role) && !"Administrator".equalsIgnoreCase(role)) {
                sendForbidden(resp, isApi, "Access denied: Student access required.");
                return;
            }
        }

        // Add headers to prevent caching of sensitive dashboard pages
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        chain.doFilter(request, response);
    }

    private void sendForbidden(HttpServletResponse resp, boolean isApi, String msg) throws IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        if (isApi) {
            resp.setContentType("application/json");
            resp.getWriter().write("{\"success\":false,\"message\":\"" + msg + "\"}");
        } else {
            resp.getWriter().write("<h1>403 Forbidden</h1><p>" + msg + "</p><a href='login1.html'>Return to Login</a>");
        }
    }
}
