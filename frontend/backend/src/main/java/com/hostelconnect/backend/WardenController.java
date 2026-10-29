
package com.hostelconnect.backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // Check that the logged-in user is a warden.
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

    // ---------------- DASHBOARD ----------------

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
        "SELECT a.*, s.name AS studentName, " +
        "       ra.roomID AS allocatedRoomID, " +
        "       CASE WHEN ra.allocationID IS NOT NULL " +
        "            THEN TRUE ELSE FALSE END AS allocated " +
        "FROM Applications a " +
        "LEFT JOIN Students s ON a.studentID = s.studentID " +
        "LEFT JOIN RoomAllocations ra ON a.studentID = ra.studentID " +
        "ORDER BY a.appliedDate DESC"
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

    // ---------------- APPLICATIONS ----------------

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

    // ---------------- ROOM ALLOCATION ----------------

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
            Integer.class,
            studentId
        );

        if (existing != null && existing > 0) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Student already has a room allocation")
            );
        }

        String allocationId = "AL" +
            UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        jdbcTemplate.update(
            "INSERT INTO RoomAllocations " +
            "(allocationID, studentID, roomID, allocationDate) " +
            "VALUES (?, ?, ?, CURRENT_DATE)",
            allocationId,
            studentId,
            roomId
        );

        jdbcTemplate.update(
            "UPDATE Rooms SET occupancy = occupancy + 1 WHERE roomID = ?",
            roomId
        );

        return ResponseEntity.ok(
            Map.of("success", true, "message", "Room allocated")
        );
    }

    // ---------------- COMPLAINTS ----------------

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
            status.toString(),
            complaintId
        );

        if (updated == 0) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "Complaint not found"));
        }

        return ResponseEntity.ok(
            Map.of("success", true, "message", "Complaint updated")
        );
    }

    // ---------------- INVENTORY ----------------

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

        if (itemName == null || category == null || quantityValue == null) {
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

        String itemId = "INV" +
            UUID.randomUUID()
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

    // ---------------- EVENTS ----------------

    @GetMapping("/events")
    public ResponseEntity<?> getEvents(HttpSession session) {
        String wardenId = getWardenId(session);

        if (wardenId == null) {
            return forbidden();
        }

        // Match the column names in the TiDB Events table.
        String sql = """
            SELECT event_id, event_name, description,
                   event_date, event_time, venue, organized_by
            FROM Events
            ORDER BY event_id DESC
            """;

        try {
            return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(500).body(
                Map.of("error", "Could not load events from the database")
            );
        }
    }

    @PostMapping("/events")
    public ResponseEntity<?> createEvent(
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        String wardenId = getWardenId(session);

        if (wardenId == null) {
            return forbidden();
        }

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
                Map.of(
                    "error",
                    "Event name, date, time and venue are required"
                )
            );
        }

        // event_time uses an underscore in the database.
        // event_id is auto-incremented by TiDB.
        String sql = """
            INSERT INTO Events
                (event_name, description, event_date,
                 event_time, venue, organized_by)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try {
            jdbcTemplate.update(
                sql,
                eventName,
                description,
                eventDate,
                eventTime,
                venue,
                wardenId
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(500).body(
                Map.of("error", "Could not save the event to the database")
            );
        }

        return ResponseEntity.ok(
            Map.of(
                "success", true,
                "message", "Event created successfully"
            )
        );
    }
}
