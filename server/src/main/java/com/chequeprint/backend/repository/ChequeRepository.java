package com.chequeprint.backend.repository;

import com.chequeprint.backend.entity.Cheque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChequeRepository extends JpaRepository<Cheque, Integer> {
    boolean existsByChequeNoAndIdNot(String chequeNo, int id);
    Optional<Cheque> findByChequeNo(String chequeNo);
}
