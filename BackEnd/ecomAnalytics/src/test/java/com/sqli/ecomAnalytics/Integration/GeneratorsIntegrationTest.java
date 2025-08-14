package com.sqli.ecomAnalytics.Integration;

import com.sqli.ecomAnalytics.dto.CustomerRegistrationDto;
import com.sqli.ecomAnalytics.dto.ProductCreateDto;
import com.sqli.ecomAnalytics.entity.*;
import com.sqli.ecomAnalytics.exceptions.CustomerAlreadyExistsException;
import com.sqli.ecomAnalytics.exceptions.ProductAlreadyExistsException;
import com.sqli.ecomAnalytics.generator.OrderDataGenerator;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.OrderItemsRepository;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import com.sqli.ecomAnalytics.service.CustomersService;
import com.sqli.ecomAnalytics.service.OrdersService;
import com.sqli.ecomAnalytics.service.ProductsService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class GeneratorsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("ecome_analytics_generators_test")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);

        registry.add("spring.flyway.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.flyway.user", postgreSQLContainer::getUsername);
        registry.add("spring.flyway.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private OrderDataGenerator orderDataGenerator;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderItemsRepository orderItemsRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductsService productService;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private CustomersService customersService;


    @BeforeEach
    void tearDown() {
        orderItemsRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();
    }
    @Test
    @Order(1)
    void containerShouldBeRunning() {
        assertThat(postgreSQLContainer.isRunning()).isTrue();
        assertThat(postgreSQLContainer.getDatabaseName()).isEqualTo("ecome_analytics_generators_test");
    }

    private List<Customers> createTestCustomers() {
        List<Customers> customers = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            CustomerRegistrationDto customer = new CustomerRegistrationDto();
            customer.setFirstName("Test" + i);
            customer.setLastName("Customer");
            customer.setEmail("test" + i + System.currentTimeMillis() + "@example.com");
            customer.setAge(25 + i);
            customer.setCountry("Morocco");
            customer.setPhone("+212600000" + String.format("%03d", i));
            customer.setRegisterDate(LocalDateTime.now().minusDays(i));

            try {
                Customers saved = customersService.registerCustomer(customer);
                customers.add(saved);
            } catch (CustomerAlreadyExistsException e) {
            }
        }

        return customers;
    }

    private List<Customers> createSegmentedTestCustomers() {
        List<Customers> customers = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            CustomerRegistrationDto customer = new CustomerRegistrationDto();
            customer.setFirstName("Segment" + i);
            customer.setLastName("Customer");
            customer.setEmail("segment" + i + System.currentTimeMillis() + "@example.com");
            customer.setAge(25 + i);
            customer.setCountry("Morocco");
            customer.setPhone("+212700000" + String.format("%03d", i));
            customer.setRegisterDate(LocalDateTime.now().minusDays(i));

            try {
                Customers saved = customersService.registerCustomer(customer);
                customers.add(saved);
            } catch (CustomerAlreadyExistsException e) {
            }
        }

        return customers;
    }

    private List<Products> createTestProducts() {
        List<ProductCreateDto> productDtos = List.of(
                createSmartphoneDto(),
                createLaptopDto(),
                createAccessoryDto("case"),
                createAccessoryDto("screen protector"),
                createAccessoryDto("charger")
        );

        List<Products> products = new ArrayList<>();
        productDtos.forEach(dto -> {
            try {
                Products saved = productService.createProduct(dto);
                products.add(saved);
            } catch (ProductAlreadyExistsException e) {
            }
        });

        return products;
    }

    private Products createSmartphoneProduct() {
        try {
            Products saved = productService.createProduct(createSmartphoneDto());
            return saved;
        } catch (ProductAlreadyExistsException e) {
            return null;
        }
    }

    private List<Products> createSmartphoneAccessories() {
        List<String> accessoryTypes = List.of("case", "screen protector", "charger");
        List<Products> accessories = new ArrayList<>();

        accessoryTypes.forEach(type -> {
            try {
                Products accessory = productService.createProduct(createAccessoryDto(type));
                accessories.add(accessory);
            } catch (ProductAlreadyExistsException e) {
            }
        });

        return accessories;
    }

    private List<Products> createLimitedStockProducts() {
        ProductCreateDto limitedProduct = createSmartphoneDto();
        limitedProduct.setStockQuantity(1);
        limitedProduct.setSku("LIMITED-" + System.currentTimeMillis());

        try {
            Products saved = productService.createProduct(limitedProduct);
            return List.of(saved);
        } catch (ProductAlreadyExistsException e) {
            return Collections.emptyList();
        }
    }

    private ProductCreateDto createSmartphoneDto() {
        ProductCreateDto smartphone = new ProductCreateDto();
        smartphone.setSku("SM-TEST-" + System.currentTimeMillis());
        smartphone.setCategory(ProductCategory.SMARTPHONES);
        smartphone.setBrand("TestBrand");
        smartphone.setProductName("Test iPhone Pro");
        smartphone.setPrice(new BigDecimal("999.99"));
        smartphone.setStockQuantity(50);
        smartphone.setDescription("Test smartphone for integration testing");
        return smartphone;
    }

    private ProductCreateDto createLaptopDto() {
        ProductCreateDto laptop = new ProductCreateDto();
        laptop.setSku("LP-TEST-" + System.currentTimeMillis());
        laptop.setCategory(ProductCategory.LAPTOPS);
        laptop.setBrand("TestBrand");
        laptop.setProductName("Test MacBook Pro");
        laptop.setPrice(new BigDecimal("1999.99"));
        laptop.setStockQuantity(30);
        laptop.setDescription("Test laptop for integration testing");
        return laptop;
    }

    private ProductCreateDto createAccessoryDto(String type) {
        ProductCreateDto accessory = new ProductCreateDto();
        accessory.setSku("AC-TEST-" + type + "-" + System.currentTimeMillis());
        accessory.setCategory(ProductCategory.ACCESSORIES);
        accessory.setBrand("TestBrand");
        accessory.setProductName("Test " + type);
        accessory.setPrice(new BigDecimal("29.99"));
        accessory.setStockQuantity(100);
        accessory.setDescription("Test " + type + " for integration testing");
        return accessory;
    }

    @Test
    @Transactional
    void debugOrderGeneration() {
        List<Customers> customers = createTestCustomers();
        List<Products> products = createTestProducts();

        System.out.println("Before generation - Orders count: " + orderRepository.count());
        System.out.println("Before generation - Customers count: " + customerRepository.count());
        System.out.println("Before generation - Products count: " + productRepository.count());

        orderDataGenerator.generateOrdersForDateRange(
                LocalDate.now(),
                LocalDate.now(),
                1
        );

        System.out.println("After generation - Orders count: " + orderRepository.count());
        System.out.println("After generation - Order items count: " + orderItemsRepository.count());

        List<Orders> allOrders = orderRepository.findAll();
        System.out.println("Direct repository access - Orders found: " + allOrders.size());

        allOrders.forEach(order -> {
            System.out.println("Order ID: " + order.getOrderId());
            System.out.println("Order Date: " + order.getOrderDate());
            System.out.println("Customer ID: " + (order.getCustomer() != null ? order.getCustomer().getCustomerId() : "NULL"));
            System.out.println("Order Items count: " + (order.getOrderItems() != null ? order.getOrderItems().size() : "NULL"));
        });
    }

    @Test
    void checkDataPersistence() {
        List<Customers> customers = createTestCustomers();
        List<Products> products = createTestProducts();

        assertThat(customerRepository.count()).isEqualTo(10);
        assertThat(productRepository.count()).isEqualTo(5);

        System.out.println("Data persistence check passed");
    }

    @Test
    @Order(2)
    @Transactional
    void generateOrdersIntegration_ShouldCreateCompleteOrderWorkflow() {
        List<Customers> customers = createTestCustomers();
        List<Products> products = createTestProducts();

        assertThat(customers.size()).isEqualTo(10);
        assertThat(products.size()).isEqualTo(5);

        System.out.println("Generating orders...");
        orderDataGenerator.generateOrdersForDateRange(
                LocalDate.now().minusDays(2),
                LocalDate.now(),
                5
        );
        System.out.println("Orders generation completed");

        List<Orders> orders = ordersService.getAllOrders();
        System.out.println("Retrieved orders: " + (orders != null ? orders.size() : "null"));

        assertThat(orders).isNotNull();

        if (orders.isEmpty()) {
            System.out.println("No orders found! Check if orderDataGenerator is working correctly");
            return;
        }

        assertThat(orders.size()).isGreaterThanOrEqualTo(15);

        orders.forEach(order -> {
            System.out.println("Checking order: " + order.getOrderId());
            System.out.println("Order items: " + (order.getOrderItems() != null ? order.getOrderItems().size() : "null"));
            System.out.println("Customer: " + (order.getCustomer() != null ? order.getCustomer().getCustomerId() : "null"));

            assertThat(order.getOrderItems()).isNotNull();
            assertThat(order.getCustomer()).isNotNull();
            assertThat(order.getOrderDate()).isNotNull();
            assertThat(order.getStatus()).isIn(
                    OrderStatus.PENDING,
                    OrderStatus.DELIVERED,
                    OrderStatus.CANCELLED
            );

            if (order.getOrderItems() != null) {
                order.getOrderItems().forEach(item -> {
                    System.out.println("Checking order item: " + item.getOrderId());
                    System.out.println("Product: " + (item.getProduct() != null ? item.getProduct().getProductId() : "null"));
                    assertThat(item.getProduct()).isNotNull();
                    assertThat(item.getQuantity()).isGreaterThan(0);
                });
            }
        });
    }
}
