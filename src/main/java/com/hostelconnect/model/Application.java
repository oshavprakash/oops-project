package com.hostelconnect.model;

import java.io.Serializable;
import java.sql.Date;

public class Application implements Serializable {
    private static final long serialVersionUID = 1L;

    private String applicationID;
    private String studentID;
    private String status; // 'Pending', 'approved', 'rejected'
    private Date appliedDate;
    private String preferenceDescription;

    public Application() {}

    public Application(String applicationID, String studentID, String status, Date appliedDate, String preferenceDescription) {
        this.applicationID = applicationID;
        this.studentID = studentID;
        this.status = status;
        this.appliedDate = appliedDate;
        this.preferenceDescription = preferenceDescription;
    }

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public String getStudentID() {
        return studentID;
    }

    public void setStudentID(String studentID) {
        this.studentID = studentID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(Date appliedDate) {
        this.appliedDate = appliedDate;
    }

    public String getPreferenceDescription() {
        return preferenceDescription;
    }

    public void setPreferenceDescription(String preferenceDescription) {
        this.preferenceDescription = preferenceDescription;
    }
}
