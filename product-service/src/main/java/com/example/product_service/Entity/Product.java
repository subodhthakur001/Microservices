package com.example.product_service.Entity;

import com.example.product_service.Enums.Category;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int productId;
    @Column(nullable = false, unique = true)
    private String productName;
    @Column(nullable = false)
    private String productDescription;
    @Column(nullable = false)
    private int price;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Category categoryName;
    @JsonManagedReference
    @OneToMany (mappedBy="product",fetch=FetchType.LAZY,cascade =CascadeType.ALL,orphanRemoval = true)
    @OrderColumn(name="position")
    private List<Image> images=new ArrayList<>();
public Product()
{

}
    public Product(String productName, String productDescription, int price, Category categoryName, List<Image> images) {
        this.productName = productName;
        this.productDescription = productDescription;
        this.price = price;
        this.categoryName = categoryName;
        this.images = images;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
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

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public Category getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(Category categoryName) {
        this.categoryName = categoryName;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }
    public void addImage(Image image) {
        images.add(image);
        image.setProduct(this);  // **Set owning side**
    }
}
