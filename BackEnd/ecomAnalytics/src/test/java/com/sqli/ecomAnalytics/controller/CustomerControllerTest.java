package com.sqli.ecomAnalytics.controller;

import com.sqli.ecomAnalytics.configuration.SecurityConfig;
import com.sqli.ecomAnalytics.entity.Customers;
import com.sqli.ecomAnalytics.entity.Segments;
import com.sqli.ecomAnalytics.service.CustomersService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomersController.class)
@Import(SecurityConfig.class)
public class CustomerControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomersService customersService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getAllCustomers_ReturnsList() throws Exception {
        Customers c = new Customers();
        c.setCustomerId(1);
        c.setFirstName("John");
        c.setLastName("Doe");

        when(customersService.findAllCustomers()).thenReturn(List.of(c));

        mockMvc.perform(get("/api/customers/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCustomerById_Success() throws Exception {
        Customers c = new Customers();
        c.setCustomerId(1);
        c.setFirstName("Alice");

        when(customersService.findCustomerById(1)).thenReturn(c);

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1));
    }

    @Test
    void registerCustomer_Success() throws Exception {
        Customers c = new Customers();
        c.setCustomerId(1);

        when(customersService.registerCustomer(any())).thenReturn(c);

        mockMvc.perform(post("/api/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1));
    }

    @Test
    void updateCustomer() throws Exception {
        Customers c = new Customers();
        c.setCustomerId(1);
        c.setFirstName("Updated");

        when(customersService.updateCustomer(eq(1), any())).thenReturn(c);

        mockMvc.perform(patch("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }


    @Test
    void getCustomerByCustomerCode() throws Exception {
        Customers c = new Customers();
        c.setCustomerCode("CUST-001");

        when(customersService.findCustomerByCustomerCode("CUST-001")).thenReturn(c);

        mockMvc.perform(get("/api/customers/customerCode/CUST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerCode").value("CUST-001"));
    }

    @Test
    void getCustomerByFirstNameAndLastName() throws Exception {
        Customers c = new Customers();
        c.setFirstName("Alice");

        when(customersService.findCustomerByFirstNameAndLastName("Alice", "Smith"))
                .thenReturn(Optional.of(c));

        mockMvc.perform(get("/api/customers/firstName/Alice/lastName/Smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    @Test
    void getHighestPaying() throws Exception {
        when(customersService.findHighestPayingCustomers()).thenReturn(List.of(new Customers()));

        mockMvc.perform(get("/api/customers/highestPaying"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getSegmentCustomersById() throws Exception {
        Customers c = new Customers();
        when(customersService.findCustomerSegment(1)).thenReturn(Optional.of(c));

        mockMvc.perform(get("/api/customers/customerSegment/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getSegmentCustomersBySegment() throws Exception {
        when(customersService.findBySegments(Segments.CHAMPION)).thenReturn(List.of(new Customers()));

        mockMvc.perform(get("/api/customers/segment/CHAMPION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getTopCustomerByTotalSpent() throws Exception {
        Customers c = new Customers();
        c.setCustomerId(1);
        when(customersService.findTopCustomerByTotalSpent()).thenReturn(c);

        mockMvc.perform(get("/api/customers/topCustomerByTotalSpent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1));
    }
}
