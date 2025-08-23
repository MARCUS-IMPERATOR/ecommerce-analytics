package com.sqli.ecomAnalytics.service;

import com.sqli.ecomAnalytics.entity.CustomerSegments;
import com.sqli.ecomAnalytics.entity.Customers;
import com.sqli.ecomAnalytics.entity.Segments;
import com.sqli.ecomAnalytics.exceptions.CustomerNotFoundException;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.CustomerSegmentsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class CustomerSegmentsService {
    private final CustomerSegmentsRepository customerSegmentsRepository;
    private final CustomerRepository customerRepository;

    public CustomerSegmentsService(CustomerSegmentsRepository customerSegmentsRepository, CustomerRepository customerRepository) {
        this.customerSegmentsRepository = customerSegmentsRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public void initializeCustomerSegments(int customerId){
        if (customerSegmentsRepository.findById(customerId).isPresent()) {
            System.out.println("Customer segment with id " + customerId + " already exists");
            return;
        }
        Customers c = customerRepository.findById(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer with id " + customerId + " not found"));

        CustomerSegments cs = new CustomerSegments();
        cs.setCustomerId(customerId);
        cs.setCustomer(c);
        cs.setRecency(0);
        cs.setFrequency(BigDecimal.ZERO);
        cs.setMonetary(BigDecimal.ZERO);
        cs.setSegmentLabel(Segments.NEW);
        cs.setSegmentScore(0);
        cs.setLastCalculated(null);

        customerSegmentsRepository.save(cs);
    }

    @Transactional
    public void updateRecency(int customerId, LocalDateTime orderDate) {
        CustomerSegments cs = customerSegmentsRepository.findById(customerId)
                .orElseGet(()-> {
                    initializeCustomerSegments(customerId);
                    return customerSegmentsRepository.findById(customerId).get();
                }
        );
        LocalDateTime now = LocalDateTime.now();
        int newRecency = (int) ChronoUnit.DAYS.between(orderDate, now);

        cs.setRecency(newRecency);
        customerSegmentsRepository.save(cs);
    }

    @Transactional(readOnly = true)
    public CustomerSegments getCustomerSegments(int customerId) {
        return customerSegmentsRepository.findById(customerId).orElse(null);
    }
}
