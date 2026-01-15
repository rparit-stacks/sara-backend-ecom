package com.sara.ecom.controller;

import com.sara.ecom.dto.FAQDto;
import com.sara.ecom.dto.FAQRequest;
import com.sara.ecom.service.FAQService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FAQController {
    
    @Autowired
    private FAQService faqService;
    
    // Public endpoints
    @GetMapping("/faqs")
    public ResponseEntity<List<FAQDto>> getAllFAQs(@RequestParam(required = false) String category) {
        List<FAQDto> faqs = faqService.getAllFAQs(category);
        return ResponseEntity.ok(faqs);
    }
    
    @GetMapping("/faqs/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(faqService.getAllCategories());
    }
    
    // Admin endpoints
    @GetMapping("/admin/faqs")
    public ResponseEntity<List<FAQDto>> getAllFAQsAdmin() {
        List<FAQDto> faqs = faqService.getAllFAQsAdmin();
        return ResponseEntity.ok(faqs);
    }
    
    @GetMapping("/admin/faqs/{id}")
    public ResponseEntity<FAQDto> getFAQById(@PathVariable Long id) {
        FAQDto faq = faqService.getFAQById(id);
        return ResponseEntity.ok(faq);
    }
    
    @PostMapping("/admin/faqs")
    public ResponseEntity<FAQDto> createFAQ(@RequestBody FAQRequest request) {
        FAQDto created = faqService.createFAQ(request);
        return ResponseEntity.ok(created);
    }
    
    @PutMapping("/admin/faqs/{id}")
    public ResponseEntity<FAQDto> updateFAQ(
            @PathVariable Long id,
            @RequestBody FAQRequest request) {
        FAQDto updated = faqService.updateFAQ(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @PutMapping("/admin/faqs/reorder")
    public ResponseEntity<Void> reorderFAQs(@RequestBody Map<String, List<Long>> request) {
        List<Long> orderedIds = request.get("ids");
        faqService.reorderFAQs(orderedIds);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/admin/faqs/{id}")
    public ResponseEntity<Void> deleteFAQ(@PathVariable Long id) {
        faqService.deleteFAQ(id);
        return ResponseEntity.noContent().build();
    }
}
