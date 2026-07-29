package com.chequeprint.backend.repository;

import com.chequeprint.backend.entity.ChequeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface ChequeTemplateRepository extends JpaRepository<ChequeTemplate, Long> {
    List<ChequeTemplate> findByBankId(Long bankId);
    List<ChequeTemplate> findByAccountId(Long accountId);
    List<ChequeTemplate> findByAccountIdOrBankId(Long accountId, Long bankId);
    Optional<ChequeTemplate> findByAccountIdAndIsDefaultTrue(Long accountId);
}
