package com.chequeprint.backend.repository;

import com.chequeprint.backend.entity.Cheque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface ChequeRepository extends JpaRepository<Cheque, Integer> {

    long countByStatus(Cheque.Status status);
    long countByIssueDate(LocalDate issueDate);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Cheque c WHERE c.issueDate >= :startOfMonth")
    BigDecimal sumThisMonth(@Param("startOfMonth") LocalDate startOfMonth);

    @Query("SELECT c.issueDate, COUNT(c) FROM Cheque c WHERE c.issueDate BETWEEN :start AND :end GROUP BY c.issueDate")
    List<Object[]> countByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = """
        SELECT
            COUNT(*) as totalCheques,
            SUM(CASE WHEN c.status = 'Pending' THEN 1 ELSE 0 END) as pendingCount,
            SUM(CASE WHEN c.status = 'Approved' THEN 1 ELSE 0 END) as approvedCount,
            SUM(CASE WHEN c.status = 'Printed' THEN 1 ELSE 0 END) as printedCount,
            SUM(CASE WHEN c.status = 'Rejected' THEN 1 ELSE 0 END) as rejectedCount,
            SUM(CASE WHEN c.status = 'Draft' THEN 1 ELSE 0 END) as draftCount,
            SUM(CASE WHEN c.status = 'Cancelled' THEN 1 ELSE 0 END) as cancelledCount,
            SUM(CASE WHEN c.status = 'Bounced' THEN 1 ELSE 0 END) as bouncedCount,
            SUM(CASE WHEN c.status = 'Cleared' THEN 1 ELSE 0 END) as clearedCount,
            SUM(CASE WHEN c.status = 'Deposited' THEN 1 ELSE 0 END) as depositedCount,
            COALESCE(SUM(c.amount), 0) as totalAmountAll,
            COALESCE(SUM(CASE WHEN c.issue_date >= DATE_FORMAT(CURDATE(), '%Y-%m-01') THEN c.amount ELSE 0 END), 0) as totalAmountThisMonth,
            COALESCE(SUM(CASE WHEN c.issue_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) THEN c.amount ELSE 0 END), 0) as totalAmountThisWeek,
            COALESCE(SUM(CASE WHEN c.issue_date = CURDATE() THEN c.amount ELSE 0 END), 0) as totalAmountToday,
            COALESCE(SUM(CASE WHEN c.status = 'Pending' THEN c.amount ELSE 0 END), 0) as pendingAmount,
            COALESCE(SUM(CASE WHEN c.status = 'Approved' THEN c.amount ELSE 0 END), 0) as approvedAmount,
            COALESCE(SUM(CASE WHEN c.status = 'Printed' THEN c.amount ELSE 0 END), 0) as printedAmount,
            SUM(CASE WHEN DATE(c.created_at) = CURDATE() THEN 1 ELSE 0 END) as chequesCreatedToday,
            SUM(CASE WHEN c.created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) THEN 1 ELSE 0 END) as chequesCreatedThisWeek,
            SUM(CASE WHEN c.created_at >= DATE_FORMAT(CURDATE(), '%Y-%m-01') THEN 1 ELSE 0 END) as chequesCreatedThisMonth,
            SUM(CASE WHEN c.status = 'Pending' AND c.issue_date < DATE_SUB(CURDATE(), INTERVAL 7 DAY) THEN 1 ELSE 0 END) as overduePendingCount,
            SUM(CASE WHEN c.status = 'Approved' THEN 1 ELSE 0 END) as readyToPrintCount
        FROM cheques c
        WHERE c.is_active = true
        """, nativeQuery = true)
    Map<String, Object> getChequeStatsNative();

    @Query(value = "SELECT status, COUNT(*) as count FROM cheques WHERE is_active = true GROUP BY status", nativeQuery = true)
    List<Map<String, Object>> getStatusBreakdown();

    @Query(value = """
        SELECT DATE_FORMAT(issue_date, '%Y-%m') as month, COALESCE(SUM(amount), 0) as total
        FROM cheques WHERE is_active = true AND issue_date >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
        GROUP BY DATE_FORMAT(issue_date, '%Y-%m') ORDER BY month
        """, nativeQuery = true)
    List<Map<String, Object>> getMonthlyTrend();

    @Query(value = """
        SELECT COALESCE(b.bank_name, 'Unknown') as bankName, COUNT(*) as count
        FROM cheques c LEFT JOIN bank_templates b ON c.bank_id = b.id
        WHERE c.is_active = true GROUP BY b.bank_name ORDER BY count DESC
        """, nativeQuery = true)
    List<Map<String, Object>> getBankBreakdown();

    @Query(value = """
        SELECT DATE(created_at) as day, COUNT(*) as count FROM cheques
        WHERE is_active = true AND created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
        GROUP BY DATE(created_at) ORDER BY day
        """, nativeQuery = true)
    List<Map<String, Object>> getDailyTrend();

    @Query("""
        SELECT c FROM Cheque c LEFT JOIN c.bankTemplate b
        WHERE (:status IS NULL OR c.status = :status)
          AND (:bankId IS NULL OR c.bankId = :bankId)
          AND (:accountId IS NULL OR c.accountId = :accountId)
          AND (:dateFrom IS NULL OR c.issueDate >= :dateFrom)
          AND (:dateTo IS NULL OR c.issueDate <= :dateTo)
          AND (:minAmount IS NULL OR c.amount >= :minAmount)
          AND (:maxAmount IS NULL OR c.amount <= :maxAmount)
          AND (:active IS NULL OR c.isActive = :active)
          AND (:searchText IS NULL OR LOWER(c.payeeName) LIKE LOWER(CONCAT('%', :searchText, '%'))
               OR LOWER(c.chequeNo) LIKE LOWER(CONCAT('%', :searchText, '%'))
               OR LOWER(b.bankName) LIKE LOWER(CONCAT('%', :searchText, '%')))
        """)
    Page<Cheque> searchCheques(
            @Param("status") Cheque.Status status, @Param("bankId") Integer bankId,
            @Param("accountId") Integer accountId, @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo, @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount, @Param("active") Boolean active,
            @Param("searchText") String searchText, Pageable pageable);

    @Query(value = """
        SELECT c.* FROM cheques c LEFT JOIN bank_templates b ON c.bank_id = b.id
        WHERE c.is_active = true
          AND (:searchText IS NULL OR MATCH(c.payee_name) AGAINST(:searchText IN BOOLEAN MODE)
               OR MATCH(c.cheque_no) AGAINST(:searchText IN BOOLEAN MODE)
               OR LOWER(b.bank_name) LIKE LOWER(CONCAT('%', :searchText, '%')))
          AND (:status IS NULL OR c.status = :status) AND (:bankId IS NULL OR c.bank_id = :bankId)
          AND (:dateFrom IS NULL OR c.issue_date >= :dateFrom) AND (:dateTo IS NULL OR c.issue_date <= :dateTo)
        ORDER BY c.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM cheques c LEFT JOIN bank_templates b ON c.bank_id = b.id
        WHERE c.is_active = true
          AND (:searchText IS NULL OR MATCH(c.payee_name) AGAINST(:searchText IN BOOLEAN MODE)
               OR MATCH(c.cheque_no) AGAINST(:searchText IN BOOLEAN MODE)
               OR LOWER(b.bank_name) LIKE LOWER(CONCAT('%', :searchText, '%')))
          AND (:status IS NULL OR c.status = :status) AND (:bankId IS NULL OR c.bank_id = :bankId)
          AND (:dateFrom IS NULL OR c.issue_date >= :dateFrom) AND (:dateTo IS NULL OR c.issue_date <= :dateTo)
        """, nativeQuery = true)
    Page<Cheque> fullTextSearch(
            @Param("searchText") String searchText, @Param("status") String status,
            @Param("bankId") Integer bankId, @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo, Pageable pageable);

    @Query("SELECT c FROM Cheque c ORDER BY c.createdAt DESC")
    List<Cheque> findRecentCheques(Pageable pageable);

    @Query("SELECT c FROM Cheque c WHERE c.status = 'Pending' AND c.issueDate < :date ORDER BY c.issueDate ASC")
    List<Cheque> findOverduePending(@Param("date") LocalDate date);

    @Query("SELECT c FROM Cheque c WHERE c.status = 'Approved' ORDER BY c.createdAt DESC")
    List<Cheque> findReadyToPrint(Pageable pageable);

    @Query("SELECT c FROM Cheque c WHERE LOWER(c.payeeName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.chequeNo) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Cheque> searchByQuery(@Param("query") String query);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Cheque c WHERE c.chequeNo = :chequeNo AND c.id != :excludeId")
    boolean existsByChequeNo(@Param("chequeNo") String chequeNo, @Param("excludeId") int excludeId);
}
