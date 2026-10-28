package com.hostelconnect.model;

import java.io.Serializable;

public class Inventory implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemID;
    private String wardenID;
    private String itemName;
    private String category;
    private int quantity;
    private String status; // 'Working', 'needs repair', 'retired'

    public Inventory() {}

    public Inventory(String itemID, String wardenID, String itemName, String category, int quantity, String status) {
        this.itemID = itemID;
        this.wardenID = wardenID;
        this.itemName = itemName;
        this.category = category;
        this.quantity = quantity;
        this.status = status;
    }

    public String getItemID() {
        return itemID;
    }

    public void setItemID(String itemID) {
        this.itemID = itemID;
    }

    public String getWardenID() {
        return wardenID;
    }

    public void setWardenID(String wardenID) {
        this.wardenID = wardenID;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
