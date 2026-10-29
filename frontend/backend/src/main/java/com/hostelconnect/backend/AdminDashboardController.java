
package com.hostelconnect.backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(HttpSession session) {

        String role = (String) session.getAttribute("userRole");

        if (role == null || !role.equals("administrator")) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Admin access required"));
        }

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT userID, name, email, role FROM users"
        );

        return ResponseEntity.ok(Map.of(
            "users", users,
            "rooms", List.of(),
            "complaints", List.of(),
            "fees", List.of(),
            "events", List.of()
        ));
    }
}
