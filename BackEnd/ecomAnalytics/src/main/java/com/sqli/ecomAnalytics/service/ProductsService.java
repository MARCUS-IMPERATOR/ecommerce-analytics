package com.sqli.ecomAnalytics.service;

import com.sqli.ecomAnalytics.dto.ProductCreateDto;
import com.sqli.ecomAnalytics.dto.ProductUpdateDto;
import com.sqli.ecomAnalytics.entity.ProductCategory;
import com.sqli.ecomAnalytics.entity.Products;
import com.sqli.ecomAnalytics.exceptions.ProductAlreadyExistsException;
import com.sqli.ecomAnalytics.exceptions.ProductNotFoundException;
import com.sqli.ecomAnalytics.exceptions.ProductStockInsufficient;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductsService {
    private final ProductRepository productRepository;

    public ProductsService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }


    private Products getProductById(int productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with id:" + productId + " not found"));
    }

    private Products getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException("Product with SKU:" + sku + " not found"));
    }

    @Caching(evict = {
            @CacheEvict(value = "productCatalogCache", key = "'products:all'"),
            @CacheEvict(value = "productByCategoryCache", allEntries = true),
            @CacheEvict(value = "productByBrandCache", allEntries = true),
            @CacheEvict(value = "productSearchCache", allEntries = true)
    })
    @Transactional
    public Products createProduct(ProductCreateDto product) {
        if (productRepository.findBySku(product.getSku()).isPresent()) {
            throw new ProductAlreadyExistsException("Product with SKU:" + product.getSku() + " already exists");
        }

        Products p = new Products();
        p.setSku(product.getSku());
        p.setName(product.getProductName());
        p.setPrice(product.getPrice());
        p.setBrand(product.getBrand());
        p.setCategory(product.getCategory());
        p.setDescription(product.getDescription());
        p.setStockQuantity(product.getStockQuantity());
        return productRepository.save(p);
    }

    @Caching(evict = {
            @CacheEvict(value = "productByIdCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).productIdKey(#productId)"),
            @CacheEvict(value = "productCatalogCache", key = "'products:all'"),
            @CacheEvict(value = "productByCategoryCache", allEntries = true),
            @CacheEvict(value = "productByBrandCache", allEntries = true),
            @CacheEvict(value = "productSearchCache", allEntries = true)
    })
    @Transactional
    public Products updateProduct(int productId, ProductUpdateDto product) {
        Products p = getProductById(productId);

        p.setPrice(product.getPrice());
        p.setBrand(product.getBrand());
        p.setCategory(product.getCategory());
        p.setDescription(product.getDescription());
        p.setStockQuantity(product.getStockQuantity());

        return productRepository.save(p);
    }

    @Caching(evict = {
            @CacheEvict(value = "productByIdCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).productIdKey(#productId)"),
            @CacheEvict(value = "productCatalogCache", key = "'products:all'"),
            @CacheEvict(value = "productByCategoryCache", allEntries = true),
            @CacheEvict(value = "productByBrandCache", allEntries = true),
            @CacheEvict(value = "productSearchCache", allEntries = true)
    })
    @Transactional
    public Products increaseStock(int productId, int quantity) {
        Products p = getProductById(productId);
        p.setStockQuantity(p.getStockQuantity() + quantity);
        return productRepository.save(p);
    }

    @Caching(evict = {
            @CacheEvict(value = "productByIdCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).productIdKey(#productId)"),
            @CacheEvict(value = "productCatalogCache", key = "'products:all'"),
            @CacheEvict(value = "productByCategoryCache", allEntries = true),
            @CacheEvict(value = "productByBrandCache", allEntries = true),
            @CacheEvict(value = "productSearchCache", allEntries = true)
    })
    @Transactional
    public Products decreaseStock(int productId, int quantity) {
        Products p = getProductById(productId);

        if (p.getStockQuantity() < quantity) {
            throw new ProductStockInsufficient("Product with Id:" + productId + " has only "
                    + p.getStockQuantity() + " and can't be reduced by " + quantity);
        }

        p.setStockQuantity(p.getStockQuantity() - quantity);
        return productRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<Products> findAllLowStockProducts() {
        int threshold = 10;
        return productRepository.findLowStock(threshold);
    }

    @Cacheable(value = "productSearchCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).productSearchKey(#searchTerm)")
    @Transactional(readOnly = true)
    public List<Products> searchProducts(String searchTerm) {
        return productRepository.searchByText(searchTerm);
    }

    @Cacheable(value = "productCatalogCache", key = "'products:all'")
    @Transactional(readOnly = true)
    public List<Products> findAllProducts() {
        return productRepository.findAll();
    }

    @Cacheable(value = "productByIdCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).productIdKey(#productId)")
    @Transactional(readOnly = true)
    public Products findProductById(int productId) {
        return getProductById(productId);
    }

    @Transactional(readOnly = true)
    public Products findProductBySku(String sku) {
        return getProductBySku(sku);
    }

    @Cacheable(value = "productByCategoryCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).productCategoryKey(#category.name())")
    @Transactional(readOnly = true)
    public List<Products> findAllProductsByCategory(ProductCategory category) {
        return productRepository.findByCategory(category);
    }

    @Cacheable(value = "productByBrandCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).productBrandKey(#brand)")
    @Transactional(readOnly = true)
    public List<Products> findAllProductsByBrand(String brand) {
        return productRepository.findByBrandIgnoreCase(brand);
    }

    @Transactional(readOnly = true)
    public List<Products> findAllProductsByBrandAndCategory(String brand, ProductCategory category) {
        return productRepository.findByCategoryAndBrandIgnoreCase(category, brand);
    }
}
