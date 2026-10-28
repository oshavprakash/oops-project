package com.hostelconnect.model;

import java.io.Serializable;
import java.sql.Date;

public class Complaint implements Serializable {
    private static final long serialVersionUID = 1L;

    private String complaintID;
    private String studentID;
    private String wardenID;
    private String category;
    private String description;
    private Date date;
    private String status; // 'Open', 'in progress', 'resolved'

    public Complaint() {}

    public Complaint(String complaintID, String studentID, String wardenID, String category, String description, Date date, String status) {
        this.complaintID = complaintID;
        this.studentID = studentID;
        this.wardenID = wardenID;
        this.category = category;
        this.description = description;
        this.date = date;
        this.status = status;
    }

    public String getComplaintID() {
        return complaintID;
    }

    public void setComplaintID(String complaintID) {
        this.complaintID = complaintID;
    }

    public String getStudentID() {
        return studentID;
    }

    public void setStudentID(String studentID) {
        this.studentID = studentID;
    }

    public String getWardenID() {
        return wardenID;
    }

    public void setWardenID(String wardenID) {
        this.wardenID = wardenID;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
