package com.sqli.ecomAnalytics.Generators;

import com.sqli.ecomAnalytics.dto.OrderCreateDto;
import com.sqli.ecomAnalytics.entity.*;
import com.sqli.ecomAnalytics.generator.OrderDataGenerator;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.service.OrdersService;
import com.sqli.ecomAnalytics.service.ProductsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderGeneratorsTest {

    @Mock
    private OrdersService ordersService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductsService productsService;

    @InjectMocks
    private OrderDataGenerator dataGenerator;

    private List<Customers>  customers;
    private List<Products> products;


    private List<Customers> createMockCustomers() {
        return IntStream.range(1, 100000)
                .mapToObj(i -> {
                    Customers customer = new Customers();
                    customer.setCustomerId(i);
                    customer.setFirstName("Customer" + i);
                    customer.setLastName("Test");
                    customer.setEmail("customer" + i + "@test.com");
                    return customer;
                })
                .collect(Collectors.toList());
    }

    private List<Products> createMockProducts() {
        return List.of(
                createSmartphone(),
                createLaptop(),
                createTablet(),
                createGamingConsole()
        );
    }

    private Products createSmartphone() {
        Products product = new Products();
        product.setProductId(1);
        product.setName("iPhone 15 Pro");
        product.setCategory(ProductCategory.SMARTPHONES);
        product.setPrice(new BigDecimal("999.99"));
        product.setStockQuantity(50);
        return product;
    }

    private Products createLaptop() {
        Products product = new Products();
        product.setProductId(2);
        product.setName("MacBook Pro");
        product.setCategory(ProductCategory.LAPTOPS);
        product.setPrice(new BigDecimal("1999.99"));
        product.setStockQuantity(30);
        return product;
    }

    private Products createTablet() {
        Products product = new Products();
        product.setProductId(3);
        product.setName("iPad Air");
        product.setCategory(ProductCategory.TABLETS);
        product.setPrice(new BigDecimal("599.99"));
        product.setStockQuantity(40);
        return product;
    }

    private Products createGamingConsole() {
        Products product = new Products();
        product.setProductId(4);
        product.setName("PlayStation 5");
        product.setCategory(ProductCategory.GAMING_CONSOLES);
        product.setPrice(new BigDecimal("499.99"));
        product.setStockQuantity(20);
        return product;
    }

    private Products createAccessory(String type) {
        Products product = new Products();
        product.setProductId(5);
        product.setName("Phone " + type);
        product.setCategory(ProductCategory.ACCESSORIES);
        product.setPrice(new BigDecimal("29.99"));
        product.setStockQuantity(100);
        return product;
    }

    private Orders createMockOrder() {
        Orders order = new Orders();
        order.setOrderId(1);
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    @BeforeEach
    void setUp() {
        customers = createMockCustomers();
        products = createMockProducts();
    }

    @Test
    void generateOrdersForRange(){
        LocalDate startDate = LocalDate.of(2025, 8, 10);
        LocalDate endDate = LocalDate.of(2025, 8, 13);
        int baseVolume = 10;

        when(customerRepository.findAll()).thenReturn(customers);
        when(productsService.findAllProducts()).thenReturn(products);
        when(ordersService.createOrder(any(OrderCreateDto.class))).thenReturn(createMockOrder());

        dataGenerator.generateOrdersForDateRange(startDate, endDate, baseVolume);

        verify(ordersService, atLeast(30)).createOrder(any(OrderCreateDto.class));
        verify(ordersService, atLeast(1)).updateOrderStatus(anyInt(), any(OrderStatus.class));
    }

    @Test
    void generateOrdersBundle(){
        Products smartphone = createSmartphone();
        List<Products> accessories = List.of(
                createAccessory("case"),
                createAccessory("screen protector"),
                createAccessory("charger")
        );

        when(customerRepository.findAll()).thenReturn(customers);
        when(productsService.findAllProducts()).thenReturn(List.of(smartphone));
        when(productsService.searchProducts("case")).thenReturn(List.of(accessories.get(0)));
        when(productsService.searchProducts("screen protector")).thenReturn(List.of(accessories.get(1)));
        when(productsService.searchProducts("charger")).thenReturn(List.of(accessories.get(2)));
        when(ordersService.createOrder(any(OrderCreateDto.class))).thenReturn(createMockOrder());

        dataGenerator.generateOrdersForDateRange(LocalDate.now(), LocalDate.now(), 10);

        ArgumentCaptor<OrderCreateDto> orderCaptor = ArgumentCaptor.forClass(OrderCreateDto.class);
        verify(ordersService, atLeast(1)).createOrder(orderCaptor.capture());
        List<OrderCreateDto> capturedOrders = orderCaptor.getAllValues();

        boolean hasBundleOrder = capturedOrders.stream()
                .anyMatch(order -> order.getOrderItems().size() > 1);

        assertThat(hasBundleOrder).isTrue();

        System.out.println("Total orders created: " + capturedOrders.size());
        capturedOrders.forEach(order ->
                System.out.println("Order items count: " + order.getOrderItems().size())
        );
    }

    @Test
    void generateOrdersStatusTest() {
        when(customerRepository.findAll()).thenReturn(customers);
        when(productsService.findAllProducts()).thenReturn(products);
        when(ordersService.createOrder(any(OrderCreateDto.class)))
                .thenReturn(createMockOrder());

        dataGenerator.generateOrdersForDateRange(LocalDate.now(), LocalDate.now(), 1000);

        ArgumentCaptor<OrderStatus> statusCaptor = ArgumentCaptor.forClass(OrderStatus.class);
        verify(ordersService, atLeast(800)).updateOrderStatus(anyInt(), statusCaptor.capture());

        List<OrderStatus> statuses = statusCaptor.getAllValues();
        long deliveredCount = statuses.stream().filter(s -> s == OrderStatus.DELIVERED).count();
        long cancelledCount = statuses.stream().filter(s -> s == OrderStatus.CANCELLED).count();

        assertThat(deliveredCount).isGreaterThan(cancelledCount * 10);
    }
}
