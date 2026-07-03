package com.chequeprint.backend.repository;

import com.chequeprint.backend.entity.Cheque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChequeRepository extends JpaRepository<Cheque, Integer> {
    boolean existsByChequeNoAndIdNot(String chequeNo, int id);
    Optional<Cheque> findByChequeNo(String chequeNo);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Cheque c WHERE " +
            "LOWER(c.payeeName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.chequeNo) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.bankTemplate.bankName) LIKE LOWER(CONCAT('%', :query, '%'))")
    java.util.List<Cheque> searchCheques(@org.springframework.data.repository.query.Param("query") String query);
}
