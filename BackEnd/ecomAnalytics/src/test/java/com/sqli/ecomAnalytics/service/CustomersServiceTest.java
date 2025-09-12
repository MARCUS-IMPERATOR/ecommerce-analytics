package com.sqli.ecomAnalytics.service;

import com.sqli.ecomAnalytics.dto.CustomerRegistrationDto;
import com.sqli.ecomAnalytics.dto.CustomerUpdateDto;
import com.sqli.ecomAnalytics.entity.Customers;
import com.sqli.ecomAnalytics.entity.Segments;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class CustomersServiceTest {
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private MLEventPublisher mlEventPublisher;

    @InjectMocks
    private CustomersService customersService;

    private Customers c;

    @BeforeEach
    void setUp() {
        c = new Customers();
        c.setCustomerId(1);
        c.setEmail("test@example.com");
        c.setFirstName("John");
        c.setLastName("Doe");
        c.setTotalSpent(new BigDecimal("1000"));
        c.setOrderCount(10);
        c.setRegistrationDate(LocalDateTime.now().minusYears(2));
    }

    @Test
    void registerCustomer() {
        CustomerRegistrationDto dto = new CustomerRegistrationDto("John", "Doe",20,"USA","test@example.com", "12345678",null);

        when(customerRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customers.class))).thenAnswer(inv -> inv.getArgument(0));

        Customers saved = customersService.registerCustomer(dto);

        doNothing().when(mlEventPublisher).publishCustomerCreated(anyInt());

        assertEquals(dto.getEmail(), saved.getEmail());
        assertTrue(saved.getCustomerCode().startsWith("CUSTJOHDOE"));
    }


    @Test
    void updateCustomer() {
        CustomerUpdateDto dto = new CustomerUpdateDto("Jane", "Smith", 12,"Morocco","new@example.com", "0000000");
        when(customerRepository.findById(1)).thenReturn(Optional.of(c));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(mlEventPublisher).publishCustomerUpdated(anyInt());

        Customers updated = customersService.updateCustomer(1, dto);

        assertEquals("Jane", updated.getFirstName());
    }


    @Test
    void findCustomerById() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(c));

        Customers found = customersService.findCustomerById(1);

        assertEquals(1, found.getCustomerId());
    }

    @Test
    void findAllCustomers() {
        when(customerRepository.findAll()).thenReturn(List.of(c));

        List<Customers> customers = customersService.findAllCustomers();

        assertEquals(1, customers.size());
    }


    @Test
    void findCustomerByCustomerCode() {
        when(customerRepository.findByCustomerCode("CUST123")).thenReturn(Optional.of(c));

        Customers found = customersService.findCustomerByCustomerCode("CUST123");

        assertEquals(c.getCustomerId(), found.getCustomerId());
    }

    @Test
    void findCustomerByFirstNameAndLastName() {
        when(customerRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(c));

        Optional<Customers> found = customersService.findCustomerByFirstNameAndLastName("John", "Doe");

        assertTrue(found.isPresent());
    }

    @Test
    void findHighestPayingCustomers() {
        when(customerRepository.findHighSpendingCustomers(new BigDecimal("10"))).thenReturn(List.of(c));

        List<Customers> result = customersService.findHighestPayingCustomers();

        assertFalse(result.isEmpty());
    }


    @Test
    void findCustomerSegment() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(c));
        when(customerRepository.findWithSegment(1)).thenReturn(Optional.of(c));

        Optional<Customers> result = customersService.findCustomerSegment(1);

        assertTrue(result.isPresent());
    }


    @Test
    void findBySegments() {
        when(customerRepository.findCustomersBySegment(Segments.CHAMPION)).thenReturn(List.of(c));

        List<Customers> result = customersService.findBySegments(Segments.CHAMPION);

        assertFalse(result.isEmpty());
    }

    @Test
    void findTopCustomerByTotalSpent() {
        when(customerRepository.findTopByOrderByTotalSpentDesc()).thenReturn(c);

        Customers result = customersService.findTopCustomerByTotalSpent();

        assertEquals(c.getCustomerId(), result.getCustomerId());
    }
}
