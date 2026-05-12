package com.chequeprint.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Invoice {

    public enum Status { Unpaid, Paid, Partial, Cancelled }

    private int id;
    private String invoiceNo;
    private String clientName;
    private BigDecimal amount;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private Status status;
    private String notes;

    public Invoice() {}

    public Invoice(String invoiceNo, String clientName, BigDecimal amount,
                   LocalDate issueDate, LocalDate dueDate) {
        this.invoiceNo  = invoiceNo;
        this.clientName = clientName;
        this.amount     = amount;
        this.issueDate  = issueDate;
        this.dueDate    = dueDate;
        this.status     = Status.Unpaid;
    }

    public int getId()                           { return id; }
    public void setId(int id)                    { this.id = id; }
    public String getInvoiceNo()                 { return invoiceNo; }
    public void setInvoiceNo(String n)           { this.invoiceNo = n; }
    public String getClientName()                { return clientName; }
    public void setClientName(String n)          { this.clientName = n; }
    public BigDecimal getAmount()                { return amount; }
    public void setAmount(BigDecimal a)          { this.amount = a; }
    public LocalDate getIssueDate()              { return issueDate; }
    public void setIssueDate(LocalDate d)        { this.issueDate = d; }
    public LocalDate getDueDate()                { return dueDate; }
    public void setDueDate(LocalDate d)          { this.dueDate = d; }
    public Status getStatus()                    { return status; }
    public void setStatus(Status s)              { this.status = s; }
    public String getNotes()                     { return notes; }
    public void setNotes(String n)               { this.notes = n; }
}