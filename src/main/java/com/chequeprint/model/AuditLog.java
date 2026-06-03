package com.chequeprint.model;

import java.time.LocalDateTime;

public class AuditLog {
  private long id;
  private Integer userId;
  private String tableName;
  private Integer recordId;
  private AuditAction action;
  private String details;
  private LocalDateTime createdAt;

  public AuditLog() {
  }

  public AuditLog(Integer userId, String tableName, Integer recordId, AuditAction action, String details) {
    this.userId = userId;
    this.tableName = tableName;
    this.recordId = recordId;
    this.action = action;
    this.details = details;
    this.createdAt = LocalDateTime.now();
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public Integer getRecordId() {
    return recordId;
  }

  public void setRecordId(Integer recordId) {
    this.recordId = recordId;
  }

  public AuditAction getAction() {
    return action;
  }

  public void setAction(AuditAction action) {
    this.action = action;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
