package com.example.product_service.DTO;

import jakarta.validation.constraints.NotEmpty;
import java.util.*;

public class ProductDto {
    @NotEmpty
    private String productName;
    @NotEmpty
    private String productDescription;
    @NotEmpty
    private int productPrice;
    @NotEmpty
    private String categoryName;
    private List<ImageDto> images=new ArrayList<>();

    public ProductDto() {
    }

    public ProductDto(String productName, String productDescription, int productPrice, String categoryName, List<ImageDto> images) {
        this.productName = productName;
        this.productDescription = productDescription;
        this.productPrice = productPrice;
        this.categoryName = categoryName;
        this.images = images;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public int getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(int productPrice) {
        this.productPrice = productPrice;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<ImageDto> getImages() {
        return images;
    }

    public void setImages(List<ImageDto> images) {
        this.images = images;
    }

    @Override
    public String toString() {
        return "ProductDto{" +
                "productName='" + productName + '\'' +
                ", productDescription='" + productDescription + '\'' +
                ", productPrice=" + productPrice +
                ", categoryName='" + categoryName + '\'' +
                ", images=" + images +
                '}';
    }

}
