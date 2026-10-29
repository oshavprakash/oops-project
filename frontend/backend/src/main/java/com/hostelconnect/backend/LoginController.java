package com.hostelconnect.backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(
    origins = "https://oshavprakash.github.io",
    allowCredentials = "true"
)
public class LoginController {

    private final JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public LoginController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping(
        value = "/login",
        consumes = "application/x-www-form-urlencoded"
    )
    public ResponseEntity<Map<String, Object>> login(
           @RequestParam String username,
           @RequestParam String password,
            HttpSession session) {

        String input = username.trim();

        try {
            var users = jdbcTemplate.query(
                "SELECT userID, name, email, password, role " +
                "FROM users WHERE email = ? OR userID = ?",
                (rs, rowNum) -> new User(
                    rs.getString("userID"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("role")
                ),
                input,
                input
            );

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "success", false,
                        "message", "Invalid ID/email or password."
                    ));
            }

            User user = users.get(0);

            if (!passwordEncoder.matches(password, user.password())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "success", false,
                        "message", "Invalid ID/email or password."
                    ));
            }
            session.setAttribute("userID", user.userID());
            session.setAttribute("userRole", user.role().toLowerCase());

            String redirect = switch (user.role().toLowerCase()) {
                case "student" -> "Student.html";
                case "warden" -> "warden.html";
                case "administrator" -> "admin.html";
                default -> "Student.html";
            };

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Login successful!",
                "userName", user.name(),
                "redirect", redirect
            ));

        } catch (Exception e) {
            System.err.println("Login database error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "A server error occurred during login."
                ));
        }
    }

    private record User(
        String userID,
        String name,
        String email,
        String password,
        String role
    ) {}
}
