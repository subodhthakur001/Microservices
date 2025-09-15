package com.example.product_service.Repository;

import com.example.product_service.Entity.Product;
import com.example.product_service.Enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product,Integer> , JpaSpecificationExecutor<Product> {
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images")
    Page<Product> findAllWithImages(Pageable pageable);
    List<Product> findByCategoryName(Category category);


}
