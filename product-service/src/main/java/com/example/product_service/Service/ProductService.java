package com.example.product_service.Service;

import com.example.product_service.DTO.ImageDto;
import com.example.product_service.DTO.ProductDto;
import com.example.product_service.DTO.ProductMetaDto;
import com.example.product_service.Entity.Image;
import com.example.product_service.Entity.Product;
import com.example.product_service.Enums.Category;
import com.example.product_service.Exception.ProductWithIdNotFound;
import com.example.product_service.Repository.ProductRepository;
import com.example.product_service.wrapper.RestPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {
    final private static Logger log = LoggerFactory.getLogger(ProductService.class);
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ImageService imageService;

    @Autowired
    private RedisTemplate<String,ProductMetaDto> redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    public ProductDto addProduct(ProductDto productDto, List<MultipartFile> files) {
        Product p = convertToEntity(productDto);
        List<Image> images = saveImage(files);
        images.forEach(p::addImage);
        log.debug("All images attached to product");
        // Register cleanup if DB transaction fails
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            transactionStatus(p, files);
        }
        Product savedProduct = productRepository.save(p);
        redisTemplate.opsForHash().put(getHashKey(productDto.getCategoryName().toString()),String.valueOf(savedProduct.getProductId()),convertToMetaDto(savedProduct));
        log.info("Product saved successfully");
        return convertToDto(savedProduct);
    }

    private List<Image> saveImage(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        return files.stream()
                .map(file -> {
                    try {
                        String fileName = imageService.save(file);
                        Image image = new Image();
                        image.setUrl(fileName);
                        return image;
                    } catch (IOException e) {
                        log.error("Failed to save image: {}", file.getOriginalFilename(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

    }

    public void deleteProduct(int productId) throws RuntimeException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductWithIdNotFound("Product not found"));

        imageService.deleteFiles(product.getImages());
        productRepository.delete(product);
        redisTemplate.opsForHash().delete(getHashKey(String.valueOf(product.getCategoryName())), String.valueOf(productId));
    }

    public ProductDto getProduct(int productId) {
        return productRepository.findById(productId).map(this::convertToDto)
                .orElseThrow(() -> new ProductWithIdNotFound("Product with id not found"));
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "products", allEntries = true)
    public ProductDto updateProduct(int productId, ProductDto productDto, List<MultipartFile> files) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with id not found"));
        log.info("Product with id exists");
        p.setPrice(productDto.getProductPrice());
        p.setProductDescription(productDto.getProductDescription());
        p.setCategoryName(Category.valueOf(productDto.getCategoryName()));
        if (!files.isEmpty()) {

            deleteFiles(p);
            List<Image> images = saveImage(files);
            log.info("Images saved successfully");
            images.forEach(p::addImage);
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            transactionStatus(p, files);
        }

        productRepository.save(p);
        log.info("Product updated successfully");
        redisTemplate.opsForHash().put(getHashKey(String.valueOf(p.getCategoryName())),String.valueOf(p.getProductId()),convertToMetaDto(p));
        return convertToDto(p);

    }

    private void deleteFiles(Product p) {
        if (p.getImages() != null || !p.getImages().isEmpty()) {
            imageService.deleteFiles(p.getImages());
            p.getImages().clear();

        }
    }

    private void transactionStatus(Product p, List<MultipartFile> files) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED && !files.isEmpty()) {
                            imageService.deleteFiles(p.getImages());
                        }
                    }
                });

    }

    private ProductDto convertToDto(Product product) {
        List<ImageDto> images = product.getImages().stream()
                .map(image -> new ImageDto(image.getUrl()))
                .collect(Collectors.toCollection(ArrayList::new)); // Java 16+ or 17

        return new ProductDto(product.getProductName(), product.getProductDescription(),
                product.getPrice(), product.getCategoryName().toString(), images
        );
    }

    private Product convertToEntity(ProductDto productDto) {
        List<Image> images = productDto.getImages().stream().map(image -> new Image(image.getUrl())).collect(Collectors.toCollection(ArrayList::new));
        return new Product(productDto.getProductName(), productDto.getProductDescription(), productDto.getProductPrice(), Category.valueOf(productDto.getCategoryName().toUpperCase()), images);
    }

    @Transactional(readOnly = true)
    public RestPage<ProductMetaDto> findAll(int page, int size, String sortBy, String sortDir) {
        log.info("Fetching products for category: {}", sortBy);

        List<ProductMetaDto> productDto;
        Map<Object, Object> products = getProductsFromCacheWithRetry(getHashKey(sortBy));

        if (products != null && !products.isEmpty()) {
            log.info("✅ Data found in cache");
ObjectMapper objectMapper=new ObjectMapper();
            productDto = products.values().stream()
                    .map(v -> objectMapper.convertValue(v, ProductMetaDto.class))
                    .toList();
        } else {
            log.info("❌ Data not in cache, fetching from DB");

            Map<Object, Object> productObject = productRepository
                    .findByCategoryName(Category.valueOf(sortBy))
                    .stream()
                    .collect(Collectors.toMap(
                            p -> String.valueOf(p.getProductId()),
                            this::convertToMetaDto
                    ));

            productDto = productObject.values().stream() .map(p -> (ProductMetaDto) p) .toList();
            // try to cache (no retry needed here, failure is ok)
            try {
                redisTemplate.opsForHash().putAll(getHashKey(sortBy), productObject);
            } catch (Exception e) {
                log.warn("Could not cache data in Redis. Error: {}", e.getMessage());
            }
        }

        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), productDto.size());

        return new RestPage<>(productDto.subList(start, end), pageable, productDto.size());
    }

    // ✅ Retry for Redis lookup
    @Retryable(
            value = { RedisConnectionFailureException.class, RedisSystemException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2) // exponential backoff
    )
    public Map<Object, Object> getProductsFromCacheWithRetry(String categoryKey) {
        log.debug("Trying to fetch from Redis, key={}", categoryKey);
        return redisTemplate.opsForHash().entries(categoryKey);
    }

    // ✅ Fallback after retries exhausted
    @Recover
    public Map<Object, Object> recoverFromRedisFailure(Exception ex, String categoryKey) {
        log.warn("⚠️ Redis unavailable after retries. Falling back to DB for key={} | Error: {}",
                categoryKey, ex.getMessage());
        return null; // force DB fetch in findAll
    }

    private ProductMetaDto convertToMetaDto(Product product) {
        String coverImageUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            coverImageUrl = product.getImages().get(0).getUrl(); // assuming getUrl()
        }
        return new ProductMetaDto(
                product.getProductId(),
                product.getProductName(),
                product.getProductDescription(),
                product.getPrice(),
                product.getCategoryName().name(),
                coverImageUrl
        );
    }
    @Retryable(
            value = { RedisConnectionFailureException.class, RedisSystemException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000) // wait 2s between retries
    )
    public Map<Object, Object> getProductsFromCache(String categoryKey) {
        return redisTemplate.opsForHash().entries(categoryKey);
    }

    private String getHashKey(String category) {
        return "products:hash:" + category.toLowerCase();
    }

}
