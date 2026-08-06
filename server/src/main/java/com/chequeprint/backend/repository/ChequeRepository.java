package com.chequeprint.backend.repository;

import com.chequeprint.backend.entity.Cheque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

// ✅ ADD THESE IMPORTS
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ChequeRepository extends JpaRepository<Cheque, Integer> {

    boolean existsByChequeNoAndIdNot(String chequeNo, int id);

    Optional<Cheque> findByChequeNo(String chequeNo);

    @Query("SELECT c FROM Cheque c WHERE " +
            "LOWER(c.payeeName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.chequeNo) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.bankTemplate.bankName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Cheque> searchCheques(@Param("query") String query);

    long countByStatus(Cheque.Status status);

    @Query("SELECT COUNT(c) FROM Cheque c WHERE c.issueDate = :date")
    long countByIssueDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(c.amount) FROM Cheque c WHERE MONTH(c.issueDate) = MONTH(CURRENT_DATE) AND YEAR(c.issueDate) = YEAR(CURRENT_DATE)")
    BigDecimal sumThisMonth();

    @Query("SELECT c.issueDate, COUNT(c) FROM Cheque c WHERE c.issueDate BETWEEN :start AND :end GROUP BY c.issueDate")
    List<Object[]> countByDateRange(@Param("start") LocalDate start,
                                   @Param("end") LocalDate end);
}