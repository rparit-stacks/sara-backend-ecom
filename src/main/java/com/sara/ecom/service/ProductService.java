package com.sara.ecom.service;

import com.sara.ecom.dto.PlainProductDto;
import com.sara.ecom.dto.ProductDto;
import com.sara.ecom.dto.ProductRequest;
import com.sara.ecom.entity.*;
import com.sara.ecom.repository.CategoryRepository;
import com.sara.ecom.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private PlainProductService plainProductService;
    
    public List<ProductDto> getAllProducts(String status, String type, Long categoryId) {
        List<Product> products;
        
        Product.Status statusEnum = status != null ? Product.Status.valueOf(status.toUpperCase()) : null;
        Product.ProductType typeEnum = type != null ? Product.ProductType.valueOf(type.toUpperCase()) : null;
        
        if (statusEnum != null && typeEnum != null && categoryId != null) {
            products = productRepository.findByStatusAndTypeAndCategoryId(statusEnum, typeEnum, categoryId);
        } else if (statusEnum != null && typeEnum != null) {
            products = productRepository.findByStatusAndType(statusEnum, typeEnum);
        } else if (statusEnum != null && categoryId != null) {
            products = productRepository.findByStatusAndCategoryId(statusEnum, categoryId);
        } else if (typeEnum != null && categoryId != null) {
            products = productRepository.findByTypeAndCategoryId(typeEnum, categoryId);
        } else if (statusEnum != null) {
            products = productRepository.findAllWithImagesByStatus(statusEnum);
        } else if (typeEnum != null) {
            products = productRepository.findByType(typeEnum);
        } else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else {
            products = productRepository.findAllWithImages();
        }
        
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        // Use separate queries to avoid MultipleBagFetchException
        Product product = productRepository.findByIdWithImages(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        // Load detail sections separately
        productRepository.findByIdWithDetailSections(id).ifPresent(p -> 
            product.setDetailSections(p.getDetailSections()));
        
        return toDtoWithDetails(product);
    }
    
    @Transactional(readOnly = true)
    public ProductDto getProductBySlug(String slug) {
        // Try to find by slug (any status first, then fallback to active only)
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Product not found with slug: " + slug));
        
        // Load images and detail sections separately
        productRepository.findByIdWithImages(product.getId()).ifPresent(p -> 
            product.setImages(p.getImages()));
        productRepository.findByIdWithDetailSections(product.getId()).ifPresent(p -> 
            product.setDetailSections(p.getDetailSections()));
        
        return toDtoWithDetails(product);
    }
    
    public List<ProductDto> getProductsByIds(List<Long> ids) {
        List<Product> products = productRepository.findByIdIn(ids);
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    /**
     * Gets all active products for a category and all its child categories.
     * This allows fetching products from any category level (parent, child, grandchild, etc.)
     */
    public List<ProductDto> getProductsByCategoryWithChildren(Long categoryId) {
        // Get all category IDs including descendants
        List<Long> allCategoryIds = categoryService.getAllDescendantCategoryIds(categoryId);
        
        // Fetch all products in these categories
        List<Product> products = productRepository.findByCategoryIdsAndStatus(
                allCategoryIds, Product.Status.ACTIVE);
        
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    @Transactional
    public ProductDto createProduct(ProductRequest request) {
        // Validate that category is a leaf category (has no subcategories)
        if (request.getCategoryId() != null) {
            if (!categoryService.isLeafCategory(request.getCategoryId())) {
                throw new RuntimeException("Products can only be added to leaf categories (categories without subcategories). Please select a subcategory instead.");
            }
        }
        
        Product product = new Product();
        mapRequestToProduct(request, product);
        Product saved = productRepository.save(product);
        return toDtoWithDetails(saved);
    }

    @Transactional
    public ProductDto createPlainProduct(ProductRequest request) {
        request.setType("PLAIN");
        return createProduct(request);
    }

    @Transactional
    public ProductDto createDesignedProduct(ProductRequest request) {
        request.setType("DESIGNED");
        return createProduct(request);
    }

    @Transactional
    public ProductDto createDigitalProduct(ProductRequest request) {
        request.setType("DIGITAL");
        return createProduct(request);
    }
    
    @Transactional
    public ProductDto updateProduct(Long id, ProductRequest request) {
        // Use separate queries to avoid MultipleBagFetchException
        Product product = productRepository.findByIdWithImages(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        // Load detail sections separately
        productRepository.findByIdWithDetailSections(id).ifPresent(p -> 
            product.setDetailSections(p.getDetailSections()));
        
        // Validate that category is a leaf category (has no subcategories) if category is being changed
        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategoryId())) {
            if (!categoryService.isLeafCategory(request.getCategoryId())) {
                throw new RuntimeException("Products can only be added to leaf categories (categories without subcategories). Please select a subcategory instead.");
            }
        }
        
        // Clear existing collections
        product.getImages().clear();
        product.getDetailSections().clear();
        product.getRecommendedFabricIds().clear();
        
        mapRequestToProduct(request, product);
        Product saved = productRepository.save(product);
        return toDtoWithDetails(saved);
    }
    
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
    
    private void mapRequestToProduct(ProductRequest request, Product product) {
        product.setName(request.getName());
        
        // Generate slug from name if not provided
        if (request.getName() != null) {
            String slug = generateSlug(request.getName());
            // Ensure slug is unique
            if (product.getId() != null) {
                // Update: check uniqueness excluding current product
                String uniqueSlug = slug;
                int counter = 1;
                while (productRepository.existsBySlug(uniqueSlug) && 
                       !productRepository.findBySlug(uniqueSlug).map(p -> p.getId().equals(product.getId())).orElse(false)) {
                    uniqueSlug = slug + "-" + counter;
                    counter++;
                }
                product.setSlug(uniqueSlug);
            } else {
                // Create: check uniqueness
                String uniqueSlug = slug;
                int counter = 1;
                while (productRepository.existsBySlug(uniqueSlug)) {
                    uniqueSlug = slug + "-" + counter;
                    counter++;
                }
                product.setSlug(uniqueSlug);
            }
        }
        
        product.setType(Product.ProductType.valueOf(request.getType().toUpperCase()));
        product.setCategoryId(request.getCategoryId());
        product.setDescription(request.getDescription());
        
        if (request.getStatus() != null) {
            product.setStatus(Product.Status.valueOf(request.getStatus().toUpperCase()));
        }
        
        product.setIsNew(request.getIsNew() != null ? request.getIsNew() : false);
        product.setIsSale(request.getIsSale() != null ? request.getIsSale() : false);
        
        // Parse pricing fields once to keep mappings consistent
        BigDecimal parsedOriginalPrice = parseBigDecimal(request.getOriginalPrice());
        BigDecimal parsedPrice = parseBigDecimal(request.getPrice());
        BigDecimal parsedPricePerMeter = parseBigDecimal(request.getPricePerMeter());

        product.setOriginalPrice(parsedOriginalPrice);

        // Prefer explicit price; for PLAIN allow pricePerMeter; final fallback to originalPrice
        if (parsedPrice != null) {
            product.setPrice(parsedPrice);
        } else if (parsedPricePerMeter != null && product.getType() == Product.ProductType.PLAIN) {
            product.setPrice(parsedPricePerMeter);
        } else if (parsedOriginalPrice != null) {
            product.setPrice(parsedOriginalPrice);
        }
        
        // Type-specific fields
        switch (product.getType()) {
            case DESIGNED:
                // Parse designPrice - handle string inputs and null values
                product.setDesignPrice(parseBigDecimal(request.getDesignPrice()));
                product.setDesignId(request.getDesignId());
                if (request.getRecommendedFabricIds() != null) {
                    product.setRecommendedFabricIds(new ArrayList<>(request.getRecommendedFabricIds()));
                }
                break;
            case PLAIN:
                product.setPlainProductId(request.getPlainProductId());
                break;
            case DIGITAL:
                product.setFileUrl(request.getFileUrl());
                break;
        }

        // Final safeguard: if price is still null but we have an originalPrice, use it
        if (product.getPrice() == null && parsedOriginalPrice != null) {
            product.setPrice(parsedOriginalPrice);
        }
        
        // Media (images and videos) - prefer media over images
        if (request.getMedia() != null && !request.getMedia().isEmpty()) {
            for (ProductRequest.MediaRequest mediaReq : request.getMedia()) {
                ProductImage media = new ProductImage();
                media.setImageUrl(mediaReq.getUrl());
                media.setDisplayOrder(mediaReq.getDisplayOrder() != null ? mediaReq.getDisplayOrder() : 0);
                // Set media type
                if ("video".equalsIgnoreCase(mediaReq.getType())) {
                    media.setMediaType(ProductImage.MediaType.VIDEO);
                } else {
                    media.setMediaType(ProductImage.MediaType.IMAGE);
                }
                product.addImage(media);
            }
        } else if (request.getImages() != null) {
            // Fallback to old images format for backward compatibility
            int order = 0;
            for (String imageUrl : request.getImages()) {
                ProductImage image = new ProductImage();
                image.setImageUrl(imageUrl);
                image.setDisplayOrder(order++);
                image.setMediaType(ProductImage.MediaType.IMAGE);
                product.addImage(image);
            }
        }
        
        // Detail sections - preserve user input exactly, only remove HTML if present
        if (request.getDetailSections() != null) {
            int order = 0;
            for (ProductRequest.DetailSectionRequest sectionReq : request.getDetailSections()) {
                ProductDetailSection section = new ProductDetailSection();
                // Preserve user input - only sanitize if HTML is detected
                String title = sectionReq.getTitle();
                section.setTitle(title != null && title.contains("<") ? sanitizeHtml(title) : (title != null ? title.trim() : ""));
                
                // Preserve user input - only sanitize if HTML is detected
                String content = sectionReq.getContent();
                section.setContent(content != null && content.contains("<") ? sanitizeHtml(content) : (content != null ? content.trim() : ""));
                
                section.setDisplayOrder(sectionReq.getDisplayOrder() != null ? sectionReq.getDisplayOrder() : order++);
                product.addDetailSection(section);
            }
        }
    }
    
    private ProductDto toDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSlug(product.getSlug());
        dto.setType(product.getType().name());
        dto.setCategoryId(product.getCategoryId());
        dto.setDescription(product.getDescription());
        dto.setStatus(product.getStatus().name());
        dto.setIsNew(product.getIsNew());
        dto.setIsSale(product.getIsSale());
        dto.setOriginalPrice(product.getOriginalPrice());
        dto.setPrice(product.getPrice());
        
        // Get category name
        if (product.getCategoryId() != null) {
            categoryRepository.findById(product.getCategoryId())
                    .ifPresent(cat -> dto.setCategoryName(cat.getName()));
        }
        
        // Images (for backward compatibility)
        if (product.getImages() != null) {
            dto.setImages(product.getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList()));
        }
        
        // Media with type information
        if (product.getImages() != null) {
            List<ProductDto.MediaDto> mediaList = product.getImages().stream()
                    .map(img -> {
                        ProductDto.MediaDto mediaDto = new ProductDto.MediaDto();
                        mediaDto.setUrl(img.getImageUrl());
                        mediaDto.setType(img.getMediaType() != null && img.getMediaType() == ProductImage.MediaType.VIDEO ? "video" : "image");
                        mediaDto.setDisplayOrder(img.getDisplayOrder());
                        return mediaDto;
                    })
                    .collect(Collectors.toList());
            dto.setMedia(mediaList);
        }
        
        // Type-specific fields
        switch (product.getType()) {
            case DESIGNED:
                dto.setDesignPrice(product.getDesignPrice());
                dto.setDesignId(product.getDesignId());
                dto.setRecommendedFabricIds(product.getRecommendedFabricIds());
                break;
            case PLAIN:
                dto.setPlainProductId(product.getPlainProductId());
                // For PLAIN products, surface the selling price in both price and pricePerMeter
                dto.setPrice(product.getPrice());
                dto.setPricePerMeter(product.getPrice());
                break;
            case DIGITAL:
                dto.setPrice(product.getPrice());
                dto.setFileUrl(product.getFileUrl());
                break;
        }
        
        return dto;
    }
    
    private ProductDto toDtoWithDetails(Product product) {
        ProductDto dto = toDto(product);
        
        // Detail sections
        if (product.getDetailSections() != null) {
            dto.setDetailSections(product.getDetailSections().stream()
                    .map(this::toDetailSectionDto)
                    .collect(Collectors.toList()));
        }
        
        // Load related data for DESIGNED products
        if (product.getType() == Product.ProductType.DESIGNED && 
            product.getRecommendedFabricIds() != null && 
            !product.getRecommendedFabricIds().isEmpty()) {
            List<PlainProductDto> fabrics = plainProductService.getPlainProductsByIds(product.getRecommendedFabricIds());
            dto.setRecommendedFabrics(fabrics);
        }
        
        // Load related data for PLAIN products
        if (product.getType() == Product.ProductType.PLAIN && product.getPlainProductId() != null) {
            PlainProductDto plainProduct = plainProductService.getPlainProductById(product.getPlainProductId());
            dto.setPlainProduct(plainProduct);
        }
        
        return dto;
    }
    
    private ProductDto.DetailSectionDto toDetailSectionDto(ProductDetailSection section) {
        ProductDto.DetailSectionDto dto = new ProductDto.DetailSectionDto();
        dto.setId(section.getId());
        dto.setTitle(section.getTitle());
        dto.setContent(section.getContent());
        dto.setDisplayOrder(section.getDisplayOrder());
        return dto;
    }
    
    private String generateSlug(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }
    
    /**
     * Safely parses BigDecimal from various input types (BigDecimal, String, Number, null).
     * Handles string concatenation issues and invalid inputs.
     * Fixes issues like "099" -> 99, "0199" -> 199, etc.
     */
    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        
        // If already a BigDecimal, return as is
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        
        // If it's a Number, convert to BigDecimal
        if (value instanceof Number) {
            double numValue = ((Number) value).doubleValue();
            // Return null for NaN or infinite values
            if (Double.isNaN(numValue) || Double.isInfinite(numValue)) {
                return null;
            }
            return BigDecimal.valueOf(numValue);
        }
        
        // If it's a String, parse it
        if (value instanceof String) {
            String strValue = ((String) value).trim();
            
            // Handle empty strings
            if (strValue.isEmpty()) {
                return null;
            }
            
            // Remove any non-numeric characters except decimal point and minus sign
            // This handles cases like "099" -> "99", "0199" -> "199", "₹99" -> "99"
            String cleaned = strValue.replaceAll("[^0-9.-]", "");
            
            // Handle empty after cleaning
            if (cleaned.isEmpty()) {
                return null;
            }
            
            // Remove leading zeros (but keep decimal point and handle negative numbers)
            // Examples: "099" -> "99", "0199" -> "199", "0.99" -> "0.99", "-099" -> "-99"
            if (cleaned.startsWith("-")) {
                // Handle negative numbers
                String withoutMinus = cleaned.substring(1);
                if (withoutMinus.matches("^0+[0-9]") && !withoutMinus.startsWith("0.")) {
                    withoutMinus = withoutMinus.replaceFirst("^0+", "");
                }
                cleaned = "-" + withoutMinus;
            } else if (cleaned.matches("^0+[0-9]") && !cleaned.startsWith("0.")) {
                // Handle positive numbers with leading zeros (but not "0.99")
                cleaned = cleaned.replaceFirst("^0+", "");
            }
            
            try {
                // Parse the cleaned string
                BigDecimal parsed = new BigDecimal(cleaned);
                // Allow zero values (return BigDecimal.ZERO instead of null)
                return parsed;
            } catch (NumberFormatException e) {
                // If parsing fails, return null
                return null;
            }
        }
        
        // For any other type, try to convert via string
        try {
            String strValue = value.toString().trim();
            if (strValue.isEmpty()) {
                return null;
            }
            return new BigDecimal(strValue);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Sanitizes HTML content by removing HTML tags but preserving user-entered text.
     * Only removes HTML tags, preserves the actual text content exactly as user entered.
     */
    private String sanitizeHtml(String html) {
        if (html == null) {
            return null;
        }
        
        // If the string doesn't contain HTML tags, return as-is (preserve user input exactly)
        if (!html.contains("<") && !html.contains(">")) {
            return html.trim();
        }
        
        // Only remove HTML tags, but preserve all text content and line breaks
        // Convert HTML entities to their text equivalents
        String cleaned = html
                .replaceAll("<[^>]+>", "") // Remove all HTML tags
                .replaceAll("&nbsp;", " ") // Replace &nbsp; with space
                .replaceAll("&amp;", "&") // Replace &amp; with &
                .replaceAll("&lt;", "<") // Replace &lt; with <
                .replaceAll("&gt;", ">") // Replace &gt; with >
                .replaceAll("&quot;", "\"") // Replace &quot; with "
                .replaceAll("&#39;", "'") // Replace &#39; with '
                .replaceAll("&apos;", "'") // Replace &apos; with '
                .replaceAll("&nbsp;", " ") // Replace &nbsp; with space
                .trim();
        
        // Return cleaned text - preserve user's actual input, just remove HTML structure
        return cleaned;
    }
}
