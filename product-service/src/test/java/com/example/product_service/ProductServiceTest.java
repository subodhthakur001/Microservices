package com.example.product_service;

import com.example.product_service.DTO.ImageDto;
import com.example.product_service.DTO.ProductDto;
import com.example.product_service.Entity.Image;
import com.example.product_service.Entity.Product;
import com.example.product_service.Enums.Category;
import com.example.product_service.Exception.ProductWithIdNotFound;
import com.example.product_service.Repository.ProductRepository;
import com.example.product_service.Service.ImageService;
import com.example.product_service.Service.ProductService;
import com.example.product_service.wrapper.RestPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @InjectMocks
    private ProductService productService;
    @Mock
    private ProductRepository productRepsitory;
    @Mock
    private ImageService imageService;

    @Test
    public void addProduct() throws IOException {
        //set up dto
        ProductDto dto = new ProductDto();
        dto.setProductName("ABC");
        dto.setProductPrice(10);
        dto.setProductDescription("Good");
        dto.setCategoryName("ELECTRONICS");
        //set up entity
        Product savedProduct = new Product();
        savedProduct.setProductId(1);
        savedProduct.setProductName("ABC");
        savedProduct.setProductDescription("Good");
        savedProduct.setPrice(10);
        savedProduct.setCategoryName(Category.ELECTRONICS);
        //set up param for the service method
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(file1, file2);
        //mock the method
        when(imageService.save(file1)).thenReturn("file1.jpg");
        when(imageService.save(file2)).thenReturn("file2.jpg");
        when(productRepsitory.save(any(Product.class))).thenReturn(savedProduct);
        //call the service
        ProductDto savedProductDto = productService.addProduct(dto, files);
        //assertion
        assertNotNull(savedProductDto);
        verify(imageService, times(2)).save(any(MultipartFile.class));
        verify(productRepsitory, times(1)).save(any(Product.class));


    }
    @Test
    public void deleteProduct()
    {
        //set up entity
        Product savedProduct = new Product();
        savedProduct.setProductId(1);
        savedProduct.setProductName("ABC");
        savedProduct.setProductDescription("Good");
        savedProduct.setPrice(10);
        savedProduct.setCategoryName(Category.ELECTRONICS);
        List<Image> images = List.of(new Image("img1.jpg"), new Image("img2.jpg"));
        savedProduct.setImages(images);
        //call the service
        when(productRepsitory.findById(1)).thenReturn(Optional.of(savedProduct));
        productService.deleteProduct(1);
        verify(imageService,times(1)).deleteFiles(images);
        verify(productRepsitory,times(1)).delete(savedProduct);

    }
    @Test
    public void checkFoDeleteIfProductDoesNotExist()
    {
        assertThrows(ProductWithIdNotFound.class, () -> productService.deleteProduct(2));
        verify(imageService,Mockito.never()).deleteFiles(Mockito.any());
        verify(productRepsitory,Mockito.never()).delete(Mockito.any(Product.class));
    }
    @Test
    public void checkForProductWithIdExist()
    {
        Product searchProduct =new Product();
        searchProduct.setProductId(1);
        searchProduct.setProductName("AC");
        searchProduct.setCategoryName(Category.ELECTRONICS);
        searchProduct.setProductDescription("Good AC");
        Image img=new Image(1,"file1.jpg");
        Image img2=new Image(2,"file2.jpg");
        List<Image> images=List.of(img,img2);
        searchProduct.setImages(images);
        when(productRepsitory.findById(1)).thenReturn(Optional.of(searchProduct));
        ProductDto dto=productService.getProduct(1);
        assertNotNull(dto);
        assertEquals(2,dto.getImages().size());
        assertEquals("AC",dto.getProductName());
    }
    @Test
    public void checkWhenProductWithIdNotExist()
    {
        assertThrows(ProductWithIdNotFound.class, () -> productService.deleteProduct(2));

    }

    @Test
    public void testUpdateProduct() throws IOException {
        Product searchProduct = new Product();
        searchProduct.setProductId(1);
        searchProduct.setProductName("AC");
        searchProduct.setCategoryName(Category.ELECTRONICS);
        searchProduct.setProductDescription("Good AC");
        searchProduct.setPrice(20);
        Image img = new Image(1, "file1");
        Image img2 = new Image(2, "file2");
        List<Image> images = Stream.of(img, img2).collect(Collectors.toList());
        searchProduct.setImages(images);

        when(productRepsitory.findById(1)).thenReturn(Optional.of(searchProduct));

        MultipartFile file1 = mock(MultipartFile.class);
        List<MultipartFile> files = Arrays.asList(file1);

        ProductDto dto = new ProductDto();
        dto.setProductPrice(10);
        dto.setProductDescription("Good AC");
        dto.setCategoryName("ELECTRONICS");

        doNothing().when(imageService).deleteFiles(anyList());

        Product updateProduct = new Product();
        updateProduct.setPrice(10);
        updateProduct.setCategoryName(Category.ELECTRONICS);
        updateProduct.setProductName("AC");
        updateProduct.setProductDescription("Good AC");
        updateProduct.setImages(Arrays.asList(new Image(1, "file")));

        when(imageService.save(file1)).thenReturn("file1.jpg");
        when(productRepsitory.findById(1)).thenReturn(Optional.of(searchProduct));
        when(productRepsitory.save(any(Product.class))).thenReturn(updateProduct);
        ProductDto updatedDto = productService.updateProduct(1, dto, files);

        assertNotNull(updatedDto);
        assertEquals(dto.getProductPrice(), updatedDto.getProductPrice());
        assertEquals(1, updatedDto.getImages().size());
        verify(imageService, times(1)).save(any(MultipartFile.class));
        verify(productRepsitory, times(1)).save(any(Product.class));


    }
    @Test
    public void findAll()
    {
        int page = 0, size = 2;
        String sortBy = "price", sortDir = "asc";

        // Prepare some Product entities
        Product p1 = new Product();
        p1.setProductName("P1");
        p1.setProductDescription("Desc1");
        p1.setPrice(100);
        p1.setCategoryName(Category.ELECTRONICS);
        p1.setImages(List.of(new Image(1, "u1"), new Image(2, "u2")));

        Product p2 = new Product();
        p2.setProductName("P2");
        p2.setProductDescription("Desc2");
        p2.setPrice(200);
        p2.setCategoryName(Category.ELECTRONICS);
        p2.setImages(List.of(new Image(3, "u3")));

        List<Product> content = List.of(p1, p2);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<Product> productPage = new PageImpl<>(content, pageable, 5);

        when(productRepsitory.findAllWithImages(pageable)).thenReturn(productPage);

        // Call service
        RestPage<ProductDto> result = productService.findAll(page, size, sortBy, sortDir);

        // Assertions
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(pageable.getPageNumber(), result.getPageable().getPageNumber());
        assertEquals(pageable.getPageSize(), result.getPageable().getPageSize());

        ProductDto dto1 = result.getContent().get(0);
        assertEquals("P1", dto1.getProductName());
        assertEquals(100, dto1.getProductPrice());
        assertEquals(2, dto1.getImages().size());
        assertTrue(dto1.getImages().stream()
                .anyMatch(i -> "u2".equals(i.getUrl())));

        verify(productRepsitory, times(1)).findAllWithImages(pageable);
    }
}
