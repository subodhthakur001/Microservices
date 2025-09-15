package com.example.product_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductMetaDto {
    private int id;
    private String name;
    private String description;
    private int price;
    private String category;
    private String coverImageUrl; // only first image
}
