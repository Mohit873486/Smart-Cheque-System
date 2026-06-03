package com.chequeprint.dao;

import com.chequeprint.config.AppConfig;
import com.chequeprint.model.AuditAction;
import com.chequeprint.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

  private static final String INSERT = "INSERT INTO audit_log (user_id, table_name, record_id, action, details, created_at) VALUES (?, ?, ?, ?, ?, ?)";
  private static final String SELECT_RECENT = "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?";

  public boolean insert(AuditLog log) throws SQLException {
    try (Connection connection = AppConfig.getConnection();
        PreparedStatement ps = connection.prepareStatement(INSERT, PreparedStatement.RETURN_GENERATED_KEYS)) {
      ps.setObject(1, log.getUserId());
      ps.setString(2, log.getTableName());
      ps.setObject(3, log.getRecordId());
      ps.setString(4, log.getAction().name());
      ps.setString(5, log.getDetails());
      ps.setTimestamp(6, Timestamp.valueOf(log.getCreatedAt()));
      boolean inserted = ps.executeUpdate() > 0;
      if (inserted) {
        try (ResultSet keys = ps.getGeneratedKeys()) {
          if (keys.next()) {
            log.setId(keys.getLong(1));
          }
        }
      }
      return inserted;
    }
  }

  public List<AuditLog> findRecent(int limit) throws SQLException {
    List<AuditLog> logs = new ArrayList<>();
    try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(SELECT_RECENT)) {
      ps.setInt(1, limit);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          logs.add(mapRow(rs));
        }
      }
    }
    return logs;
  }

  private AuditLog mapRow(ResultSet rs) throws SQLException {
    AuditLog log = new AuditLog();
    log.setId(rs.getLong("id"));
    int userId = rs.getInt("user_id");
    if (!rs.wasNull()) {
      log.setUserId(userId);
    }
    log.setTableName(rs.getString("table_name"));
    int recordId = rs.getInt("record_id");
    if (!rs.wasNull()) {
      log.setRecordId(recordId);
    }
    log.setAction(AuditAction.valueOf(rs.getString("action")));
    log.setDetails(rs.getString("details"));
    Timestamp createdAt = rs.getTimestamp("created_at");
    log.setCreatedAt(createdAt.toLocalDateTime());
    return log;
  }
}
