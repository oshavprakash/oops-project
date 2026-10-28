package com.hostelconnect.model;

import java.io.Serializable;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private String roomID;
    private String hostelID;
    private String roomType;
    private int capacity;
    private int occupancy;
    private String status; // 'Available', 'full', 'under maintenance'

    public Room() {}

    public Room(String roomID, String hostelID, String roomType, int capacity, int occupancy, String status) {
        this.roomID = roomID;
        this.hostelID = hostelID;
        this.roomType = roomType;
        this.capacity = capacity;
        this.occupancy = occupancy;
        this.status = status;
    }

    public String getRoomID() {
        return roomID;
    }

    public void setRoomID(String roomID) {
        this.roomID = roomID;
    }

    public String getHostelID() {
        return hostelID;
    }

    public void setHostelID(String hostelID) {
        this.hostelID = hostelID;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(int occupancy) {
        this.occupancy = occupancy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
