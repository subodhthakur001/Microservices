package com.example.product_service.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int imageId;
    @Column(nullable = false)
    private String url;
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name="product_id")
    Product product;

    public Image() {

    }

    public Image(String url) {
        this.url = url;
    }
    public Image(int imageId,String url) {
        this.url = url;
        this.imageId=imageId;
    }


    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
