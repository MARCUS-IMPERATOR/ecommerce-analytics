package com.sqli.ecomAnalytics.service;

import com.sqli.ecomAnalytics.dto.OrderCreateDto;
import com.sqli.ecomAnalytics.dto.OrderItemsDto;
import com.sqli.ecomAnalytics.entity.*;
import com.sqli.ecomAnalytics.exceptions.CustomerNotFoundException;
import com.sqli.ecomAnalytics.exceptions.OrderNotFoundException;
import com.sqli.ecomAnalytics.exceptions.ProductNotFoundException;
import com.sqli.ecomAnalytics.exceptions.ProductStockInsufficient;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.OrderItemsRepository;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrdersService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemsRepository orderItemsRepository;

    public OrdersService(OrderRepository orderRepository, ProductRepository productRepository,
                         CustomerRepository customerRepository, OrderItemsRepository orderItemsRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderItemsRepository = orderItemsRepository;
    }


    @Caching(evict = {
            @CacheEvict(value = "customerSpentCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).customerSpentKey(#order.customerId)"),
            @CacheEvict(value = "customerOrderCountCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).customerOrderCountKey(#order.customerId)")
    })
    @Transactional
    public Orders createOrder(OrderCreateDto order) {
        Customers customer = customerRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemsDto itemDto : order.getOrderItems()) {
            Products product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));

            if (product.getStockQuantity() < itemDto.getQuantity()) {
                throw new ProductStockInsufficient("Not enough stock for product " + product.getSku());
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            product.setStockQuantity(product.getStockQuantity() - itemDto.getQuantity());
            productRepository.save(product);
        }


        Orders o = new Orders();

        Optional<LocalDateTime> lastOrder = Optional.ofNullable(customer.getLastOrderDate());

        if (lastOrder.isEmpty() || lastOrder.get().isBefore(order.getOrderDate())) {
            customer.setLastOrderDate(order.getOrderDate());
        }

        o.setCustomer(customer);
        o.setStatus(OrderStatus.PENDING);
        o.setTotalAmount(totalAmount);
        o.setOrderDate(order.getOrderDate());

        Orders savedOrder = orderRepository.save(o);

        List<OrderItems> orderItemsList = new ArrayList<>();

        for (OrderItemsDto itemDto : order.getOrderItems()) {
            Products product = productRepository.findById(itemDto.getProductId()).get();

            OrderItems orderItem = new OrderItems();
            orderItem.setOrderId(savedOrder.getOrderId());
            orderItem.setOrder(savedOrder);
            orderItem.setProductId(itemDto.getProductId());
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setUnitPrice(product.getPrice());

            OrderItems savedOrderItem = orderItemsRepository.save(orderItem);
            orderItemsList.add(savedOrderItem);
        }

        savedOrder.setOrderItems(orderItemsList);
        return savedOrder;
    }


    @Caching(evict = {
            @CacheEvict(value = "customerSpentCache", allEntries = true),
            @CacheEvict(value = "customerOrderCountCache", allEntries = true)
    })
    @Transactional
    public Orders updateOrderStatus(int orderId, OrderStatus orderStatus) {
        Orders updatedOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID:" + orderId + " not found"));

        updatedOrder.setStatus(orderStatus);
        return orderRepository.save(updatedOrder);
    }

    @Transactional(readOnly = true)
    public List<Orders> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Orders> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Cacheable(value = "customerSpentCache",key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).customerSpentKey(#customerId)"
    )
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getTotalSpentByCustomer(int customerId) {
        BigDecimal totalAmount = orderRepository.getTotalSpentByCustomer(customerId).
                orElseThrow(() ->
                        new CustomerNotFoundException("Customer not found"));
        return Optional.of(totalAmount);
    }

    @Cacheable(value = "customerOrderCountCache",key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).customerOrderCountKey(#customerId)"
    )
    @Transactional(readOnly = true)
    public int getOrderCountByCustomer(int customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException("Customer not found");
        }
        return orderRepository.getOrderCountByCustomer(customerId);
    }
}
