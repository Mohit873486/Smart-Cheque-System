package com.chequeprint.service;

import com.chequeprint.dao.AuditLogDAO;
import com.chequeprint.model.AuditAction;
import com.chequeprint.model.AuditLog;
import com.chequeprint.model.User;

import java.sql.SQLException;
import java.util.List;

public class AuditService {

  private final AuditLogDAO dao = new AuditLogDAO();

  public void record(User user, String tableName, Integer recordId, AuditAction action, String details)
      throws SQLException {
    AuditLog log = new AuditLog(user == null ? null : user.getId(), tableName, recordId, action, details);
    dao.insert(log);
  }

  public void recordLogin(User user) throws SQLException {
    record(user, "users", user.getId(), AuditAction.LOGIN, "User logged in.");
  }

  public void recordLogout(User user) throws SQLException {
    record(user, "users", user.getId(), AuditAction.LOGOUT, "User logged out.");
  }

  public List<AuditLog> findRecent(int limit) throws SQLException {
    return dao.findRecent(limit);
  }
}
