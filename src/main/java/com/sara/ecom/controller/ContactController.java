package com.sara.ecom.controller;

import com.sara.ecom.dto.ContactSubmissionDto;
import com.sara.ecom.dto.ContactSubmissionRequest;
import com.sara.ecom.entity.ContactSubmission;
import com.sara.ecom.service.ContactSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ContactController {
    
    @Autowired
    private ContactSubmissionService contactSubmissionService;
    
    // Public endpoint - submit contact form
    @PostMapping("/contact/submit")
    public ResponseEntity<Map<String, String>> submitContact(@RequestBody ContactSubmissionRequest request) {
        try {
            contactSubmissionService.createSubmission(request);
            return ResponseEntity.ok(Map.of("message", "Thank you for contacting us! We'll get back to you soon."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Admin endpoints
    @GetMapping("/admin/contact/submissions")
    public ResponseEntity<List<ContactSubmissionDto>> getAllSubmissions(
            @RequestParam(required = false) String status) {
        if (status != null) {
            try {
                ContactSubmission.Status statusEnum = ContactSubmission.Status.valueOf(status.toUpperCase());
                return ResponseEntity.ok(contactSubmissionService.getSubmissionsByStatus(statusEnum));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(contactSubmissionService.getAllSubmissions());
            }
        }
        return ResponseEntity.ok(contactSubmissionService.getAllSubmissions());
    }
    
    @GetMapping("/admin/contact/submissions/{id}")
    public ResponseEntity<ContactSubmissionDto> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(contactSubmissionService.getSubmissionById(id));
    }
    
    @PutMapping("/admin/contact/submissions/{id}/status")
    public ResponseEntity<ContactSubmissionDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String statusStr = request.get("status");
        String adminNotes = request.get("adminNotes");
        
        ContactSubmission.Status status = ContactSubmission.Status.valueOf(statusStr.toUpperCase());
        return ResponseEntity.ok(contactSubmissionService.updateStatus(id, status, adminNotes));
    }
    
    @DeleteMapping("/admin/contact/submissions/{id}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        contactSubmissionService.deleteSubmission(id);
        return ResponseEntity.ok().build();
    }
}
