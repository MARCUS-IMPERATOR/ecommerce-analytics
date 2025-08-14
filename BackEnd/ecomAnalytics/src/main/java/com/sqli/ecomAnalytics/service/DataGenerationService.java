package com.sqli.ecomAnalytics.service;

import com.sqli.ecomAnalytics.configuration.DataGenerationProp;
import com.sqli.ecomAnalytics.generator.CustomerGenerator;
import com.sqli.ecomAnalytics.generator.OrderDataGenerator;
import com.sqli.ecomAnalytics.generator.ProductGenerator;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;


@Component
@ConditionalOnProperty(name = "data-generation.enabled", havingValue = "true")
public class DataGenerationService {

    private final CustomerGenerator customerGenerator;
    private final ProductGenerator productGenerator;
    private final OrderDataGenerator orderGenerator;
    private final DataGenerationProp prop;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    private static final Logger logger = LoggerFactory.getLogger(DataGenerationService.class);

    public DataGenerationService(CustomerGenerator customerGenerator, ProductGenerator productGenerator, OrderDataGenerator orderGenerator, DataGenerationProp prop, CustomerRepository customerRepository, CustomersService customersService, ProductsService productsService, ProductRepository productRepository) {
        this.customerGenerator = customerGenerator;
        this.productGenerator = productGenerator;
        this.orderGenerator = orderGenerator;
        this.prop = prop;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @PostConstruct
    public void generateDataOnStartup() {
        logger.info("Starting data generation on application startup...");

        try {
            // Check if data already exists
            if (shouldSkipGeneration()) {
                logger.info("Data already exists, skipping generation");
                return;
            }

            // Generate in correct order
            generateCustomers();
            generateProducts();
            generateOrders();

            logger.info("Data generation completed successfully");

        } catch (Exception e) {
            logger.error("Failed to generate data on startup", e);
            // Decide whether to fail fast or continue
            // throw new RuntimeException("Data generation failed", e);
        }
    }

    private boolean shouldSkipGeneration() {
        long customerCount = customerRepository.count();
        long productCount = productRepository.count();

        return customerCount > 0 && productCount > 0;
    }

    private void generateCustomers() {
        logger.info("Generating {} customers...", prop.getCustomers().getCount());
        customerGenerator.generateCustomers();
        logger.info("Customers generation completed");
    }

    private void generateProducts() {
        logger.info("Generating {} products...", prop.getProducts().getCount());
        productGenerator.generateAndSaveFullCatalog();
        logger.info("Products generation completed");
    }

    private void generateOrders() {
        logger.info("Generating orders for {} months with {} daily volume...",
                prop.getOrders().getMonths(),
                prop.getOrders().getDailyVolume());

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(prop.getOrders().getMonths());

        orderGenerator.generateOrdersForDateRange(startDate, endDate, prop.getOrders().getDailyVolume());
        logger.info("Orders generation completed");
    }
}
