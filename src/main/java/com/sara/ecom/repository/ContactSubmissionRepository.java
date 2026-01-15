package com.sara.ecom.repository;

import com.sara.ecom.entity.ContactSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactSubmissionRepository extends JpaRepository<ContactSubmission, Long> {
    
    List<ContactSubmission> findByStatusOrderByCreatedAtDesc(ContactSubmission.Status status);
    
    List<ContactSubmission> findAllByOrderByCreatedAtDesc();
}
