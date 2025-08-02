package com.example.product_service.Controller;

import com.example.product_service.DTO.ProductDto;
import com.example.product_service.Entity.Product;
import com.example.product_service.Service.ProductService;
import com.example.product_service.wrapper.RestPage;
import jakarta.ws.rs.core.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA, value = "/add")
    public ResponseEntity<?> addProdduct(@RequestPart("product") ProductDto productDto, @RequestPart("images") List<MultipartFile> files) {
        ProductDto savedProductDto = productService.addProduct(productDto, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProductDto);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable int id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA, value = "/update/{id}")
    public ResponseEntity<?> deleteProduct(@RequestPart("product") ProductDto productDto, @RequestPart("images") List<MultipartFile> files, @PathVariable int id) {
        ProductDto updatedProductDto = productService.updateProduct(id, productDto, files);
        return ResponseEntity.ok(updatedProductDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable int id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(defaultValue = "productName") String sortBy,
                                    @RequestParam(defaultValue = "asc") String sortDir) {
        RestPage<ProductDto> products = productService.findAll(page, size, sortBy, sortDir);
        Map<String, Object> response = new HashMap<>();
        response.put("products", products.getContent());
        response.put("currentPage", products.getNumber());
        response.put("totalItems", products.getTotalElements());
        response.put("totalPages", products.getTotalPages());
        return ResponseEntity.ok(response);

    }


}
