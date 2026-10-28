package com.hostelconnect.dao;

import com.hostelconnect.model.User;
import com.hostelconnect.util.DBConnection;
import com.hostelconnect.util.PasswordUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Random;

public class UserDAO {

    /**
     * Authenticate user with either UserID or Email.
     * Checks password using PBKDF2/SHA-256 hash.
     */
    public User authenticate(String emailOrId, String plainPassword) throws SQLException {
        String sql = "SELECT userID, name, email, password, role FROM users WHERE userID = ? OR email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, emailOrId);
            ps.setString(2, emailOrId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("password");
                    if (PasswordUtil.checkPassword(plainPassword, hashedPassword)) {
                        String dbRole = rs.getString("role");
                        // Normalize role display for app ("Student", "Warden", "Administrator")
                        String role = "Student";
                        if ("warden".equalsIgnoreCase(dbRole)) role = "Warden";
                        else if ("administrator".equalsIgnoreCase(dbRole)) role = "Administrator";

                        return new User(
                            rs.getString("userID"),
                            rs.getString("name"),
                            rs.getString("email"),
                            hashedPassword,
                            role
                        );
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks the application status of a student in Applications table.
     */
    public String getStudentApplicationStatus(String studentID) throws SQLException {
        String sql = "SELECT status FROM Applications WHERE studentID = ? ORDER BY appliedDate DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        }
        return null;
    }

    /**
     * Checks if email is already registered.
     */
    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Checks if userID is already taken.
     */
    public boolean existsById(String userId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE userID = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Registers a student using a database transaction across users, students, and Applications.
     * Application status is set to 'Pending'.
     */
    public boolean registerStudent(String studentId, String name, String email, String plainPassword, String preference) throws SQLException {
        String insertUserSql = "INSERT INTO users (userID, name, email, password, role) VALUES (?, ?, ?, ?, 'student')";
        String insertStudentSql = "INSERT INTO students (studentID, name, email) VALUES (?, ?, ?)";
        String insertAppSql = "INSERT INTO Applications (applicationID, studentID, status, appliedDate, preferenceDescription) VALUES (?, ?, 'Pending', ?, ?)";

        String hashedPassword = PasswordUtil.hashPassword(plainPassword);
        String applicationId = generateId("APP", 7);

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Insert into users
            try (PreparedStatement psUser = conn.prepareStatement(insertUserSql)) {
                psUser.setString(1, studentId);
                psUser.setString(2, name);
                psUser.setString(3, email);
                psUser.setString(4, hashedPassword);
                psUser.executeUpdate();
            }

            // 2. Insert into students
            try (PreparedStatement psStudent = conn.prepareStatement(insertStudentSql)) {
                psStudent.setString(1, studentId);
                psStudent.setString(2, name);
                psStudent.setString(3, email);
                psStudent.executeUpdate();
            }

            // 3. Insert into Applications (Status: Pending)
            try (PreparedStatement psApp = conn.prepareStatement(insertAppSql)) {
                psApp.setString(1, applicationId);
                psApp.setString(2, studentId);
                psApp.setDate(3, Date.valueOf(LocalDate.now()));
                psApp.setString(4, preference != null && !preference.isEmpty() ? preference : "Standard Room");
                psApp.executeUpdate();
            }

            conn.commit(); // Commit transaction
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Provisions the designated initial Warden and Administrator accounts if they do not exist.
     */
    public void provisionDefaultAdminAndWarden(
            String adminId, String adminName, String adminEmail, String adminPassword,
            String wardenId, String wardenName, String wardenEmail, String wardenPassword, String hostelName
    ) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // Check & seed Administrator
            if (!existsById(adminId) && !existsByEmail(adminEmail)) {
                String hashedAdmin = PasswordUtil.hashPassword(adminPassword);
                String sqlUser = "INSERT INTO users (userID, name, email, password, role) VALUES (?, ?, ?, ?, 'administrator')";
                try (PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                    ps.setString(1, adminId);
                    ps.setString(2, adminName);
                    ps.setString(3, adminEmail);
                    ps.setString(4, hashedAdmin);
                    ps.executeUpdate();
                }
                String sqlAdmin = "INSERT INTO admins (adminID) VALUES (?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlAdmin)) {
                    ps.setString(1, adminId);
                    ps.executeUpdate();
                }
            }

            // Check & seed Warden
            if (!existsById(wardenId) && !existsByEmail(wardenEmail)) {
                String hashedWarden = PasswordUtil.hashPassword(wardenPassword);
                String sqlUser = "INSERT INTO users (userID, name, email, password, role) VALUES (?, ?, ?, ?, 'warden')";
                try (PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                    ps.setString(1, wardenId);
                    ps.setString(2, wardenName);
                    ps.setString(3, wardenEmail);
                    ps.setString(4, hashedWarden);
                    ps.executeUpdate();
                }
                String sqlWarden = "INSERT INTO Wardens (wardenID, hostelName) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlWarden)) {
                    ps.setString(1, wardenId);
                    ps.setString(2, hostelName);
                    ps.executeUpdate();
                }
            }
        }
    }

    /**
     * Generates a unique short ID within 10 characters.
     */
    public static String generateId(String prefix, int randomDigits) {
        Random rand = new Random();
        int max = (int) Math.pow(10, randomDigits);
        int num = rand.nextInt(max);
        String id = prefix + String.format("%0" + randomDigits + "d", num);
        if (id.length() > 10) {
            id = id.substring(0, 10);
        }
        return id;
    }
}
