package com.sara.ecom.service;

import com.sara.ecom.dto.PlainProductDto;
import com.sara.ecom.dto.PlainProductRequest;
import com.sara.ecom.entity.PlainProduct;
import com.sara.ecom.entity.PlainProductVariant;
import com.sara.ecom.entity.PlainProductVariantOption;
import com.sara.ecom.repository.PlainProductRepository;
import com.sara.ecom.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlainProductService {
    
    @Autowired
    private PlainProductRepository plainProductRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    public List<PlainProductDto> getAllPlainProducts(String status, Long categoryId) {
        List<PlainProduct> products;
        
        if (status != null && categoryId != null) {
            PlainProduct.Status statusEnum = PlainProduct.Status.valueOf(status.toUpperCase());
            products = plainProductRepository.findByStatusAndCategoryId(statusEnum, categoryId);
        } else if (status != null) {
            PlainProduct.Status statusEnum = PlainProduct.Status.valueOf(status.toUpperCase());
            products = plainProductRepository.findAllWithVariantsByStatus(statusEnum);
        } else if (categoryId != null) {
            products = plainProductRepository.findByCategoryId(categoryId);
        } else {
            products = plainProductRepository.findAllWithVariants();
        }
        
        List<PlainProductDto> result = products.stream().map(this::toDto).collect(Collectors.toList());
        
        // Filter by fabric categories only if no specific categoryId is provided
        if (categoryId == null && status != null) {
            // Get all fabric category IDs
            List<com.sara.ecom.entity.Category> fabricCategories = 
                categoryRepository.findByIsFabricTrueAndStatus(com.sara.ecom.entity.Category.Status.valueOf(status.toUpperCase()));
            List<Long> fabricCategoryIds = fabricCategories.stream()
                .map(com.sara.ecom.entity.Category::getId)
                .collect(Collectors.toList());
            
            // Filter products to only include those in fabric categories
            if (!fabricCategoryIds.isEmpty()) {
                result = result.stream()
                    .filter(p -> p.getCategoryId() != null && fabricCategoryIds.contains(p.getCategoryId()))
                    .collect(Collectors.toList());
            }
        }
        
        return result;
    }
    
    /**
     * Gets all plain products that are in fabric categories only.
     * Used for fabric selection in design products.
     */
    public List<PlainProductDto> getFabricPlainProducts(String status) {
        // Get all fabric categories
        List<com.sara.ecom.entity.Category> fabricCategories = 
            categoryRepository.findByIsFabricTrueAndStatus(
                status != null ? 
                    com.sara.ecom.entity.Category.Status.valueOf(status.toUpperCase()) : 
                    com.sara.ecom.entity.Category.Status.ACTIVE
            );
        List<Long> fabricCategoryIds = fabricCategories.stream()
            .map(com.sara.ecom.entity.Category::getId)
            .collect(Collectors.toList());
        
        if (fabricCategoryIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Get all plain products in fabric categories
        List<PlainProduct> products;
        if (status != null) {
            PlainProduct.Status statusEnum = PlainProduct.Status.valueOf(status.toUpperCase());
            products = plainProductRepository.findAllWithVariantsByStatus(statusEnum);
        } else {
            products = plainProductRepository.findAllWithVariants();
        }
        
        // Filter by fabric categories
        return products.stream()
            .filter(p -> p.getCategoryId() != null && fabricCategoryIds.contains(p.getCategoryId()))
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    public PlainProductDto getPlainProductById(Long id) {
        PlainProduct product = plainProductRepository.findByIdWithVariants(id)
                .orElseThrow(() -> new RuntimeException("Plain product not found with id: " + id));
        return toDto(product);
    }
    
    public List<PlainProductDto> getPlainProductsByIds(List<Long> ids) {
        List<PlainProduct> products = plainProductRepository.findByIdIn(ids);
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    @Transactional
    public PlainProductDto createPlainProduct(PlainProductRequest request) {
        PlainProduct product = new PlainProduct();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImage(request.getImage());
        product.setPricePerMeter(request.getPricePerMeter());
        product.setCategoryId(request.getCategoryId());
        
        if (request.getStatus() != null) {
            product.setStatus(PlainProduct.Status.valueOf(request.getStatus().toUpperCase()));
        }
        
        if (request.getVariants() != null) {
            for (PlainProductRequest.VariantRequest variantReq : request.getVariants()) {
                PlainProductVariant variant = new PlainProductVariant();
                variant.setType(variantReq.getType());
                variant.setName(variantReq.getName());
                
                if (variantReq.getOptions() != null) {
                    for (PlainProductRequest.OptionRequest optionReq : variantReq.getOptions()) {
                        PlainProductVariantOption option = new PlainProductVariantOption();
                        option.setValue(optionReq.getValue());
                        option.setPriceModifier(optionReq.getPriceModifier() != null ? 
                                optionReq.getPriceModifier() : BigDecimal.ZERO);
                        variant.addOption(option);
                    }
                }
                
                product.addVariant(variant);
            }
        }
        
        PlainProduct saved = plainProductRepository.save(product);
        return toDto(saved);
    }
    
    @Transactional
    public PlainProductDto updatePlainProduct(Long id, PlainProductRequest request) {
        PlainProduct product = plainProductRepository.findByIdWithVariants(id)
                .orElseThrow(() -> new RuntimeException("Plain product not found with id: " + id));
        
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImage(request.getImage());
        product.setPricePerMeter(request.getPricePerMeter());
        product.setCategoryId(request.getCategoryId());
        
        if (request.getStatus() != null) {
            product.setStatus(PlainProduct.Status.valueOf(request.getStatus().toUpperCase()));
        }
        
        // Clear existing variants and add new ones
        product.getVariants().clear();
        
        if (request.getVariants() != null) {
            for (PlainProductRequest.VariantRequest variantReq : request.getVariants()) {
                PlainProductVariant variant = new PlainProductVariant();
                variant.setType(variantReq.getType());
                variant.setName(variantReq.getName());
                
                if (variantReq.getOptions() != null) {
                    for (PlainProductRequest.OptionRequest optionReq : variantReq.getOptions()) {
                        PlainProductVariantOption option = new PlainProductVariantOption();
                        option.setValue(optionReq.getValue());
                        option.setPriceModifier(optionReq.getPriceModifier() != null ? 
                                optionReq.getPriceModifier() : BigDecimal.ZERO);
                        variant.addOption(option);
                    }
                }
                
                product.addVariant(variant);
            }
        }
        
        PlainProduct saved = plainProductRepository.save(product);
        return toDto(saved);
    }
    
    @Transactional
    public void deletePlainProduct(Long id) {
        if (!plainProductRepository.existsById(id)) {
            throw new RuntimeException("Plain product not found with id: " + id);
        }
        plainProductRepository.deleteById(id);
    }
    
    private PlainProductDto toDto(PlainProduct product) {
        PlainProductDto dto = new PlainProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setImage(product.getImage());
        dto.setPricePerMeter(product.getPricePerMeter());
        dto.setCategoryId(product.getCategoryId());
        dto.setStatus(product.getStatus().name());
        
        if (product.getVariants() != null) {
            dto.setVariants(product.getVariants().stream().map(this::toVariantDto).collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private PlainProductDto.VariantDto toVariantDto(PlainProductVariant variant) {
        PlainProductDto.VariantDto dto = new PlainProductDto.VariantDto();
        dto.setId(variant.getId());
        dto.setType(variant.getType());
        dto.setName(variant.getName());
        
        if (variant.getOptions() != null) {
            dto.setOptions(variant.getOptions().stream().map(this::toOptionDto).collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private PlainProductDto.OptionDto toOptionDto(PlainProductVariantOption option) {
        PlainProductDto.OptionDto dto = new PlainProductDto.OptionDto();
        dto.setId(option.getId());
        dto.setValue(option.getValue());
        dto.setPriceModifier(option.getPriceModifier());
        return dto;
    }

    public List<PlainProductDto> getAllPlainProductsList(String active) {
        List<PlainProductDto> plainProductDtoList = new ArrayList<>();
        List<PlainProduct> plainProductList = plainProductRepository.findByStatus(PlainProduct.Status.valueOf(active.toUpperCase()));
        for (PlainProduct plainProduct : plainProductList) {
            plainProductDtoList.add(toDto(plainProduct));
        }
        return plainProductDtoList;
    }
}
