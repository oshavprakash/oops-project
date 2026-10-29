package com.hostelconnect.backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(
    origins = "https://oshavprakash.github.io",
    allowCredentials = "true"
)
public class StudentController {

    private final JdbcTemplate jdbc;

    public StudentController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    

    // -------------------- STUDENT AUTHENTICATION --------------------

    private String getStudentID(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        String userID = (String) session.getAttribute("userID");

        if (userID == null || !"student".equalsIgnoreCase(role)) {
            throw new RuntimeException("UNAUTHORIZED");
        }

        List<String> ids = jdbc.query(
            "SELECT s.studentID FROM Students s " +
            "JOIN Users u ON LOWER(u.email) = LOWER(s.email) " +
            "WHERE u.userID = ? AND LOWER(u.role) = 'student'",
            (rs, n) -> rs.getString("studentID"),
            userID
        );

        if (ids.isEmpty()) {
            throw new RuntimeException("STUDENT_RECORD_NOT_FOUND");
        }

        return ids.get(0);
    }

    // -------------------- ROOM DETAILS --------------------

 private Map<String, Object> room(String sid) {
    List<Map<String, Object>> rows = jdbc.queryForList(
        "SELECT a.allocationID, r.roomID AS roomID, " +
        "r.roomType, r.capacity, r.occupancy, r.status AS roomStatus, " +
        "a.allocationDate " +
        "FROM RoomAllocations a " +
        "LEFT JOIN Rooms r ON r.roomID = a.roomID " +
        "WHERE a.studentID = ? " +
        "ORDER BY a.allocationDate DESC LIMIT 1",
        sid
    );

    System.out.println("Room lookup studentID: " + sid);
    System.out.println("Room lookup result: " + rows);

    return rows.isEmpty() ? null : rows.get(0);
}

    // -------------------- APPLICATIONS --------------------

    private List<Map<String, Object>> applications(String sid) {
        return jdbc.queryForList(
            "SELECT applicationID, appliedDate, " +
            "preferenceDescription, status FROM Applications " +
            "WHERE studentID = ? ORDER BY appliedDate DESC",
            sid
        );
    }

    // -------------------- COMPLAINTS --------------------

    private List<Map<String, Object>> complaints(
            String sid, String status, int limit) {

        Map<String, Object> currentRoom = room(sid);

        String roomID = currentRoom == null
            ? "-" : String.valueOf(currentRoom.get("roomID"));

        List<Map<String, Object>> rows;

        if (status == null || status.isBlank()
                || "All".equalsIgnoreCase(status)) {

            rows = jdbc.queryForList(
                "SELECT complaint_id AS complaintID, " +
                "complaint_type AS category, description, " +
                "complaint_date AS date, status FROM Complaints " +
                "WHERE student_id = ? " +
                "ORDER BY complaint_date DESC LIMIT ?",
                sid, limit
            );

        } else {
            rows = jdbc.queryForList(
                "SELECT complaint_id AS complaintID, " +
                "complaint_type AS category, description, " +
                "complaint_date AS date, status FROM Complaints " +
                "WHERE student_id = ? AND status = ? " +
                "ORDER BY complaint_date DESC LIMIT ?",
                sid, status, limit
            );
        }

        rows.forEach(c -> c.put("room", roomID));

        return rows;
    }

    // -------------------- OVERVIEW --------------------

    @GetMapping("/overview")
    public ResponseEntity<?> overview(HttpSession session) {
        try {
            String sid = getStudentID(session);

            Map<String, Object> result = new LinkedHashMap<>();

            // Student name
            String studentName = jdbc.queryForObject(
                "SELECT name FROM Students WHERE studentID = ?",
                String.class, sid
            );
            result.put("name", studentName);
            result.put("student", Map.of("name", studentName));

            // Room information
            Map<String, Object> roomInfo = room(sid);
            result.put("room", roomInfo);
            // Latest hostel application
            List<Map<String, Object>> applications = jdbc.queryForList(
                "SELECT applicationID, status, appliedDate, preferenceDescription " +
                "FROM Applications " +
                "WHERE studentID = ? " +
                "ORDER BY appliedDate DESC LIMIT 1",
                sid
            );

            result.put(
                "application",
            applications.isEmpty() ? null : applications.get(0)
            );

            // Total pending fees
            BigDecimal due = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) " +
                "FROM FeePayments " +
                "WHERE studentID = ? AND LOWER(status) = 'pending'",
                BigDecimal.class, sid
            );

