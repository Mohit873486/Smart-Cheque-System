package com.chequeprint.backend.repository;

import com.chequeprint.backend.entity.TemplateLayoutField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateLayoutFieldRepository extends JpaRepository<TemplateLayoutField, Long> {

    /** Returns all layout field rows for the given template, ordered by field name. */
    List<TemplateLayoutField> findByTemplateIdOrderByFieldName(Long templateId);

    /** Deletes all layout fields for a template (used before re-saving the full set). */
    void deleteByTemplateId(Long templateId);
}
