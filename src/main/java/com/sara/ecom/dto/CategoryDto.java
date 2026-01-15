package com.sara.ecom.dto;

import com.sara.ecom.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDto {
    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private String status;
    private String image;
    private String description;
    private Integer displayOrder;
    private Boolean isFabric;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CategoryDto> subcategories;
    
    // Product count fields - helps frontend distinguish "empty category" from "not found"
    private Long productCount;       // Total products in this category + all subcategories
    private Long directProductCount; // Products directly in this category (not in subcategories)
    
    public static CategoryDto fromEntity(Category category) {
        if (category == null) return null;
        
        CategoryDto dto = CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParentId())
                .status(category.getStatus() != null ? category.getStatus().name() : null)
                .image(category.getImage())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .isFabric(category.getIsFabric())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
        
        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            dto.setSubcategories(category.getSubcategories().stream()
                    .map(CategoryDto::fromEntity)
                    .collect(Collectors.toList()));
        } else {
            dto.setSubcategories(new ArrayList<>());
        }
        
        return dto;
    }
}
