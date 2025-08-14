package com.sqli.ecomAnalytics.controller;

import com.sqli.ecomAnalytics.dto.OrderCreateDto;
import com.sqli.ecomAnalytics.entity.OrderStatus;
import com.sqli.ecomAnalytics.entity.Orders;
import com.sqli.ecomAnalytics.service.OrdersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Orders operations")
public class OrdersController {
    private final OrdersService ordersService;

    public OrdersController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @Operation(summary = "Get a list of all Orders",responses = {
            @ApiResponse(responseCode = "200",description = "List of orders returned successfully")
    })
    @Transactional
    @GetMapping("/all")
    public ResponseEntity<List<Orders>> getAllOrders() {
        List<Orders> orders = ordersService.getAllOrders();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @Operation(summary = "Create an order",responses = {
            @ApiResponse(responseCode = "200",description = "Order created successfully"),
            @ApiResponse(responseCode = "400",description = "Invalid data"),
            @ApiResponse(responseCode = "404",description = "Customer not found")
    })
    @PostMapping("/createOrder")
    public ResponseEntity<Orders> createOrder(
            @Parameter(description = "Order creation data",required = true)
            @RequestBody OrderCreateDto orders) {
        Orders order = ordersService.createOrder(orders);
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @Operation(summary = "Update an order status",responses = {
            @ApiResponse(responseCode = "200",description = "Order status updated successfully"),
            @ApiResponse(responseCode = "404",description = "Order not found"),
            @ApiResponse(responseCode = "400",description = "Invalid data")
    } )
    @PatchMapping("/orderId/{orderId}/updateOrderStatus/{status}")
    public  ResponseEntity<Orders> updateOrderStatus(
            @Parameter(description = "Order Id")
            @PathVariable int orderId,
            @Parameter(description = "new Order status")
            @PathVariable OrderStatus status) {
        Orders orders = ordersService.updateOrderStatus(orderId, status);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @Operation(summary = "Get a list of orders by a status",responses = {
            @ApiResponse(responseCode = "200",description = "List of orders with a specific status returned successfully")
    })
    @GetMapping("/orderStatus/{status}")
    public ResponseEntity<List<Orders>> getByOrderStatus(
            @Parameter(description = "Order status")
            @PathVariable OrderStatus status) {
        List<Orders> orders = ordersService.findByStatus(status);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @Operation(summary = "Get the total amount spent by a customer",responses = {
            @ApiResponse(responseCode = "200",description = "Total amount spent is returned"),
            @ApiResponse(responseCode = "404",description = "Customer not found"),
    })
    @GetMapping("/totalSpent/{customerId}")
    public ResponseEntity<Optional<BigDecimal>> getTotalSpentByCustomer(@PathVariable int customerId) {
        Optional<BigDecimal> totalSpent = ordersService.getTotalSpentByCustomer(customerId);
        return new ResponseEntity<>(totalSpent, HttpStatus.OK);
    }

    @GetMapping("/orderCount/{customerId}")
    public ResponseEntity<Integer> getOrderCountByCustomer (@PathVariable int customerId) {
        return ResponseEntity.ok(ordersService.getOrderCountByCustomer(customerId));
    }
}
