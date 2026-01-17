package com.sara.ecom.repository;

import com.sara.ecom.entity.ChatbotRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatbotRuleRepository extends JpaRepository<ChatbotRule, Long> {
    List<ChatbotRule> findByIsActiveTrueOrderByPriorityDesc();
    List<ChatbotRule> findAllByOrderByPriorityDesc();
}
