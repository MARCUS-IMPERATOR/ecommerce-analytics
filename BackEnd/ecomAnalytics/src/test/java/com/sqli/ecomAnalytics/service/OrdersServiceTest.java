package com.sqli.ecomAnalytics.service;

import com.sqli.ecomAnalytics.dto.OrderCreateDto;
import com.sqli.ecomAnalytics.dto.OrderItemsDto;
import com.sqli.ecomAnalytics.entity.*;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.OrderItemsRepository;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class OrdersServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrderItemsRepository orderItemsRepository;
    @Mock
    private MLEventPublisher mlEventPublisher;
    @Mock
    private CustomerSegmentsService customerSegmentsService;
    @InjectMocks
    private OrdersService ordersService;

    @Test
    void createOrder() {
        Customers customer = new Customers();
        customer.setCustomerId(1);
        customer.setOrderCount(0);
        customer.setTotalSpent(BigDecimal.ZERO);

        Products product = new Products();
        product.setProductId(10);
        product.setSku("SKU-001");
        product.setPrice(BigDecimal.valueOf(50));
        product.setStockQuantity(10);

        OrderItemsDto itemDto = new OrderItemsDto();
        itemDto.setProductId(10);
        itemDto.setQuantity(2);

        OrderCreateDto orderDto = new OrderCreateDto();
        orderDto.setCustomerId(1);
        orderDto.setOrderItems(List.of(itemDto));

        Orders savedOrder = new Orders();
        savedOrder.setOrderId(1);
        savedOrder.setCustomer(customer);
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setTotalAmount(BigDecimal.valueOf(100));
        savedOrder.setOrderDate(LocalDateTime.now());

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Orders.class))).thenReturn(savedOrder);
        when(orderItemsRepository.save(any(OrderItems.class))).thenAnswer(i -> i.getArgument(0));


        Orders created = ordersService.createOrder(orderDto);

//        doNothing().when(mlEventPublisher).publishOrderCreated(anyInt(), anyInt());

        assertEquals(OrderStatus.PENDING, created.getStatus());
        assertEquals(BigDecimal.valueOf(100), created.getTotalAmount());
        assertEquals(8, product.getStockQuantity());

        verify(orderItemsRepository, times(1)).save(any(OrderItems.class));
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void updateOrderStatus() {
        Orders order = new Orders();
        order.setOrderId(1);

        Orders existing = new Orders();
        existing.setOrderId(1);
        existing.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Orders updated = ordersService.updateOrderStatus(order.getOrderId(), OrderStatus.DELIVERED);
        assertEquals(OrderStatus.DELIVERED, updated.getStatus());
    }

    @Test
    void getAllOrders() {
        Orders o1 = new Orders();
        Orders o2 = new Orders();

        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));

        List<Orders> result = ordersService.getAllOrders();
        assertEquals(2, result.size());
    }

    @Test
    void findByStatus() {
        Orders o1 = new Orders();
        o1.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findByStatus(OrderStatus.DELIVERED)).thenReturn(List.of(o1));

        List<Orders> result = ordersService.findByStatus(OrderStatus.DELIVERED);
        assertEquals(1, result.size());
        assertEquals(OrderStatus.DELIVERED, result.getFirst().getStatus());
    }

    @Test
    void getTotalSpentByCustomer() {
        when(orderRepository.getTotalSpentByCustomer(1)).thenReturn(Optional.of(BigDecimal.valueOf(250)));

        Optional<BigDecimal> result = ordersService.getTotalSpentByCustomer(1);

        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(250), result.get());
    }

    @Test
    void getOrderCountByCustomer() {
        when(customerRepository.existsById(1)).thenReturn(true);
        when(orderRepository.getOrderCountByCustomer(1)).thenReturn(5);

        int count = ordersService.getOrderCountByCustomer(1);
        assertEquals(5, count);
    }
}

