package com.sara.ecom.repository;

import com.sara.ecom.entity.ChatbotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatbotConfigRepository extends JpaRepository<ChatbotConfig, Long> {
    Optional<ChatbotConfig> findFirstByOrderByIdAsc();
}
