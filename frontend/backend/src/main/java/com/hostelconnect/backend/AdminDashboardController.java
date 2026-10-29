package com.hostelconnect.backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(HttpSession session) {

        String role = (String) session.getAttribute("userRole");

        if (role == null || !role.equals("administrator")) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Admin access required"));
        }

        // Fetch users
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT userID, name, email, role FROM users"
        );

        // Fetch all rooms
        List<Map<String, Object>> rooms = jdbcTemplate.queryForList(
            "SELECT * FROM Rooms ORDER BY roomID"
        );

        // Total number of rooms
        long totalRooms = rooms.size();

        // Count rooms that have reached full capacity
        long occupiedRooms = rooms.stream()
            .filter(room -> {
                Object occupancy = room.get("occupancy");
                Object capacity = room.get("capacity");

                return occupancy instanceof Number
                    && capacity instanceof Number
                    && ((Number) occupancy).intValue()
                        >= ((Number) capacity).intValue();
            })
            .count();

        // Fetch fee payment records
        List<Map<String, Object>> fees = jdbcTemplate.queryForList(
            "SELECT f.paymentID, f.studentID, s.name AS studentName, " +
            "f.feeType, f.amount, f.paymentDate, f.status " +
            "FROM FeePayments f " +
            "JOIN Students s ON f.studentID = s.studentID " +
            "ORDER BY f.paymentID"
        );

        return ResponseEntity.ok(Map.of(
            "users", users,
            "rooms", rooms,
            "totalRooms", totalRooms,
            "occupiedRooms", occupiedRooms,
            "complaints", List.of(),
            "fees", fees,
            "events", List.of()
        ));
    }
}

