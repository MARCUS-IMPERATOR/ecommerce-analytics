package com.sqli.ecomAnalytics.controller;

import com.sqli.ecomAnalytics.configuration.SecurityConfig;
import com.sqli.ecomAnalytics.dto.OrderCreateDto;
import com.sqli.ecomAnalytics.entity.OrderStatus;
import com.sqli.ecomAnalytics.entity.Orders;
import com.sqli.ecomAnalytics.service.OrdersService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

@WebMvcTest(OrdersController.class)
@Import(SecurityConfig.class)
public class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdersService ordersService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;


    @Test
    void getAllOrders() throws Exception {

        Orders order1 = new Orders();
        order1.setOrderId(1);
        order1.setTotalAmount(new BigDecimal("100"));
        order1.setStatus(OrderStatus.PENDING);
        order1.setOrderDate(LocalDateTime.now());

        Orders order2 = new Orders();
        order2.setOrderId(2);
        order2.setTotalAmount(new BigDecimal("200"));
        order2.setStatus(OrderStatus.PENDING);
        order2.setOrderDate(LocalDateTime.now());

        when(ordersService.getAllOrders()).thenReturn(List.of(order1, order2));

        mockMvc.perform(get("/api/orders/all"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    public void createOrder() throws Exception {
        OrderCreateDto dto = new OrderCreateDto();
        Orders order = new Orders();
        order.setOrderId(1);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("100"));

        when(ordersService.createOrder(any(OrderCreateDto.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders/createOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"productIds\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1));

    }

    @Test
    void updateOrderStatus() throws Exception {
        Orders order = new Orders();
        order.setOrderId(1);
        order.setStatus(OrderStatus.DELIVERED);

        when(ordersService.updateOrderStatus(anyInt(), eq(OrderStatus.DELIVERED))).thenReturn(order);

        mockMvc.perform(patch("/api/orders/orderId/1/updateOrderStatus/DELIVERED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void getByOrderStatus() throws Exception {
        Orders order = new Orders();
        order.setOrderId(1);
        order.setStatus(OrderStatus.PENDING);

        when(ordersService.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/orderStatus/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getTotalSpentByCustomer() throws Exception {
        when(ordersService.getTotalSpentByCustomer(1)).thenReturn(Optional.of(BigDecimal.valueOf(300)));

        mockMvc.perform(get("/api/orders/totalSpent/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(300));
    }

    @Test
    void getOrderCountByCustomer_ValidCustomer() throws Exception {
        when(ordersService.getOrderCountByCustomer(1)).thenReturn(3);

        mockMvc.perform(get("/api/orders/orderCount/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }
}
