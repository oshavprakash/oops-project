package com.hostelconnect.model;

import java.io.Serializable;
import java.sql.Date;

public class RoomAllocation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String allocationID;
    private String studentID;
    private String roomID;
    private Date allocationDate;

    public RoomAllocation() {}

    public RoomAllocation(String allocationID, String studentID, String roomID, Date allocationDate) {
        this.allocationID = allocationID;
        this.studentID = studentID;
        this.roomID = roomID;
        this.allocationDate = allocationDate;
    }

    public String getAllocationID() {
        return allocationID;
    }

    public void setAllocationID(String allocationID) {
        this.allocationID = allocationID;
    }

    public String getStudentID() {
        return studentID;
    }

    public void setStudentID(String studentID) {
        this.studentID = studentID;
    }

    public String getRoomID() {
        return roomID;
    }

    public void setRoomID(String roomID) {
        this.roomID = roomID;
    }

    public Date getAllocationDate() {
        return allocationDate;
    }

    public void setAllocationDate(Date allocationDate) {
        this.allocationDate = allocationDate;
    }
}
