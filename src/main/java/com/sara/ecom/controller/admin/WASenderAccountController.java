package com.sara.ecom.controller.admin;

import com.sara.ecom.dto.WASenderAccountDto;
import com.sara.ecom.dto.WASenderAccountRequest;
import com.sara.ecom.entity.WASenderAccount;
import com.sara.ecom.repository.WASenderAccountRepository;
import com.sara.ecom.service.WASenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/whatsapp/accounts")
public class WASenderAccountController {
    
    @Autowired
    private WASenderAccountRepository accountRepository;
    
    @Autowired
    private WASenderService wasenderService;
    
    @PostMapping
    public ResponseEntity<WASenderAccountDto> createAccount(@RequestBody WASenderAccountRequest request) {
        WASenderAccount account = WASenderAccount.builder()
                .accountName(request.getAccountName())
                .bearerToken(request.getBearerToken())
                .whatsappNumber(request.getWhatsappNumber())
                .isActive(false) // New accounts are inactive by default
                .build();
        
        account = accountRepository.save(account);
        return ResponseEntity.ok(toDto(account));
    }
    
    @GetMapping
    public ResponseEntity<List<WASenderAccountDto>> getAllAccounts() {
        List<WASenderAccountDto> accounts = accountRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(accounts);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WASenderAccountDto> getAccountById(@PathVariable Long id) {
        WASenderAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return ResponseEntity.ok(toDto(account));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WASenderAccountDto> updateAccount(@PathVariable Long id, @RequestBody WASenderAccountRequest request) {
        WASenderAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        account.setAccountName(request.getAccountName());
        account.setBearerToken(request.getBearerToken());
        account.setWhatsappNumber(request.getWhatsappNumber());
        
        account = accountRepository.save(account);
        return ResponseEntity.ok(toDto(account));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        WASenderAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (account.getIsActive()) {
            throw new RuntimeException("Cannot delete active account. Please activate another account first.");
        }
        
        accountRepository.delete(account);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}/activate")
    public ResponseEntity<WASenderAccountDto> activateAccount(@PathVariable Long id) {
        wasenderService.activateAccount(id);
        WASenderAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return ResponseEntity.ok(toDto(account));
    }
    
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        WASenderAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        boolean isConnected = wasenderService.testConnection(account.getBearerToken());
        
        return ResponseEntity.ok(Map.of(
                "success", isConnected,
                "message", isConnected ? "Connection successful" : "Connection failed. Please check your bearer token."
        ));
    }
    
    private WASenderAccountDto toDto(WASenderAccount account) {
        return WASenderAccountDto.builder()
                .id(account.getId())
                .accountName(account.getAccountName())
                .whatsappNumber(account.getWhatsappNumber())
                .isActive(account.getIsActive())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
