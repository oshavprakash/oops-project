package com.hostelconnect.model;

import java.io.Serializable;

public class Hostel implements Serializable {
    private static final long serialVersionUID = 1L;

    private String hostelID;
    private String name;
    private String location;
    private int capacity;

    public Hostel() {}

    public Hostel(String hostelID, String name, String location, int capacity) {
        this.hostelID = hostelID;
        this.name = name;
        this.location = location;
        this.capacity = capacity;
    }

    public String getHostelID() {
        return hostelID;
    }

    public void setHostelID(String hostelID) {
        this.hostelID = hostelID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
