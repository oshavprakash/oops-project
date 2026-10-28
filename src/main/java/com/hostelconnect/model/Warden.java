package com.hostelconnect.model;

import java.io.Serializable;

public class Warden implements Serializable {
    private static final long serialVersionUID = 1L;

    private String wardenID;
    private String hostelName;

    public Warden() {}

    public Warden(String wardenID, String hostelName) {
        this.wardenID = wardenID;
        this.hostelName = hostelName;
    }

    public String getWardenID() {
        return wardenID;
    }

    public void setWardenID(String wardenID) {
        this.wardenID = wardenID;
    }

    public String getHostelName() {
        return hostelName;
    }

    public void setHostelName(String hostelName) {
        this.hostelName = hostelName;
    }
}
