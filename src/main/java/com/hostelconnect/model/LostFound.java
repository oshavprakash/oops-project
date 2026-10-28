package com.hostelconnect.model;

import java.io.Serializable;
import java.sql.Date;

public class LostFound implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemID;
    private String studentID;
    private String itemName;
    private String description;
    private Date date;
    private String status; // 'Lost', 'found', 'claimed'

    public LostFound() {}

    public LostFound(String itemID, String studentID, String itemName, String description, Date date, String status) {
        this.itemID = itemID;
        this.studentID = studentID;
        this.itemName = itemName;
        this.description = description;
        this.date = date;
        this.status = status;
    }

    public String getItemID() {
        return itemID;
    }

    public void setItemID(String itemID) {
        this.itemID = itemID;
    }

    public String getStudentID() {
        return studentID;
    }

    public void setStudentID(String studentID) {
        this.studentID = studentID;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
