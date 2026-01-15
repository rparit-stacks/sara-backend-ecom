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
    
    @Autowired
    private com.sara.ecom.repository.DesignRepository designRepository;
    
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
        
        List<ProductDto> result = products.stream().map(this::toDto).collect(Collectors.toList());
        
        // If type is PLAIN and no categoryId specified, filter by fabric categories only
        // This ensures only fabric products appear in fabric selection
        if (typeEnum == Product.ProductType.PLAIN && categoryId == null) {
            // Get all fabric category IDs (use ACTIVE status for categories)
            List<Category> fabricCategories = categoryRepository.findByIsFabricTrueAndStatus(Category.Status.ACTIVE);
            List<Long> fabricCategoryIds = fabricCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());
            
            // Filter products to only include those in fabric categories
            if (!fabricCategoryIds.isEmpty()) {
                result = result.stream()
                    .filter(p -> p.getCategoryId() != null && fabricCategoryIds.contains(p.getCategoryId()))
                    .collect(Collectors.toList());
            } else {
                // If no fabric categories exist, return empty list
                result = new ArrayList<>();
            }
        }
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        // Use separate queries to avoid MultipleBagFetchException
        Product product = productRepository.findByIdWithImages(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        // Load detail sections separately
        productRepository.findByIdWithDetailSections(id).ifPresent(p -> 
            product.setDetailSections(p.getDetailSections()));

        // Load custom fields separately
        productRepository.findByIdWithCustomFields(id).ifPresent(p ->
            product.setCustomFields(p.getCustomFields()));

        // Load variants separately
        productRepository.findByIdWithVariants(id).ifPresent(p ->
            product.setVariants(p.getVariants()));
        
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
        productRepository.findByIdWithCustomFields(product.getId()).ifPresent(p ->
            product.setCustomFields(p.getCustomFields()));
        productRepository.findByIdWithVariants(product.getId()).ifPresent(p ->
            product.setVariants(p.getVariants()));
        
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

        // Load custom fields separately
        productRepository.findByIdWithCustomFields(id).ifPresent(p ->
            product.setCustomFields(p.getCustomFields()));

        // Load variants separately
        productRepository.findByIdWithVariants(id).ifPresent(p ->
            product.setVariants(p.getVariants()));
        
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
        product.getCustomFields().clear();
        product.getVariants().clear();
        product.getCombinations().clear();
        
        mapRequestToProduct(request, product);
        Product saved = productRepository.save(product);
        return toDtoWithDetails(saved);
    }
    
    /**
     * Creates a Digital Product from a Design Product.
     * This allows users to purchase just the design file without the physical product.
     */
    @Transactional
    public ProductDto createDigitalProductFromDesign(Long designProductId, BigDecimal digitalPrice) {
        // Get the Design Product
        Product designProduct = productRepository.findByIdWithImages(designProductId)
                .orElseThrow(() -> new RuntimeException("Design Product not found with id: " + designProductId));
        
        if (designProduct.getType() != Product.ProductType.DESIGNED) {
            throw new RuntimeException("Product is not a Design Product");
        }
        
        // Get the Design entity if designId exists, otherwise use product images
        Design design = null;
        String designFileUrl = null;
        
        if (designProduct.getDesignId() != null) {
            design = designRepository.findById(designProduct.getDesignId())
                    .orElse(null);
            if (design != null && design.getImage() != null) {
                designFileUrl = design.getImage();
            }
        }
        
        // If no design file from Design entity, use first product image
        if (designFileUrl == null && designProduct.getImages() != null && !designProduct.getImages().isEmpty()) {
            designFileUrl = designProduct.getImages().get(0).getImageUrl();
        }
        
        // If still no file, throw error
        if (designFileUrl == null) {
            throw new RuntimeException("Design Product does not have a design file or image available for digital download");
        }
        
        // Check if Digital Product already exists for this Design Product
        Product existingDigital = productRepository.findBySourceDesignProductId(designProductId)
                .orElse(null);
        
        if (existingDigital != null) {
            // Update price if different
            if (digitalPrice != null && !digitalPrice.equals(existingDigital.getPrice())) {
                existingDigital.setPrice(digitalPrice);
                existingDigital = productRepository.save(existingDigital);
            }
            return toDtoWithDetails(existingDigital);
        }
        
        // Create new Digital Product
        Product digitalProduct = new Product();
        digitalProduct.setName(designProduct.getName() + " (Digital Design)");
        digitalProduct.setType(Product.ProductType.DIGITAL);
        digitalProduct.setCategoryId(designProduct.getCategoryId()); // Same category
        digitalProduct.setDescription(designProduct.getDescription() != null ? 
            designProduct.getDescription() + " - Digital download only." : 
            "Digital download of this design.");
        digitalProduct.setStatus(Product.Status.ACTIVE);
        digitalProduct.setPrice(digitalPrice != null ? digitalPrice : 
            (designProduct.getDesignPrice() != null ? designProduct.getDesignPrice() : BigDecimal.ZERO));
        digitalProduct.setFileUrl(designFileUrl); // Use design image or product image as downloadable file
        digitalProduct.setSourceDesignProductId(designProductId); // Link to source Design Product
        
        // Copy images from design product
        if (designProduct.getImages() != null && !designProduct.getImages().isEmpty()) {
            for (ProductImage img : designProduct.getImages()) {
                ProductImage newImg = new ProductImage();
                newImg.setImageUrl(img.getImageUrl());
                newImg.setMediaType(img.getMediaType());
                newImg.setDisplayOrder(img.getDisplayOrder());
                digitalProduct.addImage(newImg);
            }
        } else if (designFileUrl != null) {
            // Use design file as product image
            ProductImage img = new ProductImage();
            img.setImageUrl(designFileUrl);
            img.setDisplayOrder(0);
            digitalProduct.addImage(img);
        }
        
        // Generate unique slug
        String baseSlug = generateSlug(digitalProduct.getName());
        String slug = baseSlug;
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        digitalProduct.setSlug(slug);
        
        Product saved = productRepository.save(digitalProduct);
        return toDtoWithDetails(saved);
    }
    
    /**
     * Gets the Digital Product associated with a Design Product (if exists).
     */
    public ProductDto getDigitalProductFromDesign(Long designProductId) {
        Product digitalProduct = productRepository.findBySourceDesignProductId(designProductId)
                .orElse(null);
        if (digitalProduct == null) {
            return null;
        }
        return toDtoWithDetails(digitalProduct);
    }
    
    private String generateSlug(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
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
        
        // Set GST rate
        if (request.getGstRate() != null) {
            product.setGstRate(parseBigDecimal(request.getGstRate()));
        }
        
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

        // Custom Fields
        if (request.getCustomFields() != null) {
            product.getCustomFields().clear();
            for (ProductRequest.CustomFieldRequest fieldReq : request.getCustomFields()) {
                ProductCustomField field = new ProductCustomField();
                field.setLabel(fieldReq.getLabel());
                field.setFieldType(fieldReq.getFieldType());
                field.setPlaceholder(fieldReq.getPlaceholder());
                field.setRequired(fieldReq.isRequired());
                product.addCustomField(field);
            }
        }

        // Variants
        if (request.getVariants() != null) {
            product.getVariants().clear();
            for (ProductRequest.VariantRequest variantReq : request.getVariants()) {
                ProductVariant variant = new ProductVariant();
                variant.setName(variantReq.getName());
                variant.setType(variantReq.getType());
                variant.setUnit(variantReq.getUnit());
                
                if (variantReq.getOptions() != null) {
                    for (ProductRequest.VariantOptionRequest optionReq : variantReq.getOptions()) {
                        ProductVariantOption option = new ProductVariantOption();
                        option.setValue(optionReq.getValue());
                        option.setPriceModifier(optionReq.getPriceModifier());
                        variant.addOption(option);
                    }
                }
                product.addVariant(variant);
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
        dto.setGstRate(product.getGstRate());
        
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
                dto.setSourceDesignProductId(product.getSourceDesignProductId());
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

        // Custom Fields
        if (product.getCustomFields() != null) {
            dto.setCustomFields(product.getCustomFields().stream()
                    .map(field -> {
                        ProductDto.CustomFieldDto fieldDto = new ProductDto.CustomFieldDto();
                        fieldDto.setId(field.getId());
                        fieldDto.setLabel(field.getLabel());
                        fieldDto.setFieldType(field.getFieldType());
                        fieldDto.setPlaceholder(field.getPlaceholder());
                        fieldDto.setRequired(field.isRequired());
                        return fieldDto;
                    })
                    .collect(Collectors.toList()));
        }

        // Variants
        if (product.getVariants() != null) {
            dto.setVariants(product.getVariants().stream()
                    .map(variant -> {
                        ProductDto.VariantDto variantDto = new ProductDto.VariantDto();
                        variantDto.setId(variant.getId());
                        variantDto.setName(variant.getName());
                        variantDto.setType(variant.getType());
                        variantDto.setUnit(variant.getUnit());
                        
                        if (variant.getOptions() != null) {
                            variantDto.setOptions(variant.getOptions().stream()
                                    .map(option -> {
                                        ProductDto.VariantOptionDto optionDto = new ProductDto.VariantOptionDto();
                                        optionDto.setId(option.getId());
                                        optionDto.setValue(option.getValue());
                                        optionDto.setPriceModifier(option.getPriceModifier());
                                        return optionDto;
                                    })
                                    .collect(Collectors.toList()));
                        }
                        return variantDto;
                    })
                    .collect(Collectors.toList()));
        }
        
        // Load related data for DESIGNED products
        // recommendedFabricIds now points to Product IDs (type=PLAIN), not PlainProduct IDs
        if (product.getType() == Product.ProductType.DESIGNED && 
            product.getRecommendedFabricIds() != null && 
            !product.getRecommendedFabricIds().isEmpty()) {
            // Fetch products where type=PLAIN and IDs match recommendedFabricIds
            List<Product> fabricProducts = productRepository.findByIdIn(product.getRecommendedFabricIds())
                .stream()
                .filter(p -> p.getType() == Product.ProductType.PLAIN)
                .collect(Collectors.toList());
            
            // Convert Product entities to PlainProductDto format for frontend compatibility
            List<PlainProductDto> fabrics = fabricProducts.stream()
                .map(p -> {
                    PlainProductDto fabricDto = new PlainProductDto();
                    fabricDto.setId(p.getId());
                    fabricDto.setName(p.getName());
                    fabricDto.setDescription(p.getDescription());
                    fabricDto.setImage(p.getImages() != null && !p.getImages().isEmpty() 
                        ? p.getImages().get(0).getImageUrl() : null);
                    fabricDto.setPricePerMeter(p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
                    fabricDto.setCategoryId(p.getCategoryId());
                    fabricDto.setStatus(p.getStatus().name());
                    return fabricDto;
                })
                .collect(Collectors.toList());
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
