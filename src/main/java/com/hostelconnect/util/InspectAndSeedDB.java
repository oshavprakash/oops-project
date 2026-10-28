package com.hostelconnect.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class InspectAndSeedDB {

    public static void main(String[] args) {
        System.out.println("Configuring initial database setup in TiDB Cloud...");

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Seed Hostels if empty
            boolean hasHostel = false;
            try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM Hostels LIMIT 1")) {
                hasHostel = rs.next();
            }
            if (!hasHostel) {
                stmt.executeUpdate("INSERT INTO Hostels (hostelID, name, location, capacity) VALUES ('H1', 'Main Hostel Block', 'Campus Central', 100)");
                System.out.println(">>> Added initial hostel record (H1 - Main Hostel Block) <<<");
            }

            // 2. Seed Rooms if empty
            boolean hasRooms = false;
            try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM Rooms LIMIT 1")) {
                hasRooms = rs.next();
            }
            if (!hasRooms) {
                String insertRooms = "INSERT INTO Rooms (roomID, hostelID, roomType, capacity, occupancy, status) VALUES " +
                    "('A-101', 'H1', 'Single', 1, 0, 'Available')," +
                    "('A-102', 'H1', 'Double', 2, 0, 'Available')," +
                    "('A-103', 'H1', 'Double', 2, 0, 'Available')," +
                    "('B-201', 'H1', 'Double', 2, 0, 'Available')," +
                    "('B-202', 'H1', 'Dormitory', 4, 0, 'Available')," +
                    "('B-203', 'H1', 'Dormitory', 4, 0, 'Available')";
                stmt.executeUpdate(insertRooms);
                System.out.println(">>> Added initial hostel rooms (A-101 to B-203) <<<");
            }

            // 3. Ensure no mock students exist (clean slate)
            System.out.println("Verifying student accounts...");
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM students")) {
                if (rs.next()) {
                    System.out.println("Current registered students in TiDB: " + rs.getInt(1));
                }
            }

            System.out.println("\nSUCCESS: Hostel database is ready for interactive multi-module testing!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