            if (due == null) {
                due = BigDecimal.ZERO;
            }

            result.put("totalDue", due);
            result.put(
                "feeStatus",
                due.signum() > 0 ? "Due" : "Paid"
            );

            // Actual fee records for the dashboard
            List<Map<String, Object>> fees = jdbc.queryForList(
                "SELECT paymentID, feeType, amount, paymentDate, status " +
                "FROM FeePayments WHERE studentID = ? " +
                "ORDER BY paymentDate DESC",
                sid
            );

            result.put("fees", fees);
            // Unresolved complaints, using the actual Complaints table schema.
            result.put("openComplaints", jdbc.queryForObject(
                "SELECT COUNT(*) FROM Complaints " +
                "WHERE student_id = ? AND status <> 'Resolved'",
                Integer.class, sid
            ));

            // Events scheduled from today through the end of this month.
            // The Events endpoint uses event_date as the date column.
            result.put("upcomingEvents", jdbc.queryForObject(
                "SELECT COUNT(*) FROM Events " +
                "WHERE event_date BETWEEN ? AND ?",
                Integer.class,
                LocalDate.now(),
                LocalDate.now().withDayOfMonth(
                    LocalDate.now().lengthOfMonth()
                )
            ));

            // Recent complaints
            List<Map<String, Object>> recentComplaints = complaints(sid, null, 3);
            result.put("recentComplaints", recentComplaints);
            result.put("complaints", recentComplaints);

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    // -------------------- ROOM --------------------

