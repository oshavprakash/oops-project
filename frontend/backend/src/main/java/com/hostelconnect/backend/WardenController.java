
package com.hostelconnect.backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warden")
@CrossOrigin(
    origins = "https://oshavprakash.github.io",
    allowCredentials = "true"
)
public class WardenController {

    private final JdbcTemplate jdbcTemplate;

    public WardenController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

   private String getWardenId(HttpSession session) {
    String role = (String) session.getAttribute("userRole");
    Object id = session.getAttribute("userID");

    System.out.println("Session ID: " + session.getId());
    System.out.println("Session role: " + role);
    System.out.println("Session userID: " + id);

    if (!"warden".equalsIgnoreCase(role) || id == null) {
        return null;
    }

    return id.toString();
}

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "Warden access required"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(HttpSession session) {
        String wardenId = getWardenId(session);
        if (wardenId == null) return forbidden();

        List<Map<String, Object>> hostelRows =
            jdbcTemplate.queryForList(
                "SELECT HostelName FROM Wardens WHERE WardenID = ?",
                wardenId
            );

        if (hostelRows.isEmpty()) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "Warden record not found"));
        }

        List<Map<String, Object>> applications =
            jdbcTemplate.queryForList(
                "SELECT * FROM Applications ORDER BY appliedDate DESC"
            );

        List<Map<String, Object>> rooms =
            jdbcTemplate.queryForList(
                "SELECT * FROM Rooms ORDER BY roomID"
            );

        List<Map<String, Object>> complaints =
            jdbcTemplate.queryForList(
                "SELECT * FROM Complaints ORDER BY complaint_date DESC"
            );

        List<Map<String, Object>> inventory =
            jdbcTemplate.queryForList(
                "SELECT * FROM Inventory WHERE WardenID = ?",
                wardenId
            );

        return ResponseEntity.ok(Map.of(
            "applications", applications,
            "rooms", rooms,
            "complaints", complaints,
            "inventory", inventory
        ));
    }

    @PostMapping("/approve-application")
    public ResponseEntity<?> approveApplication(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        String wardenId = getWardenId(session);
        if (wardenId == null) return forbidden();

        Object applicationId = body.get("applicationID");
        if (applicationId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "applicationID is required"));
        }

        int updated = jdbcTemplate.update(
            "UPDATE Applications SET status = ? WHERE applicationID = ?",
            "Approved", applicationId
        );

        if (updated == 0) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "Application not found"));
        }

        return ResponseEntity.ok(
            Map.of("success", true, "message", "Application approved")
        );
    }

    @PostMapping("/reject-application")
    public ResponseEntity<?> rejectApplication(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        String wardenId = getWardenId(session);
        if (wardenId == null) return forbidden();

        Object applicationId = body.get("applicationID");
        if (applicationId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "applicationID is required"));
        }

        int updated = jdbcTemplate.update(
            "UPDATE Applications SET status = ? WHERE applicationID = ?",
            "Rejected", applicationId
        );

        if (updated == 0) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "Application not found"));
        }

        return ResponseEntity.ok(
            Map.of("success", true, "message", "Application rejected")
        );
    }

    @PostMapping("/allocate-room")
    @Transactional
    public ResponseEntity<?> allocateRoom(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        String wardenId = getWardenId(session);
        if (wardenId == null) return forbidden();

        Object studentId = body.get("studentID");
        Object roomId = body.get("roomID");

        if (studentId == null || roomId == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "studentID and roomID are required")
            );
        }

        List<Map<String, Object>> hostelRows =
            jdbcTemplate.queryForList(
                "SELECT HostelName FROM Wardens WHERE WardenID = ?",
                wardenId
            );

        if (hostelRows.isEmpty()) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "Warden record not found"));
        }

        List<Map<String, Object>> roomRows =
            jdbcTemplate.queryForList(
                "SELECT roomID, capacity, occupancy, status " +
                "FROM Rooms WHERE roomID = ? FOR UPDATE",
                roomId
            );

        if (roomRows.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Room not found in your hostel"));
        }

        Map<String, Object> room = roomRows.get(0);
        int capacity = ((Number) room.get("capacity")).intValue();
        int occupancy = ((Number) room.get("occupancy")).intValue();

        if (occupancy >= capacity) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Room is full"));
        }

        Integer existing = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM RoomAllocations WHERE studentID = ?",
            Integer.class, studentId
        );

        if (existing != null && existing > 0) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Student already has a room allocation")
            );
        }

        jdbcTemplate.update(
            "INSERT INTO RoomAllocations " +
            "(studentID, roomID, allocationDate) " +
            "VALUES (?, ?, CURRENT_DATE)",
            studentId, roomId
        );

        jdbcTemplate.update(
            "UPDATE Rooms SET occupancy = occupancy + 1 WHERE roomID = ?",
            roomId
        );

        return ResponseEntity.ok(
            Map.of("success", true, "message", "Room allocated")
        );
    }

    @PostMapping("/update-complaint")
    public ResponseEntity<?> updateComplaint(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        String wardenId = getWardenId(session);
        if (wardenId == null) return forbidden();

        Object complaintId = body.get("complaint_id");
        Object status = body.get("status");

        if (complaintId == null || status == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "complaint_id and status are required")
            );
        }

        int updated = jdbcTemplate.update(
            "UPDATE Complaints SET status = ? WHERE complaint_id = ?",
            status.toString(), complaintId
        );

        if (updated == 0) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "Complaint not found"));
        }

        return ResponseEntity.ok(
            Map.of("success", true, "message", "Complaint updated")
        );
    }

    @PostMapping("/inventory")
    public ResponseEntity<?> addInventory(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        String wardenId = getWardenId(session);
        if (wardenId == null) return forbidden();

        Object itemName = body.get("itemName");
        Object category = body.get("category");
        Object quantityValue = body.get("quantity");
        Object statusValue = body.get("status");

        if (itemName == null || category == null ||
                quantityValue == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "itemName, category and quantity are required")
            );
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityValue.toString());
            if (quantity < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "quantity must be a non-negative integer"
                ));
        }

       String itemId = "INV" + java.util.UUID.randomUUID()
        .toString()
        .replace("-", "")
        .substring(0, 7)
        .toUpperCase();

