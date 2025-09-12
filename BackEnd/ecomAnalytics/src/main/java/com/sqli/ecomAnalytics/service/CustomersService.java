package com.sqli.ecomAnalytics.service;

import com.sqli.ecomAnalytics.dto.CustomerRegistrationDto;
import com.sqli.ecomAnalytics.dto.CustomerUpdateDto;
import com.sqli.ecomAnalytics.entity.Customers;
import com.sqli.ecomAnalytics.entity.Segments;
import com.sqli.ecomAnalytics.exceptions.CustomerAlreadyExistsException;
import com.sqli.ecomAnalytics.exceptions.CustomerNotFoundException;
import com.sqli.ecomAnalytics.exceptions.InvalidCustomerDataException;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class CustomersService {
    private final CustomerRepository customerRepository;
    private final MLEventPublisher mlEventPublisher;

    public CustomersService(CustomerRepository customerRepository, MLEventPublisher mlEventPublisher) {
        this.customerRepository = customerRepository;
        this.mlEventPublisher = mlEventPublisher;
    }

    @Caching(evict = {
            @CacheEvict(value = "allCustomersCache", key = "'customers:all'"),
            @CacheEvict(value = "highestPayingCustomersCache", key = "'customers:highestPayers'")
    })
    @Transactional
    public Customers registerCustomer(CustomerRegistrationDto customer) {

        if (customerRepository.findByEmail(customer.getEmail()).isPresent()) {
            throw new CustomerAlreadyExistsException("Customer with email"+ customer.getEmail() +"already exists");
        }
        if (customer.getRegisterDate() == null) {
            customer.setRegisterDate(LocalDateTime.now());
        }
        String customerCode = generateCustomerCode(customer.getFirstName(), customer.getLastName(), LocalDateTime.now());

        Customers c = new Customers();

        c.setCustomerCode(customerCode);
        c.setFirstName(customer.getFirstName());
        c.setLastName(customer.getLastName());
        c.setAge(customer.getAge());
        c.setCountry(customer.getCountry());
        c.setEmail(customer.getEmail());
        c.setPhone(customer.getPhone());
        c.setOrderCount(0);
        c.setTotalSpent(BigDecimal.ZERO);
        c.setRegistrationDate(customer.getRegisterDate());

        Customers savedCustomer = customerRepository.save(c);

        mlEventPublisher.publishCustomerCreated(savedCustomer.getCustomerId());
        return savedCustomer;
    }

    @Caching(evict = {
            @CacheEvict(value = "customerProfileCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).customerIdKey(#customerId)"),
            @CacheEvict(value = "customerCodeCache", allEntries = true),
            @CacheEvict(value = "allCustomersCache", key = "'customers:all'"),
            @CacheEvict(value = "highestPayingCustomersCache", key = "'customers:highestPayers'")
    })
    @Transactional
    public Customers updateCustomer(int customerId, CustomerUpdateDto customer) {
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new InvalidCustomerDataException("Customer with id " + customerId + " does not exist");
        }
        Customers c = customerRepository.findById(customerId).get();
        c.setFirstName(customer.getFirstName());
        c.setLastName(customer.getLastName());
        c.setCountry(customer.getCountry());
        c.setAge(c.getAge());
        c.setEmail(customer.getEmail());
        c.setPhone(customer.getPhone());

        Customers updatedCustomer = customerRepository.save(c);

        mlEventPublisher.publishCustomerUpdated(updatedCustomer.getCustomerId());

        return updatedCustomer;
    }

    private String generateCustomerCode(String firstName, String lastName, LocalDateTime registrationDate) {
        String firstNameCode = firstName.length() >= 3 ?
                firstName.substring(0, 3).toUpperCase() :
                firstName.toUpperCase();
        String lastNameCode = lastName.length() >= 3 ?
                lastName.substring(0, 3).toUpperCase() :
                lastName.toUpperCase();
        String dateCode = registrationDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomCode = String.format("%03d", new Random().nextInt(1000));

        return "CUST" + firstNameCode + lastNameCode + dateCode + randomCode;
    }


    @Cacheable(value = "customerProfileCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).customerIdKey(#customerId)")
    @Transactional(readOnly = true)
    public Customers findCustomerById(int customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID: " + customerId + " not found"));
    }

//    @Cacheable(value = "allCustomersCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).allCustomersKey()")
//    @Transactional(readOnly = true)
    public List<Customers> findAllCustomers() {
        return customerRepository.findAll();
    }

    @Cacheable(value = "customerProfileCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).customerCodeKey(#customerCode)")
    @Transactional(readOnly = true)
    public Customers findCustomerByCustomerCode(String customerCode) {
        if (customerRepository.findByCustomerCode(customerCode).isEmpty()) {
            throw new CustomerNotFoundException("Customer with code " + customerCode + " not found");
        }
        return customerRepository.findByCustomerCode(customerCode).get();
    }

    @Transactional(readOnly = true)
    public Optional<Customers> findCustomerByFirstNameAndLastName(String firstName, String lastName) {
        if (customerRepository.findByFirstNameAndLastName(firstName, lastName).isEmpty()) {
            throw new CustomerNotFoundException("Customer with name " + firstName + " " + lastName + " not found");
        }
        return customerRepository.findByFirstNameAndLastName(firstName,lastName);
    }

    @Cacheable(value = "highestPayingCustomersCache",key = "'customers:highestPaying'")
    @Transactional(readOnly = true)
    public List<Customers> findHighestPayingCustomers() {
        BigDecimal minSpent = new BigDecimal("10");
        return customerRepository.findHighSpendingCustomers(minSpent);
    }

    @Transactional(readOnly = true)
    public Optional<Customers> findCustomerSegment(int customerId) {
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException("Customer with id " + customerId + " not found");
        }
        return customerRepository.findWithSegment(customerId);
    }

    @Transactional(readOnly = true)
    public List<Customers> findBySegments(Segments segment) {
        return customerRepository.findCustomersBySegment(segment);
    }

    @Transactional(readOnly = true)
    public Customers findTopCustomerByTotalSpent() {
        return customerRepository.findTopByOrderByTotalSpentDesc();
    }

    public static BigDecimal calculateLTV(Customers customer) {

        BigDecimal totalSpent = customer.getTotalSpent();
        Integer orderCount = customer.getOrderCount();

        if (orderCount == 0 || totalSpent == null || totalSpent.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal avgPurchaseValue = totalSpent.divide(BigDecimal.valueOf(orderCount), 2, BigDecimal.ROUND_HALF_UP);

        long yearsActive = ChronoUnit.YEARS.between(customer.getRegistrationDate(), LocalDateTime.now());
        if (yearsActive == 0) {
            yearsActive = 1;
        }

        double purchaseFrequency = (double) orderCount / yearsActive;

        long lifespan = yearsActive;

        return avgPurchaseValue.multiply(BigDecimal.valueOf(purchaseFrequency))
                .multiply(BigDecimal.valueOf(lifespan));
    }
}