    @GetMapping("/room")
    public ResponseEntity<?> getRoom(HttpSession session) {
        try {
            String sid = getStudentID(session);
            Map<String, Object> currentRoom = room(sid);

            List<Map<String, Object>> roommates =
                currentRoom == null ? List.of() : jdbc.queryForList(
                    "SELECT s.studentID, s.name, s.email " +
                    "FROM RoomAllocations a " +
                    "JOIN Students s ON s.studentID = a.studentID " +
                    "WHERE a.roomID = ? AND a.studentID <> ?",
                    currentRoom.get("roomID"), sid
                );

            Map<String, Object> result = new LinkedHashMap<>();

            result.put(
                "room",
                currentRoom == null ? Map.of() : currentRoom
            );
            result.put("roommates", roommates);
            result.put("applications", applications(sid));

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    // -------------------- APPLICATIONS --------------------

    @GetMapping("/applications")
    public ResponseEntity<?> getApplications(HttpSession session) {
        try {
            return ResponseEntity.ok(
                applications(getStudentID(session))
            );
        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    @PostMapping("/applications")
    public ResponseEntity<?> createApplication(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        try {
            String sid = getStudentID(session);

            String preference = body.getOrDefault(
                "preferenceDescription", ""
            ).trim();

            if (preference.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "message",
                        "Preference description is required"
                    )
                );
            }

            String id = UUID.randomUUID()
                .toString().replace("-", "").substring(0, 10);

            LocalDate today = LocalDate.now();

            jdbc.update(
                "INSERT INTO Applications " +
                "(applicationID, studentID, status, appliedDate, " +
                "preferenceDescription) " +
                "VALUES (?, ?, 'Pending', ?, ?)",
                id, sid, today, preference
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                    "applicationID", id,
                    "appliedDate", today.toString(),
                    "preferenceDescription", preference,
                    "status", "Pending"
                )
            );

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    // -------------------- FEES --------------------

    @GetMapping("/fees")
    public ResponseEntity<?> getFees(HttpSession session) {
        try {
            String sid = getStudentID(session);

            List<Map<String, Object>> all = jdbc.queryForList(
                "SELECT paymentID, feeType, amount, paymentDate, status " +
                "FROM FeePayments WHERE studentID = ? " +
                "ORDER BY paymentDate DESC",
                sid
            );

            List<Map<String, Object>> paid = new ArrayList<>();
            List<Map<String, Object>> due = new ArrayList<>();

            BigDecimal totalDue = BigDecimal.ZERO;

            for (Map<String, Object> fee : all) {
                String status = String.valueOf(fee.get("status"));

                if ("Paid".equalsIgnoreCase(status)
                        || "Success".equalsIgnoreCase(status)) {

                    paid.add(fee);

                } else if ("Pending".equalsIgnoreCase(status)) {

                    due.add(fee);

                    Object amount = fee.get("amount");

                    if (amount != null) {
                        totalDue = totalDue.add(
                            new BigDecimal(String.valueOf(amount))
                        );
                    }
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("paid", paid);
            result.put("due", due);
            result.put("totalDue", totalDue);

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    // -------------------- PAY FEE --------------------

    
    // -------------------- PAY FEES --------------------

    
@PostMapping("/fees/pay")
@Transactional
public ResponseEntity<?> payFees(
        @RequestBody Map<String, List<String>> request,
        HttpSession session) {

    try {
        // Identify the currently logged-in student.
        String sid = getStudentID(session);

        List<String> paymentIDs = request.get("paymentIDs");

        if (paymentIDs == null || paymentIDs.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Select at least one fee.")
            );
        }

        // Remove duplicate IDs.
        paymentIDs = new ArrayList<>(
            new LinkedHashSet<>(paymentIDs)
        );

        // Validate all selected fees before updating any of them.
        for (String paymentID : paymentIDs) {

            if (paymentID == null || paymentID.isBlank()) {
                return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid payment ID.")
                );
            }

            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM FeePayments " +
                "WHERE paymentID = ? AND studentID = ? " +
                "AND status = 'Pending'",
                Integer.class,
                paymentID,
                sid
            );

            if (count == null || count != 1) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "message",
                        "A selected fee is invalid or is no longer pending. Refresh and try again."
                    )
                );
            }
        }

        // Mark each selected fee as paid.
        int paidCount = 0;

        for (String paymentID : paymentIDs) {

            int updated = jdbc.update(
                "UPDATE FeePayments " +
                "SET status = 'Paid', paymentDate = ? " +
                "WHERE paymentID = ? AND studentID = ? " +
                "AND status = 'Pending'",
                LocalDate.now(),
                paymentID,
                sid
            );

            if (updated != 1) {
                throw new IllegalStateException(
                    "Fee update failed. Please refresh and try again."
                );
            }

            paidCount++;
        }

        return ResponseEntity.ok(
            Map.of(
                "success", true,
                "message", "Selected fees marked as paid.",
                "paidCount", paidCount
            )
        );

    } catch (RuntimeException e) {
        return handleError(e);
    }
}


    // -------------------- COMPLAINTS --------------------

    @GetMapping("/complaints")
    public ResponseEntity<?> getComplaints(
            @RequestParam(required = false) String status,
            HttpSession session) {

        try {
            return ResponseEntity.ok(
                complaints(getStudentID(session), status, 200)
            );
        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    @PostMapping("/complaints")
    public ResponseEntity<?> createComplaint(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        try {
            String sid = getStudentID(session);

            String category = body.getOrDefault(
                "category", ""
            ).trim();

            String description = body.getOrDefault(
                "description", ""
            ).trim();

            if (category.isEmpty() || description.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "message",
                        "Category and description are required"
                    )
                );
            }

            Map<String, Object> currentRoom = room(sid);

            // Do not depend on a hostel identifier; the room lookup does not provide one.
            // The complaint can be created without assigning a warden here;
            // warden assignment can be handled separately using the schema's
            // actual room/warden relationship, if one exists.

            LocalDate today = LocalDate.now();

            // Use the actual table columns. complaint_id is left to the
            // database to generate, and no hostelID/wardenID is required.
            jdbc.update(
                "INSERT INTO Complaints " +
                "(student_id, complaint_type, description, " +
                "complaint_date, status) " +
                "VALUES (?, ?, ?, ?, 'Pending')",
                sid, category, description, today
            );

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("category", category);
            result.put("description", description);
            result.put("date", today.toString());
            result.put("status", "Pending");
            result.put(
                "room",
                currentRoom == null
                    ? "-" : currentRoom.get("roomID")
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(result);

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    // -------------------- LOST AND FOUND --------------------

    @GetMapping("/lostfound")
    public ResponseEntity<?> getLostFound(HttpSession session) {
        try {
            String sid = getStudentID(session);

            
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT lf.itemID, lf.studentID, lf.itemName, " +
                "lf.description, lf.`date`, lf.status, " +
                "s.name AS reporterName, r.roomID AS roomNumber " +
                "FROM LostFound lf " +
                "LEFT JOIN Students s ON lf.studentID = s.studentID " +
                "LEFT JOIN RoomAllocations ra ON lf.studentID = ra.studentID " +
                "LEFT JOIN Rooms r ON ra.roomID = r.roomID " +
                "ORDER BY lf.`date` DESC"
            );


            for (Map<String, Object> row : rows) {
                Object ownerID = row.remove("studentID");

                row.put(
                    "mine",
                    sid.equals(String.valueOf(ownerID))
                );
            }

            return ResponseEntity.ok(rows);

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    @PostMapping("/lostfound")
    public ResponseEntity<?> addLostFound(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        try {
            String sid = getStudentID(session);

            String name = body.getOrDefault(
                "itemName", ""
            ).trim();

            if (name.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("message", "Item name is required")
                );
            }

            String status = "Found".equalsIgnoreCase(
                body.get("status")
            ) ? "Found" : "Lost";

            String id = UUID.randomUUID()
                .toString().replace("-", "").substring(0, 10);

            String description = body.getOrDefault(
                "description", ""
            );

            LocalDate today = LocalDate.now();

            jdbc.update(
                "INSERT INTO LostFound " +
                "(itemID, studentID, itemName, description, " +
                "`date`, status) VALUES (?, ?, ?, ?, ?, ?)",
                id, sid, name, description, today, status
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                    "itemID", id,
                    "itemName", name,
                    "description", description,
                    "date", today.toString(),
                    "status", status,
                    "mine", true
                )
            );

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    @PutMapping("/lostfound/{itemID}/claim")
    public ResponseEntity<?> claimItem(
            @PathVariable String itemID,
            HttpSession session) {

        try {
            getStudentID(session);

            int updated = jdbc.update(
                "UPDATE LostFound SET status = 'Claimed' " +
                "WHERE itemID = ? AND status = 'Found'",
                itemID
            );

            if (updated == 0) {
                return ResponseEntity.status(404).body(
                    Map.of(
                        "message",
                        "Item not found or already claimed"
                    )
                );
            }

            return ResponseEntity.ok(
                Map.of("itemID", itemID, "status", "Claimed")
            );

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    // -------------------- EVENTS --------------------


@GetMapping("/events")
public ResponseEntity<?> getEvents(
        @RequestParam(required = false) String all,
        HttpSession session) {

    try {
        getStudentID(session);

        String sql =
            "SELECT event_id, event_name, description, " +
            "event_date, event_time, venue, organized_by, created_at " +
            "FROM Events ";

        if ("true".equalsIgnoreCase(all)) {
            sql += "ORDER BY event_date, event_time";
            return ResponseEntity.ok(jdbc.queryForList(sql));
        }

        sql += "WHERE event_date >= ? " +
               "ORDER BY event_date, event_time";

        return ResponseEntity.ok(
            jdbc.queryForList(sql, LocalDate.now())
        );

    } catch (RuntimeException e) {
        return handleError(e);
    }
}



    // -------------------- ROOMMATE PREFERENCES --------------------

    @GetMapping("/roommate/preference")
    public ResponseEntity<?> getPreference(HttpSession session) {
        try {
            String sid = getStudentID(session);
            Map<String, Object> currentRoom = room(sid);

            if (currentRoom == null) {
                return ResponseEntity.ok(
                    Map.of("preferenceDetails", "")
                );
            }

            List<String> values = jdbc.query(
                "SELECT preferenceDetails FROM RoommatePreferences " +
                "WHERE allocationID = ?",
                (rs, n) -> rs.getString("preferenceDetails"),
                currentRoom.get("allocationID")
            );

            String details = values.isEmpty()
                || values.get(0) == null
                    ? "" : values.get(0);

            return ResponseEntity.ok(
                Map.of("preferenceDetails", details)
            );

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    @PutMapping("/roommate/preference")
    public ResponseEntity<?> savePreference(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        try {
            String sid = getStudentID(session);

            String details = body.getOrDefault(
                "preferenceDetails", ""
            ).trim();

            if (details.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "message",
                        "Preference details are required"
                    )
                );
            }

            Map<String, Object> currentRoom = room(sid);

            if (currentRoom == null) {
                return ResponseEntity.status(409).body(
                    Map.of(
                        "message",
                        "A room allocation is required first"
                    )
                );
            }

            Object allocationID = currentRoom.get("allocationID");

            int updated = jdbc.update(
                "UPDATE RoommatePreferences " +
                "SET preferenceDetails = ? WHERE allocationID = ?",
                details, allocationID
            );

            if (updated == 0) {
                String id = UUID.randomUUID()
                    .toString().replace("-", "").substring(0, 10);

                jdbc.update(
                    "INSERT INTO RoommatePreferences " +
                    "(preferenceID, allocationID, preferenceDetails) " +
                    "VALUES (?, ?, ?)",
                    id, allocationID, details
                );
            }

            return ResponseEntity.ok(
                Map.of("message", "Preferences saved")
            );

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    @GetMapping("/roommate/matches")
    public ResponseEntity<?> getMatches(HttpSession session) {
        try {
            String sid = getStudentID(session);
            Map<String, Object> currentRoom = room(sid);

            if (currentRoom == null) {
                return ResponseEntity.ok(List.of());
            }

            List<String> mine = jdbc.query(
                "SELECT preferenceDetails FROM RoommatePreferences " +
                "WHERE allocationID = ?",
                (rs, n) -> rs.getString("preferenceDetails"),
                currentRoom.get("allocationID")
            );

            if (mine.isEmpty() || mine.get(0) == null
                    || mine.get(0).isBlank()) {
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, Object>> matches = jdbc.queryForList(
                "SELECT s.studentID, s.name, p.preferenceDetails " +
                "FROM RoommatePreferences p " +
                "JOIN RoomAllocations a " +
                "ON a.allocationID = p.allocationID " +
                "JOIN Rooms r ON r.roomID = a.roomID " +
                "JOIN Students s ON s.studentID = a.studentID " +
                "WHERE a.roomID = ? AND s.studentID <> ?",
                currentRoom.get("roomID"), sid
            );

            Set<String> myWords = words(mine.get(0));

            for (Map<String, Object> match : matches) {
                match.put(
                    "matchScore",
                    score(
                        myWords,
                        words(String.valueOf(
                            match.get("preferenceDetails")
                        ))
                    )
                );
            }

            matches.sort((a, b) -> Integer.compare(
                ((Number) b.get("matchScore")).intValue(),
                ((Number) a.get("matchScore")).intValue()
            ));

            return ResponseEntity.ok(matches);

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }
    
    // -------------------- MESS: MEAL TIMINGS --------------------

    @GetMapping("/mess/timings")
    public ResponseEntity<?> getMealTimings(HttpSession session) {
        try {
            getStudentID(session);

            return ResponseEntity.ok(jdbc.queryForList(
                "SELECT timing_id, meal_type, start_time, end_time, updated_at " +
                "FROM MealTimings ORDER BY FIELD(" +
                "meal_type, 'BREAKFAST', 'LUNCH', 'SNACKS', 'DINNER')"
            ));

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    // -------------------- MESS: FOOD PREFERENCE --------------------

    @GetMapping("/mess/preference")
    public ResponseEntity<?> getMessPreference(HttpSession session) {
        try {
            String sid = getStudentID(session);

            List<String> preferences = jdbc.query(
                "SELECT food_preference FROM StudentFoodPreferences " +
                "WHERE student_id = ?",
                (rs, rowNum) -> rs.getString("food_preference"),
                sid
            );

            String preference = preferences.isEmpty()
                ? "VEG" : preferences.get(0);

            return ResponseEntity.ok(
                Map.of("foodPreference", preference)
            );

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    @PutMapping("/mess/preference")
    public ResponseEntity<?> saveMessPreference(
            @RequestBody Map<String, String> body,
            HttpSession session) {
        try {
            String sid = getStudentID(session);

            String preference = body.getOrDefault(
                "foodPreference", ""
            ).trim().toUpperCase(Locale.ROOT);

            if (!"VEG".equals(preference)
                    && !"NON_VEG".equals(preference)) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "message",
                        "Food preference must be VEG or NON_VEG"
                    )
                );
            }

            jdbc.update(
                "INSERT INTO StudentFoodPreferences " +
                "(student_id, food_preference) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "food_preference = ?, updated_at = CURRENT_TIMESTAMP",
                sid, preference, preference
            );

            return ResponseEntity.ok(
                Map.of(
                    "message", "Food preference saved",
                    "foodPreference", preference
                )
            );

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    // -------------------- MESS: MENU --------------------

    @GetMapping("/mess/menu")
    public ResponseEntity<?> getMessMenu(
            @RequestParam(required = false) String date,
            HttpSession session) {
        try {
            String sid = getStudentID(session);

            LocalDate menuDate;

            try {
                menuDate = (date == null || date.isBlank())
                    ? LocalDate.now()
                    : LocalDate.parse(date);
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "message",
                        "Invalid date. Use YYYY-MM-DD format"
                    )
                );
            }

            List<String> preferences = jdbc.query(
                "SELECT food_preference FROM StudentFoodPreferences " +
                "WHERE student_id = ?",
                (rs, rowNum) -> rs.getString("food_preference"),
                sid
            );

            String preference = preferences.isEmpty()
                ? "VEG" : preferences.get(0);

            List<Map<String, Object>> menu;

            if ("NON_VEG".equalsIgnoreCase(preference)) {
                // NON_VEG students can see both VEG and NON_VEG items.
                menu = jdbc.queryForList(
                    "SELECT menu_id, meal_type, item_name, food_category, " +
                    "menu_date, available " +
                    "FROM MessMenu " +
                    "WHERE menu_date = ? AND available = TRUE " +
                    "ORDER BY FIELD(meal_type, 'BREAKFAST', 'LUNCH', " +
                    "'SNACKS', 'DINNER'), item_name",
                    menuDate
                );
            } else {
                // VEG students can see only vegetarian items.
                menu = jdbc.queryForList(
                    "SELECT menu_id, meal_type, item_name, food_category, " +
                    "menu_date, available " +
                    "FROM MessMenu " +
                    "WHERE menu_date = ? AND available = TRUE " +
                    "AND food_category = 'VEG' " +
                    "ORDER BY FIELD(meal_type, 'BREAKFAST', 'LUNCH', " +
                    "'SNACKS', 'DINNER'), item_name",
                    menuDate
                );
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("date", menuDate.toString());
            result.put("foodPreference", preference);
            result.put("menu", menu);

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return handleError(e);
        }
    }

    // -------------------- MATCHING HELPERS --------------------
    
    private Set<String> words(String text) {
        Set<String> result = new HashSet<>();

        for (String word :
                text.toLowerCase().split("[^a-z0-9]+")) {
            if (word.length() > 2) {
                result.add(word);
            }
        }

        return result;
    }

    private int score(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }

        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);

        Set<String> union = new HashSet<>(a);
        union.addAll(b);

        return (int) Math.round(
            100.0 * intersection.size() / union.size()
        );
    }

    // -------------------- ERROR HANDLING --------------------

    private ResponseEntity<?> handleError(RuntimeException e) {
        if ("UNAUTHORIZED".equals(e.getMessage())) {
            return ResponseEntity.status(401).body(
                Map.of(
                    "message",
                    "Please log in with a student account"
                )
            );
        }

        if ("STUDENT_RECORD_NOT_FOUND".equals(e.getMessage())) {
            return ResponseEntity.status(404).body(
                Map.of(
                    "message",
                    "No student record is linked to this account"
                )
            );
        }

        throw e;
    }
}
