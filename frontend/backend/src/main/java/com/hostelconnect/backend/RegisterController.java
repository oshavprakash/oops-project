package com.hostelconnect.backend;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RegisterController {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public RegisterController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping(
        value = "/register",
        consumes = "application/x-www-form-urlencoded"
    )
    public ResponseEntity<Map<String, Object>> register(
            @RequestParam String studentID,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password) {

        studentID = studentID.trim();
        name = name.trim();
        email = email.trim().toLowerCase();

        if (studentID.isEmpty() || studentID.length() > 10
                || name.isEmpty() || email.isEmpty()
                || password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Please check the registration details."
            ));
        }

        Integer existing = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ? OR userID = ?",
            Integer.class, email, studentID
        );

        if (existing != null && existing > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "message", "This email or user ID is already registered."
            ));
        }

        Integer existingStudent = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM students WHERE studentID = ? OR email = ?",
            Integer.class, studentID, email
        );

        if (existingStudent != null && existingStudent > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "message", "This student ID or email is already registered."
            ));
        }

        String hashedPassword = passwordEncoder.encode(password);
        String userID = UUID.randomUUID().toString().substring(0, 10);

        try {
            jdbcTemplate.update(
                "INSERT INTO users (userID, name, email, password, role) "
                + "VALUES (?, ?, ?, ?, ?)",
                userID, name, email, hashedPassword, "student"
            );

            jdbcTemplate.update(
                "INSERT INTO students (studentID, name, email) "
                + "VALUES (?, ?, ?)",
                studentID, name, email
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Registration successful!"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Registration could not be completed. Please check the database table structure."
                ));
        }
    }
}
