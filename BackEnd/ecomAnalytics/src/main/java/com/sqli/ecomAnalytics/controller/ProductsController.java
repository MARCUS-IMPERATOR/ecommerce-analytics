package com.sqli.ecomAnalytics.controller;

import com.sqli.ecomAnalytics.dto.ProductCreateDto;
import com.sqli.ecomAnalytics.dto.ProductUpdateDto;
import com.sqli.ecomAnalytics.entity.ProductCategory;
import com.sqli.ecomAnalytics.entity.Products;
import com.sqli.ecomAnalytics.service.ProductsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Products operations")
public class ProductsController {


    private final ProductsService productsService;

    public ProductsController(ProductsService productsService) {
        this.productsService = productsService;
    }

    @Operation(summary = "Get all products",responses = {
            @ApiResponse(responseCode = "200", description = "List of products returned successfully")
    })
    @GetMapping("/all")
    public ResponseEntity<List<Products>> getAllProducts() {
        List<Products> products = productsService.findAllProducts();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @Operation(summary = "Get product by ID",responses = {
                    @ApiResponse(responseCode = "200",description = "Product found"),
                    @ApiResponse(responseCode = "404",description = "Product not found")
    })
    @GetMapping("/{productId}")
    public ResponseEntity<Products> getProductById(
            @Parameter(description = "ID of the product to retrieve", example = "1", required = true)
            @PathVariable("productId") Integer productId) {
        Products product = productsService.findProductById(productId);
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @Operation(summary = "Get product by SKU", responses = {
            @ApiResponse(responseCode = "200",description = "Product found"),
            @ApiResponse(responseCode = "404",description = "Product not found")
    })
    @GetMapping("/sku/{productSku}")
    public ResponseEntity<Products> getProductBySku(
            @Parameter(description = "SKU of the product to retrieve", example = "PROD-123-XYZ", required = true)
            @PathVariable("productSku") String productSku) {
        Products product = productsService.findProductBySku(productSku);
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @Operation(summary = "Get products by category",responses = {
            @ApiResponse(responseCode = "200",description = "Products found"),
            @ApiResponse(responseCode = "400",description = "Invalid category provided")
    })
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Products>> getProductsByCategory(
            @Parameter(description = "Product category", example = "ELECTRONICS", required = true)
            @PathVariable("category") ProductCategory category) {
        List<Products> products = productsService.findAllProductsByCategory(category);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @Operation(summary = "Get products by brand",responses = {
            @ApiResponse(responseCode = "200",description = "Products found")
    })
    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<Products>> getProductsByBrand(
            @Parameter(description = "Product brand name", example = "Samsung", required = true)
            @PathVariable("brand") String brand) {
        List<Products> products = productsService.findAllProductsByBrand(brand);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @Operation(summary = "Get products by brand and category",responses = {
            @ApiResponse(responseCode = "200",description = "Products found"),
            @ApiResponse(responseCode = "400",description = "Invalid brand or category provided")
    })
    @GetMapping("/brand/{brand}/category/{category}")
    public ResponseEntity<List<Products>> getProductsByBrandAndCategory(
            @Parameter(description = "Product brand name", example = "Samsung", required = true)
            @PathVariable("brand") String brand,
            @Parameter(description = "Product category", example = "ELECTRONICS", required = true)
            @PathVariable("category") ProductCategory category) {
        List<Products> products = productsService.findAllProductsByBrandAndCategory(brand, category);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @Operation(summary = "Create a new product",responses = {
            @ApiResponse(responseCode = "201",description = "Product created successfully"),
            @ApiResponse(responseCode = "400",description = "Invalid product data provided"),
            @ApiResponse(responseCode = "409",description = "Product with provided SKU already exists")
    })
    @PostMapping("/createProduct")
    public ResponseEntity<Products> createProduct(
            @Parameter(description = "Product creation information", required = true)
            @RequestBody ProductCreateDto product) {
        Products p = productsService.createProduct(product);
        return new ResponseEntity<>(p, HttpStatus.CREATED);
    }

    @Operation(summary = "Increase product stock",responses = {
            @ApiResponse(responseCode = "200",description = "Stock increased successfully"),
            @ApiResponse(responseCode = "400",description = "Invalid quantity provided"),
            @ApiResponse(responseCode = "404",description = "Product not found")
            })
    @PatchMapping("/increase/{productId}/{quantity}")
    public ResponseEntity<Products> increaseStock(
            @Parameter(description = "ID of the product to update stock", example = "1", required = true)
            @PathVariable("productId") int productId,
            @Parameter(description = "Quantity to add to stock", example = "50", required = true)
            @PathVariable("quantity") int quantity) {
        Products p = productsService.increaseStock(productId, quantity);
        return new ResponseEntity<>(p, HttpStatus.OK);
    }

    @Operation(summary = "Decrease product stock",responses = {
            @ApiResponse(responseCode = "200",description = "Stock decreased successfully"),
            @ApiResponse(responseCode = "400",description = "Invalid quantity or insufficient stock"),
            @ApiResponse(responseCode = "404",description = "Product not found")
    })
    @PatchMapping("/decrease/{productId}/{quantity}")
    public ResponseEntity<Products> decreaseStock(
            @Parameter(description = "ID of the product to update stock", example = "1", required = true)
            @PathVariable("productId") int productId,
            @Parameter(description = "Quantity to remove from stock", example = "10", required = true)
            @PathVariable("quantity") int quantity) {
        Products p = productsService.decreaseStock(productId, quantity);
        return new ResponseEntity<>(p, HttpStatus.OK);
    }

    @Operation(summary = "Get low stock products",responses = {
            @ApiResponse(responseCode = "200",description = "Low stock products retrieved successfully")
    })
    @GetMapping("/lowStock")
    public ResponseEntity<List<Products>> getLowStockProducts() {
        List<Products> products = productsService.findAllLowStockProducts();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @Operation(summary = "Search products",responses = {
            @ApiResponse(responseCode = "200",description = "Search results returned successfully"),
            @ApiResponse(responseCode = "400",description = "Invalid search parameters")
    })
    @GetMapping("/search")
    public ResponseEntity<List<Products>> searchProducts(
            @Parameter(description = "Search term for product name, description, or SKU",
                    example = "smartphone", required = true)
            @RequestParam("search") String search) {
        List<Products> products = productsService.searchProducts(search);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @Operation(summary = "Update product information",responses = {
            @ApiResponse(responseCode = "200",description = "Product updated successfully"),
            @ApiResponse(responseCode = "400",description = "Invalid update data provided"),
            @ApiResponse(responseCode = "404",description = "Product not found")
    })
    @PatchMapping("/update/{productId}")
    public ResponseEntity<Products> updateProduct(
            @Parameter(description = "ID of the product to update", example = "1", required = true)
            @PathVariable("productId") int productId,
            @Parameter(description = "Product update information", required = true)
            @RequestBody ProductUpdateDto product) {
        Products p = productsService.updateProduct(productId, product);
        return new ResponseEntity<>(p, HttpStatus.OK);
    }
}
