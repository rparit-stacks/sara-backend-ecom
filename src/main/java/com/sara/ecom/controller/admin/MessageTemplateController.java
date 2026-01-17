package com.sara.ecom.controller.admin;

import com.sara.ecom.dto.MessageTemplateDto;
import com.sara.ecom.service.MessageTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/whatsapp/templates")
public class MessageTemplateController {
    
    @Autowired
    private MessageTemplateService templateService;
    
    @PostMapping
    public ResponseEntity<MessageTemplateDto> createTemplate(@RequestBody MessageTemplateDto dto) {
        return ResponseEntity.ok(templateService.createTemplate(dto));
    }
    
    @GetMapping
    public ResponseEntity<List<MessageTemplateDto>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MessageTemplateDto> getTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MessageTemplateDto> updateTemplate(@PathVariable Long id, @RequestBody MessageTemplateDto dto) {
        return ResponseEntity.ok(templateService.updateTemplate(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
