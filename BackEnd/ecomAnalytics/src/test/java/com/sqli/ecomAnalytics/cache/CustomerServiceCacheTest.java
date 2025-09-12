package com.sqli.ecomAnalytics.cache;

import com.sqli.ecomAnalytics.dto.CustomerUpdateDto;
import com.sqli.ecomAnalytics.entity.Customers;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.service.CustomersService;
import com.sqli.ecomAnalytics.util.RedisCacheKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public class CustomerServiceCacheTest extends  BaseCacheTest{

    @MockitoBean
    private CustomerRepository customerRepository;
    @Autowired
    private CustomersService customersService;

    private Customers c;
    private List<Customers> customers;

    @BeforeEach
    void setUp() {
        clearAllCaches();
        c = new Customers();
        c.setCustomerId(1);
        c.setFirstName("John");
        c.setLastName("Doe");
        c.setEmail("john@email.com");
        c.setPhone("123456789");
        c.setCountry("Morocco");
        c.setAge(24);
        c.setTotalSpent(new BigDecimal("1000"));
        c.setOrderCount(10);
        c.setRegistrationDate(LocalDateTime.now().minusYears(2));

        customers = Arrays.asList(c);
    }

    @Test
    @DisplayName("Cache customer by id on first call")
    void getCustomerById() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(c));

        Customers r1 = customersService.findCustomerById(1);

        verify(customerRepository, times(1)).findById(1);

        assertThat(r1).isEqualTo(c);

        Customers r2 = customersService.findCustomerById(1);

        verify(customerRepository, times(1)).findById(1);

        String key = RedisCacheKeys.customerIdKey(1);
        assertCache("customerProfileCache",key,c);
    }

//    @Test
//    @DisplayName("Should cache all customers on the first call")
//    void getAllCustomers() {
//        when(customerRepository.findAll()).thenReturn(customers);
//
//        List<Customers> r1 = customersService.findAllCustomers();
//
//        verify(customerRepository, times(1)).findAll();
//        assertThat(r1.size()).isEqualTo(1);
//
//        List<Customers> r2 = customersService.findAllCustomers();
//
//        verify(customerRepository, times(1)).findAll();
//        assertThat(r2.size()).isEqualTo(1);
//
//        assertCache("allCustomersCache","customers:all",customers);
//    }

//    @Test
//    @DisplayName("Should evict cache when registering new customer")
//    void evictCacheRegisterCustomer() {
//        when(customerRepository.findAll()).thenReturn(customers);
//        when(customerRepository.findHighSpendingCustomers(any())).thenReturn(customers);
//
//        customersService.findAllCustomers();
//        customersService.findHighestPayingCustomers();
//
//        assertCache("allCustomersCache","customers:all",customers);
//
//        CustomerRegistrationDto registrationDto = new CustomerRegistrationDto();
//        registrationDto.setFirstName("Jane");
//        registrationDto.setLastName("Doe");
//        registrationDto.setAge(22);
//        registrationDto.setPhone("123456789");
//        registrationDto.setCountry("France");
//
//        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
//        when(customerRepository.save(any())).thenReturn(c);
//
//        customersService.registerCustomer(registrationDto);
//
//        assertEvict("allCustomersCache","customers:all");
//        assertEvict("highestPayingCustomersCache","customers:highestPayers");
//    }

    @Test
    @DisplayName("Should evict specific customer cache when updating")
    void evictSpecificCustomerCacheOnUpdate() {

        when(customerRepository.findById(1)).thenReturn(Optional.of(c));
        when(customerRepository.findAll()).thenReturn(customers);

        customersService.findCustomerById(1);
        customersService.findAllCustomers();

        String customerKey = "customer:profile:1";
        assertCache("customerProfileCache", customerKey, c);

        CustomerUpdateDto updateDto = new CustomerUpdateDto();
        updateDto.setFirstName("John Updated");
        updateDto.setLastName("Doe");
        updateDto.setEmail("john.updated@email.com");
        updateDto.setAge(30);
        updateDto.setCountry("Canada");
        updateDto.setPhone("987654321");

        when(customerRepository.save(any(Customers.class))).thenReturn(c);

        customersService.updateCustomer(1, updateDto);

        assertEvict("customerProfileCache", customerKey);
        assertEvict("allCustomersCache", "customers:all");
    }
}
