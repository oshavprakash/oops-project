package com.hostelconnect.model;

import java.io.Serializable;

public class Administrator implements Serializable {
    private static final long serialVersionUID = 1L;

    private String adminID;

    public Administrator() {}

    public Administrator(String adminID) {
        this.adminID = adminID;
    }

    public String getAdminID() {
        return adminID;
    }

    public void setAdminID(String adminID) {
        this.adminID = adminID;
    }
}
