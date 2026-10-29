package com.hostelconnect.backend;

import org.springframework.jdbc.core.JdbcTemplate;
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
    public Map<String, Object> getDashboard() {

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT userID, name, email, role FROM users"
        );

        return Map.of(
            "users", users,
            "rooms", List.of(),
            "complaints", List.of(),
            "fees", List.of(),
            "events", List.of()
        );
    }
}
