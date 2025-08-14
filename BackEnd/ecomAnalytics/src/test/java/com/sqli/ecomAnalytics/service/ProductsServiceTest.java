package com.sqli.ecomAnalytics.service;

import com.sqli.ecomAnalytics.dto.ProductCreateDto;
import com.sqli.ecomAnalytics.dto.ProductUpdateDto;
import com.sqli.ecomAnalytics.entity.ProductCategory;
import com.sqli.ecomAnalytics.entity.Products;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class ProductsServiceTest {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductsService productsService;

    @Test
    void createProduct() {
        ProductCreateDto dto = new ProductCreateDto("SKU123", "name", ProductCategory.LAPTOPS, "Apple", BigDecimal.valueOf(1000), "Desc", 20);
        when(productRepository.findBySku("SKU123")).thenReturn(Optional.empty());

        Products saved = new Products();
        saved.setSku("SKU123");
        when(productRepository.save(any(Products.class))).thenReturn(saved);

        Products result = productsService.createProduct(dto);

        assertEquals("SKU123", result.getSku());
    }

    @Test
    void updateProduct() {
        Products existingProduct = new Products();
        existingProduct.setProductId(1);
        existingProduct.setPrice(BigDecimal.valueOf(1000));

        ProductUpdateDto dto = new ProductUpdateDto("Desc", ProductCategory.LAPTOPS, "Dell", BigDecimal.valueOf(1222), 30);

        when(productRepository.findById(1)).thenReturn(Optional.of(existingProduct));

        Products updatedProduct = new Products();
        updatedProduct.setProductId(1);
        updatedProduct.setDescription(dto.getDescription());
        updatedProduct.setCategory(dto.getCategory());
        updatedProduct.setBrand(dto.getBrand());
        updatedProduct.setPrice(dto.getPrice());
        updatedProduct.setStockQuantity(dto.getStockQuantity());

        when(productRepository.save(any(Products.class))).thenReturn(updatedProduct);

        Products result = productsService.updateProduct(1, dto);

        assertEquals(BigDecimal.valueOf(1222), result.getPrice());
    }

    @Test
    void increaseStock() {
        Products product = new Products();
        product.setProductId(1);
        product.setStockQuantity(10);

        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        Products updatedProduct = new Products();
        updatedProduct.setProductId(1);
        updatedProduct.setStockQuantity(15);

        when(productRepository.save(any(Products.class))).thenReturn(updatedProduct);

        Products result = productsService.increaseStock(1, 5);

        assertEquals(15, result.getStockQuantity());
    }

    @Test
    void decreaseStock() {
        Products product = new Products();
        product.setProductId(1);
        product.setStockQuantity(10);

        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        Products updatedProduct = new Products();
        updatedProduct.setProductId(1);
        updatedProduct.setStockQuantity(5);

        when(productRepository.save(any(Products.class))).thenReturn(updatedProduct);

        Products result = productsService.decreaseStock(1, 5);

        assertEquals(5, result.getStockQuantity());
    }

    @Test
    void findAllLowStockProducts() {
        List<Products> lowStockProducts = List.of(new Products(), new Products());
        when(productRepository.findLowStock(10)).thenReturn(lowStockProducts);

        List<Products> result = productsService.findAllLowStockProducts();

        assertEquals(2, result.size());
    }

    @Test
    void searchProducts() {
        when(productRepository.searchByText("test")).thenReturn(List.of(new Products()));

        assertEquals(1, productsService.searchProducts("test").size());
    }
}
