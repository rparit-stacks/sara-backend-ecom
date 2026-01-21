package com.sara.ecom.repository;

import com.sara.ecom.entity.WhatsAppNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppNotificationLogRepository extends JpaRepository<WhatsAppNotificationLog, Long> {
    List<WhatsAppNotificationLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    Page<WhatsAppNotificationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
