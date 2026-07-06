package com.chequeprint.backend.service;

import com.chequeprint.backend.entity.AuditLog;
import com.chequeprint.backend.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog record(Integer userId, String tableName, int recordId, String action, String details) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setTableName(tableName);
        log.setRecordId(recordId);
        log.setAction(action.toUpperCase());
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getHistory(String tableName, int recordId) {
        return auditLogRepository.findByTableNameAndRecordIdOrderByCreatedAtDesc(tableName, recordId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecent(int limit) {
        return auditLogRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"))).getContent();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getHistoryByUserId(Integer userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
