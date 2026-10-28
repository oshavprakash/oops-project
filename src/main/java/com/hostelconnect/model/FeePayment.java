package com.hostelconnect.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;

public class FeePayment implements Serializable {
    private static final long serialVersionUID = 1L;

    private String paymentID;
    private String studentID;
    private String feeType;
    private BigDecimal amount;
    private Date paymentDate;
    private String status; // 'Success', 'failed', 'pending'

    public FeePayment() {}

    public FeePayment(String paymentID, String studentID, String feeType, BigDecimal amount, Date paymentDate, String status) {
        this.paymentID = paymentID;
        this.studentID = studentID;
        this.feeType = feeType;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.status = status;
    }

    public String getPaymentID() {
        return paymentID;
    }

    public void setPaymentID(String paymentID) {
        this.paymentID = paymentID;
    }

    public String getStudentID() {
        return studentID;
    }

    public void setStudentID(String studentID) {
        this.studentID = studentID;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
