package com.sara.ecom.service;

import com.sara.ecom.dto.PlainProductDto;
import com.sara.ecom.dto.ProductDto;
import com.sara.ecom.dto.ProductRequest;
import com.sara.ecom.dto.CustomConfigDto;
import com.sara.ecom.entity.*;
import com.sara.ecom.repository.CategoryRepository;
import com.sara.ecom.repository.ProductRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    
    @Autowired
    private CustomConfigService customConfigService;
    
    @Autowired
    private CloudinaryService cloudinaryService;
    
    /**
     * Get all products matching the given filters.
     * Note: This method only returns Product entities, never CustomProduct entities.
     * CustomProducts are user-specific and are accessed via CustomProductService.
     * This ensures CustomProducts never appear in public product listings.
     */
    public List<ProductDto> getAllProducts(String status, String type, Long categoryId) {
        return getAllProducts(status, type, categoryId, null);
    }
    
    /**
     * Get all products matching the given filters, filtered by user email for category accessibility.
     */
    public List<ProductDto> getAllProducts(String status, String type, Long categoryId, String userEmail) {
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
        
        // Filter products by category accessibility (always filter, even if no userEmail - show only public)
        result = result.stream()
                .filter(p -> isProductAccessible(p.getId(), userEmail))
                .collect(Collectors.toList());
        
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
    
    /**
     * Checks if a product is accessible to a user based on its category's email restrictions.
     */
    public boolean isProductAccessible(Long productId, String userEmail) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return false;
        }
        
        if (product.getCategoryId() == null) {
            // Product without category is accessible
            return true;
        }
        
        // Check category accessibility
        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        return categoryService.isCategoryAccessible(category, userEmail);
    }
    
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        return getProductById(id, null);
    }
    
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id, String userEmail) {
        // Use separate queries to avoid MultipleBagFetchException
        Product product = productRepository.findByIdWithImages(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        // Check if product is accessible
        if (!isProductAccessible(id, userEmail)) {
            throw new RuntimeException("Product is not accessible");
        }
        
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
    
    /**
     * Creates a designed product from user-uploaded design with custom config.
     * This is a public endpoint that uses custom config from admin panel.
     */
    @Transactional
    public ProductDto createDesignedProductFromUpload(ProductRequest request) {
        // Get custom config from admin panel
        CustomConfigDto config = customConfigService.getPublicConfig();
        
        // Set product type
        request.setType("DESIGNED");
        
        // Use custom config values if not provided in request
        String baseName = config.getPageTitle() != null && !config.getPageTitle().trim().isEmpty() 
            ? config.getPageTitle() : "Custom Design";
        
        // Set clean display name (without unique suffix)
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            request.setName(baseName);
        }
        
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            request.setDescription(config.getPageDescription() != null && !config.getPageDescription().trim().isEmpty()
                ? config.getPageDescription() : "Your custom design");
        }
        
        // Set design price from config
        if (request.getDesignPrice() == null && config.getDesignPrice() != null) {
            request.setDesignPrice(config.getDesignPrice());
        }
        
        // Set status to ACTIVE by default for user-uploaded products
        if (request.getStatus() == null) {
            request.setStatus("ACTIVE");
        }
        
        // Ensure media/images are set if provided
        if (request.getMedia() == null || request.getMedia().isEmpty()) {
            // If images are provided, convert to media format
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                List<ProductRequest.MediaRequest> mediaList = new ArrayList<>();
                int order = 0;
                for (String imageUrl : request.getImages()) {
                    ProductRequest.MediaRequest media = new ProductRequest.MediaRequest();
                    media.setUrl(imageUrl);
                    media.setType("image");
                    media.setDisplayOrder(order++);
                    mediaList.add(media);
                }
                request.setMedia(mediaList);
            }
        }
        
        // Create the product
        Product product = new Product();
        mapRequestToProduct(request, product);
        
        // Inherit everything from config - Single Source of Truth
        inheritFromConfig(product, config);
        
        // Generate unique slug with timestamp to ensure uniqueness
        String baseSlug = generateSlug(product.getName());
        // Add timestamp to make slug unique
        String uniqueSlug = baseSlug + "-" + System.currentTimeMillis();
        
        // Double check uniqueness (in case of race condition)
        String finalSlug = uniqueSlug;
        int counter = 1;
        while (productRepository.existsBySlug(finalSlug)) {
            finalSlug = uniqueSlug + "-" + counter;
            counter++;
        }
        product.setSlug(finalSlug);
        
        Product saved = productRepository.save(product);
        return toDtoWithDetails(saved);
    }
    
    /**
     * Inherits all business logic from CustomProductConfig to Product.
     * This ensures config is the single source of truth.
     */
    private void inheritFromConfig(Product product, CustomConfigDto config) {
        // GST and HSN from config
        if (config.getGstRate() != null) {
            product.setGstRate(config.getGstRate());
        }
        if (config.getHsnCode() != null && !config.getHsnCode().trim().isEmpty()) {
            product.setHsnCode(config.getHsnCode());
        }
        
        // Recommended fabrics from config
        if (config.getRecommendedFabricIds() != null && !config.getRecommendedFabricIds().isEmpty()) {
            product.setRecommendedFabricIds(new ArrayList<>(config.getRecommendedFabricIds()));
        }
        
        // Copy variants from config to product
        if (config.getVariants() != null && !config.getVariants().isEmpty()) {
            product.getVariants().clear();
            for (CustomConfigDto.VariantDto configVariant : config.getVariants()) {
                ProductVariant productVariant = new ProductVariant();
                productVariant.setType(configVariant.getType());
                productVariant.setName(configVariant.getName());
                productVariant.setUnit(configVariant.getUnit());
                productVariant.setFrontendId(configVariant.getFrontendId());
                product.addVariant(productVariant);
                
                // Copy options
                if (configVariant.getOptions() != null) {
                    for (CustomConfigDto.VariantOptionDto configOption : configVariant.getOptions()) {
                        ProductVariantOption productOption = new ProductVariantOption();
                        productOption.setValue(configOption.getValue());
                        productOption.setFrontendId(configOption.getFrontendId());
                        productOption.setPriceModifier(configOption.getPriceModifier() != null ? 
                            configOption.getPriceModifier() : BigDecimal.ZERO);
                        productVariant.addOption(productOption);
                    }
                }
            }
        }
        
        // Copy pricing slabs from config to product
        if (config.getPricingSlabs() != null && !config.getPricingSlabs().isEmpty()) {
            product.getPricingSlabs().clear();
            for (CustomConfigDto.PricingSlabDto configSlab : config.getPricingSlabs()) {
                ProductPricingSlab productSlab = new ProductPricingSlab();
                productSlab.setMinQuantity(configSlab.getMinQuantity());
                productSlab.setMaxQuantity(configSlab.getMaxQuantity());
                productSlab.setDisplayOrder(configSlab.getDisplayOrder() != null ? configSlab.getDisplayOrder() : 0);
                
                // Set discount type and value
                if (configSlab.getDiscountType() != null && !configSlab.getDiscountType().isEmpty()) {
                    try {
                        ProductPricingSlab.DiscountType discountType = 
                            ProductPricingSlab.DiscountType.valueOf(configSlab.getDiscountType().toUpperCase());
                        productSlab.setDiscountType(discountType);
                        productSlab.setDiscountValue(configSlab.getDiscountValue() != null ? 
                            configSlab.getDiscountValue() : BigDecimal.ZERO);
                        productSlab.setPricePerMeter(BigDecimal.ZERO);
                    } catch (IllegalArgumentException e) {
                        productSlab.setDiscountType(ProductPricingSlab.DiscountType.FIXED_AMOUNT);
                        productSlab.setDiscountValue(BigDecimal.ZERO);
                        productSlab.setPricePerMeter(BigDecimal.ZERO);
                    }
                } else {
                    productSlab.setDiscountType(ProductPricingSlab.DiscountType.FIXED_AMOUNT);
                    productSlab.setDiscountValue(BigDecimal.ZERO);
                    productSlab.setPricePerMeter(BigDecimal.ZERO);
                }
                
                productSlab.setProduct(product);
                product.getPricingSlabs().add(productSlab);
            }
        }
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
        
        // Collect ALL design images/files for download
        List<String> allDesignFileUrls = new ArrayList<>();
        
        // Add design file if exists
        if (designFileUrl != null) {
            allDesignFileUrls.add(designFileUrl);
        }
        
        // Add all product images
        if (designProduct.getImages() != null && !designProduct.getImages().isEmpty()) {
            for (ProductImage img : designProduct.getImages()) {
                String imgUrl = img.getImageUrl();
                if (imgUrl != null && !allDesignFileUrls.contains(imgUrl)) {
                    allDesignFileUrls.add(imgUrl);
                }
            }
        }
        
        // If still no files, throw error
        if (allDesignFileUrls.isEmpty()) {
            throw new RuntimeException("Design Product does not have any design files or images available for digital download");
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
            designProduct.getDescription() + " - Digital download only. All design images included." : 
            "Digital download of this design. All design images included.");
        digitalProduct.setStatus(Product.Status.ACTIVE);
        BigDecimal finalPrice = digitalPrice != null ? digitalPrice : 
            (designProduct.getDesignPrice() != null ? designProduct.getDesignPrice() : BigDecimal.ZERO);
        digitalProduct.setPrice(finalPrice);
        digitalProduct.setOriginalPrice(finalPrice); // Set originalPrice to match price
        
        // Store all file URLs as JSON array in fileUrl field
        // Use TEXT column to avoid VARCHAR(255) length limit
        // Format: ["url1", "url2", "url3"] or single URL if only one file
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String fileUrlJson = objectMapper.writeValueAsString(allDesignFileUrls);
            // Ensure it fits in database - if too long, use first URL only
            if (fileUrlJson.length() > 10000) {
                // If JSON is too long, just use the first URL
                digitalProduct.setFileUrl(allDesignFileUrls.get(0));
            } else {
                digitalProduct.setFileUrl(fileUrlJson);
            }
        } catch (Exception e) {
            // Fallback: use first URL only if JSON serialization fails
            digitalProduct.setFileUrl(allDesignFileUrls.get(0));
        }
        
        digitalProduct.setSourceDesignProductId(designProductId); // Link to source Design Product
        
        // Copy images from design product
        if (designProduct.getImages() != null && !designProduct.getImages().isEmpty()) {
            for (ProductImage img : designProduct.getImages()) {
                // Only add image if imageUrl is not null (required field)
                if (img.getImageUrl() != null && !img.getImageUrl().trim().isEmpty()) {
                ProductImage newImg = new ProductImage();
                newImg.setImageUrl(img.getImageUrl());
                    newImg.setMediaType(img.getMediaType() != null ? img.getMediaType() : ProductImage.MediaType.IMAGE);
                    newImg.setDisplayOrder(img.getDisplayOrder() != null ? img.getDisplayOrder() : 0);
                digitalProduct.addImage(newImg);
            }
            }
        } else if (designFileUrl != null && !designFileUrl.trim().isEmpty()) {
            // Use design file as product image
            ProductImage img = new ProductImage();
            img.setImageUrl(designFileUrl);
            img.setDisplayOrder(0);
            img.setMediaType(ProductImage.MediaType.IMAGE);
            digitalProduct.addImage(img);
        }
        
        // If no images were added, add at least one image from fileUrl if available
        if (digitalProduct.getImages().isEmpty() && !allDesignFileUrls.isEmpty()) {
            String firstFileUrl = allDesignFileUrls.get(0);
            if (firstFileUrl != null && !firstFileUrl.trim().isEmpty()) {
                ProductImage img = new ProductImage();
                img.setImageUrl(firstFileUrl);
                img.setDisplayOrder(0);
                img.setMediaType(ProductImage.MediaType.IMAGE);
                digitalProduct.addImage(img);
            }
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
    
    /**
     * Bulk delete products by IDs.
     */
    @Transactional
    public void bulkDeleteProducts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("Product IDs list cannot be empty");
        }
        List<Product> products = productRepository.findAllById(ids);
        if (products.size() != ids.size()) {
            throw new RuntimeException("Some products were not found");
        }
        productRepository.deleteAll(products);
    }
    
    /**
     * Toggle product status (pause/unpause).
     * ACTIVE -> INACTIVE (pause)
     * INACTIVE -> ACTIVE (unpause)
     */
    @Transactional
    public ProductDto toggleProductStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (product.getStatus() == Product.Status.ACTIVE) {
            product.setStatus(Product.Status.INACTIVE);
        } else {
            product.setStatus(Product.Status.ACTIVE);
        }
        
        product = productRepository.save(product);
        return toDtoWithDetails(product);
    }
    
    /**
     * Bulk toggle product status.
     */
    @Transactional
    public void bulkToggleProductStatus(List<Long> ids, Product.Status targetStatus) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("Product IDs list cannot be empty");
        }
        List<Product> products = productRepository.findAllById(ids);
        if (products.size() != ids.size()) {
            throw new RuntimeException("Some products were not found");
        }
        products.forEach(product -> product.setStatus(targetStatus));
        productRepository.saveAll(products);
    }
    
    /**
     * Gets all products for Excel export.
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProductsForExport() {
        List<Product> products = productRepository.findAllWithImages();
        return products.stream().map(this::toDtoWithDetails).collect(Collectors.toList());
    }
    
    /**
     * Exports all products to Excel file.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> exportProductsToExcel() throws IOException {
        List<ProductDto> products = getAllProductsForExport();
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");
        
        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Name", "Slug", "Type", "Status", "Category", "Price", "Design Price", 
                           "Original Price", "Is New", "Is Sale", "Description", "Created At"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Create data rows
        int rowNum = 1;
        for (ProductDto product : products) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(product.getId() != null ? product.getId() : 0);
            row.createCell(1).setCellValue(product.getName() != null ? product.getName() : "");
            row.createCell(2).setCellValue(product.getSlug() != null ? product.getSlug() : "");
            row.createCell(3).setCellValue(product.getType() != null ? product.getType() : "");
            row.createCell(4).setCellValue(product.getStatus() != null ? product.getStatus() : "");
            
            // Category name
            String categoryName = "";
            if (product.getCategoryId() != null) {
                try {
                    Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
                    if (category != null) {
                        categoryName = category.getName();
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
            row.createCell(5).setCellValue(categoryName);
            
            // Prices
            row.createCell(6).setCellValue(product.getPrice() != null ? product.getPrice().doubleValue() : 0);
            row.createCell(7).setCellValue(product.getDesignPrice() != null ? product.getDesignPrice().doubleValue() : 0);
            row.createCell(8).setCellValue(product.getOriginalPrice() != null ? product.getOriginalPrice().doubleValue() : 0);
            
            row.createCell(9).setCellValue(product.getIsNew() != null && product.getIsNew() ? "Yes" : "No");
            row.createCell(10).setCellValue(product.getIsSale() != null && product.getIsSale() ? "Yes" : "No");
            
            // Description (truncate if too long)
            String description = product.getDescription() != null ? product.getDescription() : "";
            if (description.length() > 100) {
                description = description.substring(0, 100) + "...";
            }
            row.createCell(11).setCellValue(description);
            
            // Created at (if available in DTO, otherwise empty)
            row.createCell(12).setCellValue("");
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // Set minimum width
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }
        
        // Write to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        byte[] excelBytes = outputStream.toByteArray();
        ByteArrayResource resource = new ByteArrayResource(excelBytes);
        
        // Generate filename with timestamp
        String filename = "products_export_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(excelBytes.length)
                .body(resource);
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
        
        // Set HSN code
        if (request.getHsnCode() != null) {
            product.setHsnCode(request.getHsnCode());
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
                // Handle pricing slabs
                if (request.getPricingSlabs() != null && !request.getPricingSlabs().isEmpty()) {
                    // Clear existing slabs
                    product.getPricingSlabs().clear();
                    // Add new slabs
                    int order = 0;
                    for (ProductRequest.PricingSlabRequest slabRequest : request.getPricingSlabs()) {
                        com.sara.ecom.entity.ProductPricingSlab slab = new com.sara.ecom.entity.ProductPricingSlab();
                        slab.setProduct(product);
                        slab.setMinQuantity(slabRequest.getMinQuantity());
                        slab.setMaxQuantity(slabRequest.getMaxQuantity());
                        
                        // Set discount type and value
                        if (slabRequest.getDiscountType() != null && !slabRequest.getDiscountType().isEmpty()) {
                            try {
                                com.sara.ecom.entity.ProductPricingSlab.DiscountType discountType = 
                                    com.sara.ecom.entity.ProductPricingSlab.DiscountType.valueOf(slabRequest.getDiscountType().toUpperCase());
                                slab.setDiscountType(discountType);
                                BigDecimal discountValue = parseBigDecimal(slabRequest.getDiscountValue());
                                if (discountValue == null) {
                                    discountValue = BigDecimal.ZERO;
                                }
                                slab.setDiscountValue(discountValue);
                                // Set pricePerMeter to 0 for new discount-based system (database requires NOT NULL)
                                slab.setPricePerMeter(BigDecimal.ZERO);
                            } catch (IllegalArgumentException e) {
                                // Invalid discount type, use default
                                slab.setDiscountType(com.sara.ecom.entity.ProductPricingSlab.DiscountType.FIXED_AMOUNT);
                                slab.setDiscountValue(BigDecimal.ZERO);
                                slab.setPricePerMeter(BigDecimal.ZERO);
                            }
                        } else {
                            // Legacy support: if discountType not provided, use FIXED_AMOUNT with pricePerMeter
                            // This is for backward compatibility - legacy system used absolute prices
                            slab.setDiscountType(com.sara.ecom.entity.ProductPricingSlab.DiscountType.FIXED_AMOUNT);
                            BigDecimal pricePerMeter = parseBigDecimal(slabRequest.getPricePerMeter());
                            if (pricePerMeter != null && pricePerMeter.compareTo(BigDecimal.ZERO) > 0) {
                                // Legacy: pricePerMeter was absolute price, set discount to 0 for now
                                // Admin will need to reconfigure slabs with proper discount values
                                slab.setDiscountValue(BigDecimal.ZERO);
                                slab.setPricePerMeter(pricePerMeter);
                            } else {
                                slab.setDiscountValue(BigDecimal.ZERO);
                                // Database requires NOT NULL, so set to 0
                                slab.setPricePerMeter(BigDecimal.ZERO);
                            }
                        }
                        
                        slab.setDisplayOrder(slabRequest.getDisplayOrder() != null ? slabRequest.getDisplayOrder() : order);
                        product.getPricingSlabs().add(slab);
                        order++;
                    }
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
        dto.setHsnCode(product.getHsnCode());
        
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
                // Map pricing slabs
                if (product.getPricingSlabs() != null && !product.getPricingSlabs().isEmpty()) {
                    dto.setPricingSlabs(product.getPricingSlabs().stream()
                        .map(slab -> {
                            ProductDto.PricingSlabDto slabDto = new ProductDto.PricingSlabDto();
                            slabDto.setId(slab.getId());
                            slabDto.setMinQuantity(slab.getMinQuantity());
                            slabDto.setMaxQuantity(slab.getMaxQuantity());
                            // Set discount type and value
                            if (slab.getDiscountType() != null) {
                                slabDto.setDiscountType(slab.getDiscountType().name());
                                slabDto.setDiscountValue(slab.getDiscountValue());
                            }
                            // Legacy support
                            slabDto.setPricePerMeter(slab.getPricePerMeter());
                            slabDto.setDisplayOrder(slab.getDisplayOrder());
                            return slabDto;
                        })
                        .collect(Collectors.toList()));
                }
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
    
    /**
     * Generates password-protected ZIP from digital product files and uploads to Cloudinary.
     * Returns the Cloudinary URL of the uploaded ZIP.
     */
    @Transactional
    public String generatePasswordProtectedZip(Long productId, String password) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        if (product.getType() != Product.ProductType.DIGITAL) {
            throw new RuntimeException("Product is not a Digital Product");
        }
        
        List<String> fileUrls = new ArrayList<>();
        
        // Get all file URLs from fileUrl field
        String fileUrl = product.getFileUrl();
        if (fileUrl != null && !fileUrl.trim().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Object parsed = objectMapper.readValue(fileUrl, Object.class);
                if (parsed instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> urls = (List<String>) parsed;
                    for (String url : urls) {
                        if (url != null && !url.trim().isEmpty() && !fileUrls.contains(url)) {
                            fileUrls.add(url);
                        }
                    }
                } else if (parsed instanceof String) {
                    String url = (String) parsed;
                    if (!fileUrls.contains(url)) {
                        fileUrls.add(url);
                    }
                }
            } catch (Exception e) {
                if (fileUrl.contains(",")) {
                    String[] urls = fileUrl.split(",");
                    for (String url : urls) {
                        String trimmed = url.trim();
                        if (!trimmed.isEmpty() && !fileUrls.contains(trimmed)) {
                            fileUrls.add(trimmed);
                        }
                    }
                } else {
                    if (!fileUrls.contains(fileUrl)) {
                        fileUrls.add(fileUrl);
                    }
                }
            }
        }
        
        // If created from design product, add all images from source
        if (product.getSourceDesignProductId() != null) {
            Product sourceDesignProduct = productRepository.findByIdWithImages(product.getSourceDesignProductId())
                    .orElse(null);
            
            if (sourceDesignProduct != null && sourceDesignProduct.getImages() != null) {
                for (ProductImage img : sourceDesignProduct.getImages()) {
                    String imgUrl = img.getImageUrl();
                    if (imgUrl != null && !imgUrl.trim().isEmpty() && !fileUrls.contains(imgUrl)) {
                        fileUrls.add(imgUrl);
                    }
                }
            }
        }
        
        if (fileUrls.isEmpty()) {
            throw new RuntimeException("No valid file URLs found");
        }
        
        // Create temporary directory for ZIP creation
        java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"));
        java.io.File tempZipFile = java.io.File.createTempFile("digital_product_", ".zip", tempDir);
        
        try {
            // Use Zip4j to create password-protected ZIP
            net.lingala.zip4j.ZipFile zipFile = new net.lingala.zip4j.ZipFile(tempZipFile);
            net.lingala.zip4j.model.ZipParameters zipParameters = new net.lingala.zip4j.model.ZipParameters();
            zipParameters.setEncryptFiles(true);
            zipParameters.setEncryptionMethod(net.lingala.zip4j.model.enums.EncryptionMethod.ZIP_STANDARD);
            
            // Download and add each file to ZIP
            for (int i = 0; i < fileUrls.size(); i++) {
                String url = fileUrls.get(i);
                try {
                    URL fileUrlObj = URI.create(url).toURL();
                    HttpURLConnection connection = (HttpURLConnection) fileUrlObj.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(30000);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        continue;
                    }
                    
                    String extension = "";
                    if (url.contains(".")) {
                        String[] parts = url.split("\\.");
                        if (parts.length > 1) {
                            extension = "." + parts[parts.length - 1].split("\\?")[0];
                        }
                    }
                    if (extension.isEmpty()) {
                        extension = ".png";
                    }
                    
                    // Download file to temporary location
                    java.io.File tempFile = java.io.File.createTempFile("download_", extension, tempDir);
                    try (InputStream inputStream = connection.getInputStream();
                         java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    // Add file to ZIP with password protection
                    zipFile.addFile(tempFile, zipParameters);
                    
                    // Clean up temp file
                    tempFile.delete();
                } catch (Exception e) {
                    System.err.println("Failed to download file from URL: " + url + " - " + e.getMessage());
                }
            }
            
            // Set password for the ZIP
            zipFile.setPassword(password.toCharArray());
            
            // Close ZIP file before reading bytes
            zipFile.close();
            
            // Read ZIP file bytes
            byte[] zipBytes = java.nio.file.Files.readAllBytes(tempZipFile.toPath());
            
            if (zipBytes.length == 0) {
                throw new RuntimeException("Failed to create ZIP file");
            }
            
            // Upload ZIP to Cloudinary
            String zipFileName = product.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_files.zip";
            String cloudinaryUrl = cloudinaryService.uploadFile(zipBytes, zipFileName, "digital_product_zips");
            
            return cloudinaryUrl;
        } finally {
            // Clean up temp ZIP file
            if (tempZipFile.exists()) {
                tempZipFile.delete();
            }
        }
    }
    
    /**
     * Generates ZIP from digital product files and uploads to Cloudinary (without password).
     * Returns the Cloudinary URL of the uploaded ZIP.
     * @deprecated Use generatePasswordProtectedZip instead for digital products
     */
    @Transactional
    public String generateAndUploadDigitalZip(Long productId) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        if (product.getType() != Product.ProductType.DIGITAL) {
            throw new RuntimeException("Product is not a Digital Product");
        }
        
        List<String> fileUrls = new ArrayList<>();
        
        // Get all file URLs from fileUrl field
        String fileUrl = product.getFileUrl();
        if (fileUrl != null && !fileUrl.trim().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Object parsed = objectMapper.readValue(fileUrl, Object.class);
                if (parsed instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> urls = (List<String>) parsed;
                    for (String url : urls) {
                        if (url != null && !url.trim().isEmpty() && !fileUrls.contains(url)) {
                            fileUrls.add(url);
                        }
                    }
                } else if (parsed instanceof String) {
                    String url = (String) parsed;
                    if (!fileUrls.contains(url)) {
                        fileUrls.add(url);
                    }
                }
            } catch (Exception e) {
                if (fileUrl.contains(",")) {
                    String[] urls = fileUrl.split(",");
                    for (String url : urls) {
                        String trimmed = url.trim();
                        if (!trimmed.isEmpty() && !fileUrls.contains(trimmed)) {
                            fileUrls.add(trimmed);
                        }
                    }
                } else {
                    if (!fileUrls.contains(fileUrl)) {
                        fileUrls.add(fileUrl);
                    }
                }
            }
        }
        
        // If created from design product, add all images from source
        if (product.getSourceDesignProductId() != null) {
            Product sourceDesignProduct = productRepository.findByIdWithImages(product.getSourceDesignProductId())
                    .orElse(null);
            
            if (sourceDesignProduct != null && sourceDesignProduct.getImages() != null) {
                for (ProductImage img : sourceDesignProduct.getImages()) {
                    String imgUrl = img.getImageUrl();
                    if (imgUrl != null && !imgUrl.trim().isEmpty() && !fileUrls.contains(imgUrl)) {
                        fileUrls.add(imgUrl);
                    }
                }
            }
        }
        
        if (fileUrls.isEmpty()) {
            throw new RuntimeException("No valid file URLs found");
        }
        
        // Create ZIP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        
        for (int i = 0; i < fileUrls.size(); i++) {
            String url = fileUrls.get(i);
            try {
                URL fileUrlObj = URI.create(url).toURL();
                HttpURLConnection connection = (HttpURLConnection) fileUrlObj.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    continue;
                }
                
                String fileName = "file_" + (i + 1);
                String extension = "";
                if (url.contains(".")) {
                    String[] parts = url.split("\\.");
                    if (parts.length > 1) {
                        extension = "." + parts[parts.length - 1].split("\\?")[0];
                    }
                }
                if (extension.isEmpty()) {
                    extension = ".png";
                }
                fileName += extension;
                
                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                
                InputStream inputStream = connection.getInputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    zos.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                zos.closeEntry();
            } catch (Exception e) {
                System.err.println("Failed to download file from URL: " + url + " - " + e.getMessage());
            }
        }
        
        zos.close();
        byte[] zipBytes = baos.toByteArray();
        
        if (zipBytes.length == 0) {
            throw new RuntimeException("Failed to download any files");
        }
        
        // Upload ZIP to Cloudinary
        String zipFileName = product.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_files.zip";
        String cloudinaryUrl = cloudinaryService.uploadFile(zipBytes, zipFileName, "products/digital-downloads");
        
        return cloudinaryUrl;
    }
    
    /**
     * Downloads digital product files as a ZIP archive.
     * Fetches all files from Cloudinary URLs and bundles them into a ZIP.
     * For design products, includes ALL images from the source design product.
     * If orderItemId is provided and has digitalDownloadUrl, returns redirect to that URL.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadDigitalProductFiles(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        if (product.getType() != Product.ProductType.DIGITAL) {
            throw new RuntimeException("Product is not a Digital Product");
        }
        
        List<String> fileUrls = new ArrayList<>();
        
        // First, try to get URLs from fileUrl field
        String fileUrl = product.getFileUrl();
        if (fileUrl != null && !fileUrl.trim().isEmpty()) {
            // Parse fileUrl - can be single URL, JSON array, or comma-separated
            try {
                // Try parsing as JSON array first
                ObjectMapper objectMapper = new ObjectMapper();
                Object parsed = objectMapper.readValue(fileUrl, Object.class);
                if (parsed instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> urls = (List<String>) parsed;
                    for (String url : urls) {
                        if (url != null && !url.trim().isEmpty() && !fileUrls.contains(url)) {
                            fileUrls.add(url);
                        }
                    }
                } else if (parsed instanceof String) {
                    String url = (String) parsed;
                    if (!fileUrls.contains(url)) {
                        fileUrls.add(url);
                    }
                }
            } catch (Exception e) {
                // Not JSON, try comma-separated or single URL
                if (fileUrl.contains(",")) {
                    String[] urls = fileUrl.split(",");
                    for (String url : urls) {
                        String trimmed = url.trim();
                        if (!trimmed.isEmpty() && !fileUrls.contains(trimmed)) {
                            fileUrls.add(trimmed);
                        }
                    }
                } else {
                    if (!fileUrls.contains(fileUrl)) {
                        fileUrls.add(fileUrl);
                    }
                }
            }
        }
        
        // If this digital product was created from a design product, also fetch ALL images from source design product
        if (product.getSourceDesignProductId() != null) {
            Product sourceDesignProduct = productRepository.findByIdWithImages(product.getSourceDesignProductId())
                    .orElse(null);
            
            if (sourceDesignProduct != null && sourceDesignProduct.getImages() != null) {
                // Add all images from source design product
                for (ProductImage img : sourceDesignProduct.getImages()) {
                    String imgUrl = img.getImageUrl();
                    if (imgUrl != null && !imgUrl.trim().isEmpty() && !fileUrls.contains(imgUrl)) {
                        fileUrls.add(imgUrl);
                    }
                }
                
                // Also check if source design product has a design entity with image
                if (sourceDesignProduct.getDesignId() != null) {
                    Design design = designRepository.findById(sourceDesignProduct.getDesignId())
                            .orElse(null);
                    if (design != null && design.getImage() != null && !design.getImage().trim().isEmpty()) {
                        String designImageUrl = design.getImage();
                        if (!fileUrls.contains(designImageUrl)) {
                            fileUrls.add(designImageUrl);
                        }
                    }
                }
            }
        }
        
        if (fileUrls.isEmpty()) {
            throw new RuntimeException("No valid file URLs found");
        }
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            
            for (int i = 0; i < fileUrls.size(); i++) {
                String url = fileUrls.get(i);
                try {
                    // Download file from URL
                    URL fileUrlObj = URI.create(url).toURL();
                    HttpURLConnection connection = (HttpURLConnection) fileUrlObj.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(30000);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        continue; // Skip failed downloads
                    }
                    
                    // Get file extension from URL or use default
                    String fileName = "file_" + (i + 1);
                    String extension = "";
                    if (url.contains(".")) {
                        String[] parts = url.split("\\.");
                        if (parts.length > 1) {
                            extension = "." + parts[parts.length - 1].split("\\?")[0]; // Remove query params
                        }
                    }
                    if (extension.isEmpty()) {
                        extension = ".png"; // Default extension
                    }
                    fileName += extension;
                    
                    // Add to ZIP
                    ZipEntry entry = new ZipEntry(fileName);
                    zos.putNextEntry(entry);
                    
                    InputStream inputStream = connection.getInputStream();
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                    inputStream.close();
                    zos.closeEntry();
                } catch (Exception e) {
                    // Log error but continue with other files
                    System.err.println("Failed to download file from URL: " + url + " - " + e.getMessage());
                }
            }
            
            zos.close();
            byte[] zipBytes = baos.toByteArray();
            
            if (zipBytes.length == 0) {
                throw new RuntimeException("Failed to download any files");
            }
            
            ByteArrayResource resource = new ByteArrayResource(zipBytes);
            
            String zipFileName = product.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_files.zip";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipBytes.length)
                    .body(resource);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create ZIP file: " + e.getMessage(), e);
        }
    }
}