jdbcTemplate.update(
    "INSERT INTO Inventory " +
    "(itemID, wardenID, itemName, category, quantity, status) " +
    "VALUES (?, ?, ?, ?, ?, ?)",
    itemId,
    wardenId,
    itemName.toString(),
    category.toString(),
    quantity,
    statusValue == null ? "Available" : statusValue.toString()
);

        return ResponseEntity.ok(
            Map.of("success", true, "message", "Inventory item added")
        );
    }

    // GET: List events for the warden dashboard.
    @GetMapping("/events")
    public ResponseEntity<?> getEvents(HttpSession session) {
        String wardenId = getWardenId(session);
        if (wardenId == null) return forbidden();

        String sql = """
            SELECT `event id`, `event name`, description,
                   event_date, `event time`, venue,
                   organized_by, created_at
            FROM Events
            ORDER BY created_at DESC
            """;

        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    // POST: Create an event. Warden login is required.
    @PostMapping("/events")
    public ResponseEntity<?> createEvent(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        String wardenId = getWardenId(session);
        if (wardenId == null) return forbidden();

        String eventName = body.get("event_name") == null
            ? "" : body.get("event_name").toString().trim();

        String description = body.get("description") == null
            ? "" : body.get("description").toString().trim();

        String eventDate = body.get("event_date") == null
            ? "" : body.get("event_date").toString().trim();

        String eventTime = body.get("event_time") == null
            ? "" : body.get("event_time").toString().trim();

        String venue = body.get("venue") == null
            ? "" : body.get("venue").toString().trim();

        if (eventName.isBlank() || eventDate.isBlank()
                || eventTime.isBlank() || venue.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("error",
                    "Event name, date, time and venue are required")
            );
        }

        String sql = """
            INSERT INTO Events
                (`event name`, description, event_date,
                 `event time`, venue, organized_by)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(
            sql,
            eventName,
            description,
            eventDate,
            eventTime,
            venue,
            wardenId
        );

        return ResponseEntity.ok(
            Map.of(
                "success", true,
                "message", "Event created successfully"
            )
        );
    }
}
