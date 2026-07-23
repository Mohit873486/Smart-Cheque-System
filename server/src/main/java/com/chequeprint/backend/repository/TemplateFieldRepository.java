package com.chequeprint.backend.repository;

import com.chequeprint.backend.entity.TemplateField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateFieldRepository extends JpaRepository<TemplateField, Long> {
    List<TemplateField> findByTemplateId(Long templateId);
    void deleteByTemplateId(Long templateId);
}
