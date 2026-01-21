package com.sara.ecom.repository;

import com.sara.ecom.entity.WhatsAppTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface WhatsAppTemplateRepository extends JpaRepository<WhatsAppTemplate, Long> {
    Optional<WhatsAppTemplate> findByStatusType(String statusType);
    List<WhatsAppTemplate> findByIsEnabledTrue();
}
