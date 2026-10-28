package com.hostelconnect.model;

import java.io.Serializable;

public class RoommatePreference implements Serializable {
    private static final long serialVersionUID = 1L;

    private String preferenceID;
    private String allocationID;
    private String preferenceDetails;

    public RoommatePreference() {}

    public RoommatePreference(String preferenceID, String allocationID, String preferenceDetails) {
        this.preferenceID = preferenceID;
        this.allocationID = allocationID;
        this.preferenceDetails = preferenceDetails;
    }

    public String getPreferenceID() {
        return preferenceID;
    }

    public void setPreferenceID(String preferenceID) {
        this.preferenceID = preferenceID;
    }

    public String getAllocationID() {
        return allocationID;
    }

    public void setAllocationID(String allocationID) {
        this.allocationID = allocationID;
    }

    public String getPreferenceDetails() {
        return preferenceDetails;
    }

    public void setPreferenceDetails(String preferenceDetails) {
        this.preferenceDetails = preferenceDetails;
    }
}
