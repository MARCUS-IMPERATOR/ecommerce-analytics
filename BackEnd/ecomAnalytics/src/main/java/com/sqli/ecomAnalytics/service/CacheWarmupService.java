package com.sqli.ecomAnalytics.service;

import com.sqli.ecomAnalytics.entity.ProductCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Component
public class CacheWarmupService {
    private static final Logger log = LoggerFactory.getLogger(CacheWarmupService.class);
    private final ProductsService productsService;
    private final CustomersService customersService;

    public CacheWarmupService(ProductsService productsService, CustomersService customersService) {
        this.productsService = productsService;
        this.customersService = customersService;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    @Retryable(retryFor = { Exception.class },maxAttempts = 3,backoff = @Backoff(delay = 5000))
    public void init(){
        log.info("Starting cache warmup...");
        try {
            productsService.findAllProducts();
            productsService.findAllProductsByCategory(ProductCategory.LAPTOPS);
            productsService.findAllProductsByCategory(ProductCategory.SMARTPHONES);
            productsService.findAllProductsByCategory(ProductCategory.TABLETS);
            productsService.findAllProductsByCategory(ProductCategory.GAMING_CONSOLES);
            productsService.findAllProductsByCategory(ProductCategory.OTHERS);
            customersService.findAllCustomers();
            customersService.findHighestPayingCustomers();
            log.info("Finished cache warmup...");
        }catch (Exception e){
            log.error("Cache warmup attempt failed: {}",e.getMessage());
            throw e;
        }
    }

    @Recover
    public void recover(Exception e) {
        log.error("Cache warmup failed after all retries: {}",e.getMessage());
    }

}
