package com.sara.ecom.service;

import com.sara.ecom.dto.DesignDto;
import com.sara.ecom.dto.DesignRequest;
import com.sara.ecom.entity.Design;
import com.sara.ecom.entity.PlainProduct;
import com.sara.ecom.repository.DesignRepository;
import com.sara.ecom.repository.PlainProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DesignService {
    
    @Autowired
    private DesignRepository designRepository;
    
    @Autowired
    private PlainProductRepository plainProductRepository;
    
    public List<DesignDto> getAllDesigns(String status, String category) {
        List<Design> designs;
        
        if (status != null && category != null) {
            Design.Status statusEnum = Design.Status.valueOf(status.toUpperCase());
            designs = designRepository.findByStatusAndCategory(statusEnum, category);
        } else if (status != null) {
            Design.Status statusEnum = Design.Status.valueOf(status.toUpperCase());
            designs = designRepository.findAllWithFabricsByStatus(statusEnum);
        } else if (category != null) {
            designs = designRepository.findByCategory(category);
        } else {
            designs = designRepository.findAllWithFabrics();
        }
        
        return designs.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    public DesignDto getDesignById(Long id) {
        Design design = designRepository.findByIdWithFabrics(id)
                .orElseThrow(() -> new RuntimeException("Design not found with id: " + id));
        return toDto(design);
    }
    
    public List<String> getAllCategories() {
        return designRepository.findAllCategories();
    }
    
    @Transactional
    public DesignDto createDesign(DesignRequest request) {
        Design design = new Design();
        design.setName(request.getName());
        design.setCategory(request.getCategory());
        design.setImage(request.getImage());
        design.setDescription(request.getDescription());
        
        if (request.getStatus() != null) {
            design.setStatus(Design.Status.valueOf(request.getStatus().toUpperCase()));
        }
        
        if (request.getAssignedFabricIds() != null && !request.getAssignedFabricIds().isEmpty()) {
            List<PlainProduct> fabrics = plainProductRepository.findByIdIn(request.getAssignedFabricIds());
            design.setAssignedFabrics(new HashSet<>(fabrics));
        }
        
        Design saved = designRepository.save(design);
        return toDto(saved);
    }
    
    @Transactional
    public DesignDto updateDesign(Long id, DesignRequest request) {
        Design design = designRepository.findByIdWithFabrics(id)
                .orElseThrow(() -> new RuntimeException("Design not found with id: " + id));
        
        design.setName(request.getName());
        design.setCategory(request.getCategory());
        design.setImage(request.getImage());
        design.setDescription(request.getDescription());
        
        if (request.getStatus() != null) {
            design.setStatus(Design.Status.valueOf(request.getStatus().toUpperCase()));
        }
        
        // Update assigned fabrics
        design.getAssignedFabrics().clear();
        if (request.getAssignedFabricIds() != null && !request.getAssignedFabricIds().isEmpty()) {
            List<PlainProduct> fabrics = plainProductRepository.findByIdIn(request.getAssignedFabricIds());
            design.setAssignedFabrics(new HashSet<>(fabrics));
        }
        
        Design saved = designRepository.save(design);
        return toDto(saved);
    }
    
    @Transactional
    public void deleteDesign(Long id) {
        if (!designRepository.existsById(id)) {
            throw new RuntimeException("Design not found with id: " + id);
        }
        designRepository.deleteById(id);
    }
    
    private DesignDto toDto(Design design) {
        DesignDto dto = new DesignDto();
        dto.setId(design.getId());
        dto.setName(design.getName());
        dto.setCategory(design.getCategory());
        dto.setImage(design.getImage());
        dto.setDescription(design.getDescription());
        dto.setStatus(design.getStatus().name());
        dto.setFabricCount(design.getFabricCount());
        
        if (design.getAssignedFabrics() != null) {
            dto.setAssignedFabricIds(
                design.getAssignedFabrics().stream()
                    .map(PlainProduct::getId)
                    .collect(Collectors.toList())
            );
            
            dto.setAssignedFabrics(
                design.getAssignedFabrics().stream()
                    .map(this::toFabricSummary)
                    .collect(Collectors.toList())
            );
        }
        
        return dto;
    }
    
    private DesignDto.FabricSummary toFabricSummary(PlainProduct fabric) {
        DesignDto.FabricSummary summary = new DesignDto.FabricSummary();
        summary.setId(fabric.getId());
        summary.setName(fabric.getName());
        summary.setImage(fabric.getImage());
        summary.setStatus(fabric.getStatus().name());
        return summary;
    }
}
