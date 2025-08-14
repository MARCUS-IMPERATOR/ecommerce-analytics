package com.sqli.ecomAnalytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqli.ecomAnalytics.configuration.SecurityConfig;
import com.sqli.ecomAnalytics.dto.ProductCreateDto;
import com.sqli.ecomAnalytics.dto.ProductUpdateDto;
import com.sqli.ecomAnalytics.entity.ProductCategory;
import com.sqli.ecomAnalytics.entity.Products;
import com.sqli.ecomAnalytics.exceptions.ProductStockInsufficient;
import com.sqli.ecomAnalytics.service.ProductsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductsController.class)
@Import(SecurityConfig.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductsService productsService;

    @MockitoBean
    private JpaMetamodelMappingContext  jpaMetamodelMappingContext;


    @Test
    void getAllProducts() throws Exception {
        List<Products> products = List.of(new Products(), new Products());
        when(productsService.findAllProducts()).thenReturn(products);

        mockMvc.perform(get("/api/products/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }


    @Test
    void getProductById() throws Exception {
        Products product = new Products();
        when(productsService.findProductById(1)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk());
    }


    @Test
    void getProductBySku() throws Exception {
        Products product = new Products();
        when(productsService.findProductBySku("PROD-123")).thenReturn(product);

        mockMvc.perform(get("/api/products/sku/PROD-123"))
                .andExpect(status().isOk());
    }


    @Test
    void getProductsByCategory() throws Exception {
        when(productsService.findAllProductsByCategory(ProductCategory.LAPTOPS))
                .thenReturn(List.of(new Products()));

        mockMvc.perform(get("/api/products/category/LAPTOPS"))
                .andExpect(status().isOk());
    }


    @Test
    void getProductsByBrand() throws Exception {
        when(productsService.findAllProductsByBrand("Apple"))
                .thenReturn(List.of(new Products()));

        mockMvc.perform(get("/api/products/brand/Apple"))
                .andExpect(status().isOk());
    }


    @Test
    void getProductsByBrandAndCategory() throws Exception {
        when(productsService.findAllProductsByBrandAndCategory("Apple", ProductCategory.LAPTOPS))
                .thenReturn(List.of(new Products()));

        mockMvc.perform(get("/api/products/brand/Apple/category/LAPTOPS"))
                .andExpect(status().isOk());
    }


    @Test
    void createProduct_success() throws Exception {
        ProductCreateDto dto = new ProductCreateDto();
        Products product = new Products();
        when(productsService.createProduct(any())).thenReturn(product);

        mockMvc.perform(post("/api/products/createProduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }


    @Test
    void increaseStock() throws Exception {
        when(productsService.increaseStock(1, 10)).thenReturn(new Products());

        mockMvc.perform(patch("/api/products/increase/1/10"))
                .andExpect(status().isOk());
    }


    @Test
    void decreaseStock() throws Exception {
        when(productsService.decreaseStock(1, 10)).thenReturn(new Products());

        mockMvc.perform(patch("/api/products/decrease/1/10"))
                .andExpect(status().isOk());
    }


    @Test
    void decreaseStock_insufficient() throws Exception {
        when(productsService.decreaseStock(1, 1000)).thenThrow(new ProductStockInsufficient("Product's stock exceeded"));

        mockMvc.perform(patch("/api/products/decrease/1/1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLowStockProducts() throws Exception {
        when(productsService.findAllLowStockProducts()).thenReturn(List.of(new Products()));

        mockMvc.perform(get("/api/products/lowStock"))
                .andExpect(status().isOk());
    }


    @Test
    void searchProducts() throws Exception {
        when(productsService.searchProducts("phone")).thenReturn(List.of(new Products()));

        mockMvc.perform(get("/api/products/search").param("search", "phone"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProduct() throws Exception {
        ProductUpdateDto dto = new ProductUpdateDto();
        when(productsService.updateProduct(eq(1), any())).thenReturn(new Products());

        mockMvc.perform(patch("/api/products/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}
