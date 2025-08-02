package com.example.product_service.Service;

import com.example.product_service.DTO.ImageDto;
import com.example.product_service.DTO.ProductDto;
import com.example.product_service.Entity.Image;
import com.example.product_service.Entity.Product;
import com.example.product_service.Enums.Category;
import com.example.product_service.Exception.ProductWithIdNotFound;
import com.example.product_service.Repository.ProductRepository;
import com.example.product_service.wrapper.RestPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class ProductService {
    final private static Logger log = LoggerFactory.getLogger(ProductService.class);
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ImageService imageService;

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "products", allEntries = true)
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

    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(int productId) throws RuntimeException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductWithIdNotFound("Product not found"));

        imageService.deleteFiles(product.getImages());
        productRepository.delete(product);
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
    @Cacheable(value = "products", key = "#page + '-' + #size + '-' + #sortBy + '-' + #sortDir")
    public RestPage<ProductDto> findAll(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() :
                Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> result = productRepository.findAllWithImages(pageable);
        List<ProductDto> productDto = result.stream().map(p ->
                new ProductDto(p.getProductName(), p.getProductDescription()
                        , p.getPrice(), p.getCategoryName().toString(), p.getImages().stream().
                        map(img -> new ImageDto(img.getUrl())).toList()
                )).toList();
        return new RestPage<>(productDto, result.getPageable(), result.getTotalElements());
    }


}
