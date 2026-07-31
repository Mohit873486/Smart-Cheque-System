package com.chequeprint.backend.repository;

import com.chequeprint.backend.entity.BankAccount;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.query.timeout", value = "3000"),
            @QueryHint(name = "org.hibernate.readOnly", value = "true")
    })
    List<BankAccount> findAllByOrderByIdAsc();

    Optional<BankAccount> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
    boolean existsByAccountNumberAndIdNot(String accountNumber, Long id);
}
