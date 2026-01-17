package com.sara.ecom.repository;

import com.sara.ecom.entity.SentMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SentMessageRepository extends JpaRepository<SentMessage, Long> {
    List<SentMessage> findAllByOrderByCreatedAtDesc();
    List<SentMessage> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<SentMessage> findByMessageTypeOrderByCreatedAtDesc(SentMessage.MessageType messageType);
    List<SentMessage> findByStatusOrderByCreatedAtDesc(SentMessage.Status status);
}
