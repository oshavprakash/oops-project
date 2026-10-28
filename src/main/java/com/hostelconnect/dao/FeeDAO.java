package com.hostelconnect.dao;

import com.hostelconnect.model.FeePayment;
import com.hostelconnect.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeeDAO {

    /**
     * Get fee payments for a specific student.
     */
    public List<FeePayment> getFeesByStudent(String studentId) throws SQLException {
        String sql = "SELECT paymentID, studentID, feeType, amount, paymentDate, status FROM FeePayments WHERE studentID = ? ORDER BY paymentDate DESC";
        List<FeePayment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new FeePayment(
                        rs.getString("paymentID"),
                        rs.getString("studentID"),
                        rs.getString("feeType"),
                        rs.getBigDecimal("amount"),
                        rs.getDate("paymentDate"),
                        rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Get all fee records (for Admin monitor) with student details.
     */
    public List<Map<String, Object>> getAllFees() throws SQLException {
        String sql = "SELECT fp.paymentID, fp.studentID, s.name as studentName, fp.feeType, fp.amount, fp.paymentDate, fp.status " +
                     "FROM FeePayments fp " +
                     "LEFT JOIN students s ON fp.studentID = s.studentID " +
                     "ORDER BY fp.paymentDate DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("paymentID", rs.getString("paymentID"));
                map.put("studentID", rs.getString("studentID"));
                map.put("studentName", rs.getString("studentName"));
                map.put("feeType", rs.getString("feeType"));
                map.put("amount", rs.getBigDecimal("amount"));
                map.put("paymentDate", rs.getDate("paymentDate") != null ? rs.getDate("paymentDate").toString() : "");
                map.put("status", rs.getString("status"));
                list.add(map);
            }
        }
        return list;
    }

    /**
     * Record a fee payment.
     */
    public boolean recordPayment(String studentId, String feeType, BigDecimal amount, String status) throws SQLException {
        String sql = "INSERT INTO FeePayments (paymentID, studentID, feeType, amount, paymentDate, status) VALUES (?, ?, ?, ?, ?, ?)";
        String paymentId = UserDAO.generateId("PAY", 7);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentId);
            ps.setString(2, studentId);
            ps.setString(3, feeType);
            ps.setBigDecimal(4, amount);
            ps.setDate(5, Date.valueOf(LocalDate.now()));
            ps.setString(6, status != null ? status : "Success");
            return ps.executeUpdate() > 0;
        }
    }
}
