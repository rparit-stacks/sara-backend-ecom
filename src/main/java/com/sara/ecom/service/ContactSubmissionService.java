package com.sara.ecom.service;

import com.sara.ecom.dto.ContactSubmissionDto;
import com.sara.ecom.dto.ContactSubmissionRequest;
import com.sara.ecom.entity.ContactSubmission;
import com.sara.ecom.repository.ContactSubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactSubmissionService {
    
    @Autowired
    private ContactSubmissionRepository contactSubmissionRepository;
    
    @Transactional
    public ContactSubmissionDto createSubmission(ContactSubmissionRequest request) {
        ContactSubmission submission = new ContactSubmission();
        submission.setFirstName(request.getFirstName());
        submission.setLastName(request.getLastName());
        submission.setEmail(request.getEmail());
        submission.setCountryCode(request.getCountryCode());
        submission.setPhoneNumber(request.getPhoneNumber());
        submission.setSubject(request.getSubject());
        submission.setMessage(request.getMessage());
        submission.setStatus(ContactSubmission.Status.NEW);
        
        return toDto(contactSubmissionRepository.save(submission));
    }
    
    public List<ContactSubmissionDto> getAllSubmissions() {
        return contactSubmissionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public List<ContactSubmissionDto> getSubmissionsByStatus(ContactSubmission.Status status) {
        return contactSubmissionRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public ContactSubmissionDto getSubmissionById(Long id) {
        ContactSubmission submission = contactSubmissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact submission not found"));
        return toDto(submission);
    }
    
    @Transactional
    public ContactSubmissionDto updateStatus(Long id, ContactSubmission.Status status, String adminNotes) {
        ContactSubmission submission = contactSubmissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact submission not found"));
        submission.setStatus(status);
        if (adminNotes != null) {
            submission.setAdminNotes(adminNotes);
        }
        return toDto(contactSubmissionRepository.save(submission));
    }
    
    @Transactional
    public void deleteSubmission(Long id) {
        if (!contactSubmissionRepository.existsById(id)) {
            throw new RuntimeException("Contact submission not found");
        }
        contactSubmissionRepository.deleteById(id);
    }
    
    private ContactSubmissionDto toDto(ContactSubmission submission) {
        ContactSubmissionDto dto = new ContactSubmissionDto();
        dto.setId(submission.getId());
        dto.setFirstName(submission.getFirstName());
        dto.setLastName(submission.getLastName());
        dto.setEmail(submission.getEmail());
        dto.setCountryCode(submission.getCountryCode());
        dto.setPhoneNumber(submission.getPhoneNumber());
        dto.setSubject(submission.getSubject());
        dto.setMessage(submission.getMessage());
        dto.setStatus(submission.getStatus().name());
        dto.setAdminNotes(submission.getAdminNotes());
        dto.setCreatedAt(submission.getCreatedAt());
        dto.setUpdatedAt(submission.getUpdatedAt());
        return dto;
    }
}
