package com.hostelconnect.dao;

import com.hostelconnect.model.Inventory;
import com.hostelconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAO {

    /**
     * Get all inventory items.
     */
    public List<Inventory> getAllInventory() throws SQLException {
        String sql = "SELECT itemID, wardenID, itemName, category, quantity, status FROM Inventory ORDER BY itemName ASC";
        List<Inventory> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Inventory(
                    rs.getString("itemID"),
                    rs.getString("wardenID"),
                    rs.getString("itemName"),
                    rs.getString("category"),
                    rs.getInt("quantity"),
                    rs.getString("status")
                ));
            }
        }
        return list;
    }

    /**
     * Add new inventory item.
     */
    public boolean addInventory(String wardenId, String itemName, String category, int quantity, String status) throws SQLException {
        String sql = "INSERT INTO Inventory (itemID, wardenID, itemName, category, quantity, status) VALUES (?, ?, ?, ?, ?, ?)";
        String itemId = UserDAO.generateId("INV", 7);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemId);
            ps.setString(2, wardenId);
            ps.setString(3, itemName);
            ps.setString(4, category);
            ps.setInt(5, quantity);
            ps.setString(6, status != null ? status : "Working");
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Update inventory status and quantity.
     */
    public boolean updateInventory(String itemId, int quantity, String status) throws SQLException {
        String sql = "UPDATE Inventory SET quantity = ?, status = ? WHERE itemID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setString(2, status);
            ps.setString(3, itemId);
            return ps.executeUpdate() > 0;
        }
    }
}
