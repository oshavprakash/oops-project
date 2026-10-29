package com.hostelconnect.backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(
    origins = "https://oshavprakash.github.io",
    allowCredentials = "true"
)
public class AdminDashboardController {

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "administrator".equals(role);
    }

    // ADMIN DASHBOARD
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "Admin access required"));
        }

        List<Map<String, Object>> users =
            jdbcTemplate.queryForList(
                "SELECT userID, name, email, role FROM users"
            );

        List<Map<String, Object>> rooms =
            jdbcTemplate.queryForList(
                "SELECT * FROM Rooms ORDER BY roomID"
            );

        long occupiedRooms = rooms.stream()
            .filter(room -> {
                Object occupancy = room.get("occupancy");
                return occupancy instanceof Number
                    && ((Number) occupancy).intValue() > 0;
            })
            .count();

        List<Map<String, Object>> fees =
            jdbcTemplate.queryForList(
                "SELECT f.paymentID, f.studentID, " +
                "s.name AS studentName, f.feeType, f.amount, " +
                "f.paymentDate, f.status " +
                "FROM FeePayments f " +
                "JOIN Students s ON f.studentID = s.studentID " +
                "ORDER BY f.paymentID"
            );

        return ResponseEntity.ok(Map.of(
            "users", users,
            "rooms", rooms,
            "totalRooms", rooms.size(),
            "occupiedRooms", occupiedRooms,
            "complaints", List.of(),
            "fees", fees,
            "events", List.of()
        ));
    }

    // GET STUDENTS FOR THE ASSIGN FEE DROPDOWN
    @GetMapping("/students")
    public ResponseEntity<?> getStudents(HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "Admin access required"));
        }

        List<Map<String, Object>> students =
            jdbcTemplate.queryForList(
                "SELECT studentID, name FROM Students ORDER BY name"
            );

        return ResponseEntity.ok(students);
    }

    // ASSIGN FEE MANUALLY
    @PostMapping("/assign-fee")
    public ResponseEntity<?> assignFee(
            @RequestBody Map<String, Object> request,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "Admin access required"));
        }

        String studentID = String.valueOf(
            request.getOrDefault("studentID", "")
        ).trim();

        String feeType = String.valueOf(
            request.getOrDefault("feeType", "")
        ).trim();

        BigDecimal amount;

        try {
            amount = new BigDecimal(
                String.valueOf(request.get("amount"))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Enter a valid fee amount"));
        }

        if (studentID.isEmpty() || feeType.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Select a student and fee type"));
        }

        if (feeType.length() > 50) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Fee type must be 50 characters or fewer"));
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.compareTo(new BigDecimal("99999999.99")) > 0
                || amount.scale() > 2) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Enter a valid positive amount with up to 2 decimal places"));
        }

        List<Map<String, Object>> student =
            jdbcTemplate.queryForList(
                "SELECT studentID FROM Students WHERE studentID = ?",
                studentID
            );

        if (student.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Student not found"));
        }

        // Generate a unique 10-character payment ID.
        String paymentID;
        int attempts = 0;

        do {
            paymentID = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10);

            attempts++;

            if (attempts > 5) {
                return ResponseEntity.status(500)
                    .body(Map.of("error", "Could not generate a payment ID"));
            }

        } while (Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM FeePayments WHERE paymentID = ?)",
                Boolean.class,
                paymentID
            )
        ));

        jdbcTemplate.update(
            "INSERT INTO FeePayments " +
            "(paymentID, studentID, feeType, amount, paymentDate, status) " +
            "VALUES (?, ?, ?, ?, CURRENT_DATE, 'Pending')",
            paymentID, studentID, feeType, amount
        );

        return ResponseEntity.ok(
            Map.of("message", "Fee assigned successfully",
                   "paymentID", paymentID)
        );
    }
}

