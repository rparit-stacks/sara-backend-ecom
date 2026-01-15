package com.sara.ecom.service;

import com.sara.ecom.dto.FAQDto;
import com.sara.ecom.dto.FAQRequest;
import com.sara.ecom.entity.FAQ;
import com.sara.ecom.repository.FAQRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FAQService {
    
    @Autowired
    private FAQRepository faqRepository;
    
    public List<FAQDto> getAllFAQs(String category) {
        List<FAQ> faqs;
        if (category != null) {
            faqs = faqRepository.findByCategoryAndStatusOrderByDisplayOrderAsc(category, FAQ.Status.ACTIVE);
        } else {
            faqs = faqRepository.findByStatusOrderByDisplayOrderAsc(FAQ.Status.ACTIVE);
        }
        return faqs.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    public List<FAQDto> getAllFAQsAdmin() {
        return faqRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public FAQDto getFAQById(Long id) {
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FAQ not found with id: " + id));
        return toDto(faq);
    }
    
    public List<String> getAllCategories() {
        return faqRepository.findAllCategories();
    }
    
    @Transactional
    public FAQDto createFAQ(FAQRequest request) {
        FAQ faq = new FAQ();
        mapRequestToFAQ(request, faq);
        return toDto(faqRepository.save(faq));
    }
    
    @Transactional
    public FAQDto updateFAQ(Long id, FAQRequest request) {
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FAQ not found with id: " + id));
        mapRequestToFAQ(request, faq);
        return toDto(faqRepository.save(faq));
    }
    
    @Transactional
    public void reorderFAQs(List<Long> orderedIds) {
        int order = 0;
        for (Long id : orderedIds) {
            FAQ faq = faqRepository.findById(id).orElse(null);
            if (faq != null) {
                faq.setDisplayOrder(order++);
                faqRepository.save(faq);
            }
        }
    }
    
    @Transactional
    public void deleteFAQ(Long id) {
        if (!faqRepository.existsById(id)) {
            throw new RuntimeException("FAQ not found with id: " + id);
        }
        faqRepository.deleteById(id);
    }
    
    private void mapRequestToFAQ(FAQRequest request, FAQ faq) {
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setCategory(request.getCategory());
        faq.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        
        if (request.getStatus() != null) {
            faq.setStatus(FAQ.Status.valueOf(request.getStatus().toUpperCase()));
        }
    }
    
    private FAQDto toDto(FAQ faq) {
        FAQDto dto = new FAQDto();
        dto.setId(faq.getId());
        dto.setQuestion(faq.getQuestion());
        dto.setAnswer(faq.getAnswer());
        dto.setCategory(faq.getCategory());
        dto.setDisplayOrder(faq.getDisplayOrder());
        dto.setStatus(faq.getStatus().name());
        return dto;
    }
}
