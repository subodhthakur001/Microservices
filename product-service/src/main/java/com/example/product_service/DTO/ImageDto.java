package com.example.product_service.DTO;

import jakarta.validation.constraints.NotEmpty;

public class ImageDto {
    @NotEmpty
    private String url;


    public ImageDto() {
    }

    public ImageDto(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
