package com.sqli.ecomAnalytics.Integration;

import com.sqli.ecomAnalytics.dto.OrderCreateDto;
import com.sqli.ecomAnalytics.dto.OrderItemsDto;
import com.sqli.ecomAnalytics.entity.*;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.OrderItemsRepository;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OrderControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("ecom_analytics_test")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        registry.add("spring.flyway.url", postgresContainer::getJdbcUrl);
        registry.add("spring.flyway.user", postgresContainer::getUsername);
        registry.add("spring.flyway.password", postgresContainer::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemsRepository orderItemsRepository;

    @LocalServerPort
    private int port;

    private String url;
    private Customers c;
    private Products p1;
    private Products p2;

    private void setUpTestData() {
        c = new Customers();
        c.setCustomerCode("CUSTTEST001");
        c.setFirstName("John");
        c.setLastName("Doe");
        c.setAge(29);
        c.setCountry("Morocco");
        c.setEmail("john.doe@test.com");
        c.setPhone("+1234567890");
        c.setRegistrationDate(LocalDateTime.now());
        c.setTotalSpent(BigDecimal.ZERO);
        c.setOrderCount(0);
        c = customerRepository.save(c);

        p1 = new Products();
        p1.setSku("PROD-001");
        p1.setName("MacBook Air");
        p1.setDescription("Apple 2025 MacBook Air 13-inch Laptop with M4 chip");
        p1.setCategory(ProductCategory.LAPTOPS);
        p1.setBrand("Apple");
        p1.setPrice(new BigDecimal("999.99"));
        p1.setStockQuantity(50);
        p1 = productRepository.save(p1);

        p2 = new Products();
        p2.setSku("PROD-002");
        p2.setName("Samsung Galaxy S23");
        p2.setDescription("SAMSUNG Galaxy S23 Ultra Series AI Phone");
        p2.setCategory(ProductCategory.SMARTPHONES);
        p2.setBrand("Samsung");
        p2.setPrice(new BigDecimal("599.99"));
        p2.setStockQuantity(30);
        p2 = productRepository.save(p2);
    }

    @BeforeEach
    void setUp() {
        url = "http://localhost:" + port;
        setUpTestData();
    }

    @AfterEach
    void tearDown() {
        orderItemsRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Get all orders")
    void getAllOrders() {
        Orders order1 = new Orders();
        order1.setCustomer(c);
        order1.setStatus(OrderStatus.PENDING);
        order1.setTotalAmount(new BigDecimal("999.99"));
        order1.setOrderDate(LocalDateTime.now());
        orderRepository.save(order1);

        Orders order2 = new Orders();
        order2.setCustomer(c);
        order2.setStatus(OrderStatus.DELIVERED);
        order2.setTotalAmount(new BigDecimal("599.99"));
        order2.setOrderDate(LocalDateTime.now());
        orderRepository.save(order2);

        ResponseEntity<List<Orders>> response = restTemplate.exchange(
                url + "/api/orders/all",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Orders>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Orders> ordersList = response.getBody();
        assertThat(ordersList).isNotNull();
        assertThat(ordersList.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should create order successfully")
    void testCreateOrder() {
        OrderCreateDto orderDto = new OrderCreateDto();
        orderDto.setCustomerId(c.getCustomerId());
        orderDto.setOrderDate(LocalDateTime.now());

        List<OrderItemsDto> items = Arrays.asList(
                new OrderItemsDto(p1.getProductId(), 2),
                new OrderItemsDto(p2.getProductId(), 1)
        );
        orderDto.setOrderItems(items);

        ResponseEntity<Orders> response = restTemplate.postForEntity(
                url + "/api/orders/createOrder",
                orderDto,
                Orders.class
        );


        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCustomer().getCustomerId()).isEqualTo(c.getCustomerId());
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getBody().getTotalAmount()).isEqualByComparingTo(new BigDecimal("2599.97"));

        Products updatedProduct1 = productRepository.findById(p1.getProductId()).orElseThrow();
        Products updatedProduct2 = productRepository.findById(p2.getProductId()).orElseThrow();
        assertThat(updatedProduct1.getStockQuantity()).isEqualTo(48);
        assertThat(updatedProduct2.getStockQuantity()).isEqualTo(29);
    }

    @Test
    @DisplayName("Should update order status successfully")
    void testUpdateOrderStatus() {
        Orders o = new Orders();
        o.setCustomer(c);
        o.setStatus(OrderStatus.PENDING);
        o.setOrderDate(LocalDateTime.now());
        o.setTotalAmount(new BigDecimal("999.99"));

        Orders savedOrder = orderRepository.save(o);

        OrderStatus newStatus = OrderStatus.DELIVERED;

        HttpEntity<Orders> requestEntity = new HttpEntity<>(o);
        ResponseEntity<Orders> response = restTemplate.exchange(
                url + "/api/orders/orderId/"+savedOrder.getOrderId()+"/updateOrderStatus/" + newStatus,
                HttpMethod.PATCH,
                null,
                Orders.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("Should get orders by status successfully")
    void testGetOrdersByStatus() {
        Orders o1 = new Orders();
        o1.setCustomer(c);
        o1.setStatus(OrderStatus.PENDING);
        o1.setOrderDate(LocalDateTime.now());
        o1.setTotalAmount(new BigDecimal("999.99"));
        orderRepository.save(o1);

        Orders o2 = new Orders();
        o2.setCustomer(c);
        o2.setStatus(OrderStatus.PENDING);
        o2.setOrderDate(LocalDateTime.now());
        o2.setTotalAmount(new BigDecimal("590.99"));
        orderRepository.save(o2);

        Orders o3 = new Orders();
        o3.setCustomer(c);
        o3.setStatus(OrderStatus.DELIVERED);
        o3.setOrderDate(LocalDateTime.now());
        o3.setTotalAmount(new BigDecimal("1202.99"));
        orderRepository.save(o3);

        ResponseEntity<Orders[]> response = restTemplate.getForEntity(
                url + "/api/orders/orderStatus/" + OrderStatus.PENDING, Orders[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        Arrays.stream(response.getBody())
                .forEach(order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING));
    }

    @Test
    @DisplayName("Should get total spent by customer successfully")
    void testGetTotalSpentByCustomer() {

        Orders o1 = new Orders();
        o1.setCustomer(c);
        o1.setStatus(OrderStatus.DELIVERED);
        o1.setOrderDate(LocalDateTime.now());
        o1.setTotalAmount(new BigDecimal("999"));
        orderRepository.save(o1);

        Orders o2 = new Orders();
        o2.setCustomer(c);
        o2.setStatus(OrderStatus.PENDING);
        o2.setOrderDate(LocalDateTime.now());
        o2.setTotalAmount(new BigDecimal("590"));
        orderRepository.save(o2);

        Orders o3 = new Orders();
        o3.setCustomer(c);
        o3.setStatus(OrderStatus.DELIVERED);
        o3.setOrderDate(LocalDateTime.now());
        o3.setTotalAmount(new BigDecimal("1202"));
        orderRepository.save(o3);
        ResponseEntity<BigDecimal> response = restTemplate.getForEntity(
                url + "/api/orders/totalSpent/" + c.getCustomerId(), BigDecimal.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualByComparingTo(new BigDecimal("2201"));
    }

    @Test
    @DisplayName("Should get order count by customer successfully")
    void testGetOrderCountByCustomer() {

        Orders o1 = new Orders();
        o1.setCustomer(c);
        o1.setStatus(OrderStatus.DELIVERED);
        o1.setOrderDate(LocalDateTime.now());
        o1.setTotalAmount(new BigDecimal("990"));
        orderRepository.save(o1);

        Orders o2 = new Orders();
        o2.setCustomer(c);
        o2.setStatus(OrderStatus.PENDING);
        o2.setOrderDate(LocalDateTime.now());
        o2.setTotalAmount(new BigDecimal("200"));
        orderRepository.save(o2);

        Orders o3 = new Orders();
        o3.setCustomer(c);
        o3.setStatus(OrderStatus.DELIVERED);
        o3.setOrderDate(LocalDateTime.now());
        o3.setTotalAmount(new BigDecimal("300"));
        orderRepository.save(o3);


        ResponseEntity<Long> response = restTemplate.getForEntity(
                url + "/api/orders/orderCount/" + c.getCustomerId(), Long.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(2L);
    }
}
