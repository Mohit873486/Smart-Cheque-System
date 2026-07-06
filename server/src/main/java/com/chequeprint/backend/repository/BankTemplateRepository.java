package com.chequeprint.backend.repository;

import com.chequeprint.backend.entity.BankTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankTemplateRepository extends JpaRepository<BankTemplate, Integer> {
    Optional<BankTemplate> findByBankCode(String bankCode);
}
