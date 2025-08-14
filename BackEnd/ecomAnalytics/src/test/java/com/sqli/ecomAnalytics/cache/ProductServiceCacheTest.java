package com.sqli.ecomAnalytics.cache;

import com.sqli.ecomAnalytics.dto.ProductCreateDto;
import com.sqli.ecomAnalytics.entity.ProductCategory;
import com.sqli.ecomAnalytics.entity.Products;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import com.sqli.ecomAnalytics.service.ProductsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public class ProductServiceCacheTest extends BaseCacheTest{

    @MockitoBean
    private ProductRepository productRepository;
    @Autowired
    private ProductsService  productsService;

    private Products p;
    private List<Products> products;

    @BeforeEach
    public void setup(){
        clearAllCaches();

        p = new Products();
        p.setName("Probook 640 F5");
        p.setDescription("Professional Laptop for everyday use");
        p.setProductId(1);
        p.setStockQuantity(10);
        p.setBrand("HP");
        p.setDescription("Laptop");
        p.setCategory(ProductCategory.LAPTOPS);
        p.setSku("SKU1");
        p.setPrice(new BigDecimal("1000"));

        products = Arrays.asList(p);
    }

    @Test
    @DisplayName("Should cache product by ID")
    void productByIdTest(){
        when(productRepository.findById(1)).thenReturn(Optional.of(p));

        Products r1 = productsService.findProductById(1);

        verify(productRepository, times(1)).findById(1);
        assertThat(r1).isEqualTo(p);

        Products r2 = productsService.findProductById(1);
        verify(productRepository, times(1)).findById(1);
        assertThat(r1).isEqualTo(p);

        String expectedKey = "product:id:1";
        assertCache("productByIdCache", expectedKey, p);
    }

    @Test
    @DisplayName("Should cache products by category")
    void cacheProductsByCategory() {
        when(productRepository.findByCategory(ProductCategory.LAPTOPS)).thenReturn(products);

        List<Products> r1 = productsService.findAllProductsByCategory(ProductCategory.LAPTOPS);

        verify(productRepository, times(1)).findByCategory(ProductCategory.LAPTOPS);
        assertThat(r1.size()).isEqualTo(1);

        List<Products> r2 = productsService.findAllProductsByCategory(ProductCategory.LAPTOPS);

        verify(productRepository, times(1)).findByCategory(ProductCategory.LAPTOPS);
        assertThat(r2.size()).isEqualTo(1);

        assertCache("productByCategoryCache", "product:category:laptops", products);
    }

    @Test
    @DisplayName("Should cache product search results")
    void cacheProductSearch() {
        String search = "use";
        when(productRepository.searchByText(search.toLowerCase())).thenReturn(products);

        List<Products> r1 = productsService.searchProducts(search);

        verify(productRepository, times(1)).searchByText(search.toLowerCase());
        assertThat(r1.size()).isEqualTo(1);

        List<Products> r2 = productsService.searchProducts(search);

        verify(productRepository, times(1)).searchByText(search.toLowerCase());
        assertThat(r2.size()).isEqualTo(1);

        assertCache("productSearchCache", "product:search:use", products);
    }

    @Test
    @DisplayName("Should evict multiple caches when creating product")
    void evictCachesOnProductCreation() {

        when(productRepository.findAll()).thenReturn(products);
        when(productRepository.findByCategory(any())).thenReturn(products);
        when(productRepository.findByBrandIgnoreCase(anyString())).thenReturn(products);
        when(productRepository.searchByText(anyString())).thenReturn(products);

        productsService.findAllProducts();
        productsService.findAllProductsByCategory(ProductCategory.LAPTOPS);
        productsService.findAllProductsByBrand("HP");
        productsService.searchProducts("everyday");

        assertCache("productCatalogCache", "products:all", products);

        ProductCreateDto createDto = new ProductCreateDto();
        createDto.setSku("LAPTOP002");
        createDto.setProductName("New Laptop");
        createDto.setPrice(new BigDecimal("1199.99"));
        createDto.setCategory(ProductCategory.LAPTOPS);
        createDto.setBrand("NewBrand");
        createDto.setStockQuantity(5);

        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());
        when(productRepository.save(any(Products.class))).thenReturn(p);

        productsService.createProduct(createDto);

        assertEvict("productCatalogCache", "products:all");
    }
}
