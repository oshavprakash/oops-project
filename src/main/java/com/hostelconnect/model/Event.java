package com.hostelconnect.model;

import java.io.Serializable;
import java.sql.Date;

public class Event implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventID;
    private String adminID;
    private String title;
    private Date date;
    private String description;
    private String type; // e.g. cultural, academic

    public Event() {}

    public Event(String eventID, String adminID, String title, Date date, String description, String type) {
        this.eventID = eventID;
        this.adminID = adminID;
        this.title = title;
        this.date = date;
        this.description = description;
        this.type = type;
    }

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public String getAdminID() {
        return adminID;
    }

    public void setAdminID(String adminID) {
        this.adminID = adminID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
