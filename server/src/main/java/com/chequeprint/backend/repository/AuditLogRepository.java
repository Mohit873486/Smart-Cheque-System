package com.chequeprint.backend.repository;

import com.chequeprint.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByTableNameAndRecordIdOrderByCreatedAtDesc(String tableName, int recordId);
}
