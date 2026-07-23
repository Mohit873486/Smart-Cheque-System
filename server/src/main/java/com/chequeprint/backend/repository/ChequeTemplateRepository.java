package com.chequeprint.backend.repository;

import com.chequeprint.backend.entity.ChequeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChequeTemplateRepository extends JpaRepository<ChequeTemplate, Long> {
    List<ChequeTemplate> findByBankId(Long bankId);
}
