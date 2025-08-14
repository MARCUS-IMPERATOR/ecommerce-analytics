package com.sqli.ecomAnalytics.Integration;

import com.sqli.ecomAnalytics.entity.CustomerSegments;
import com.sqli.ecomAnalytics.entity.Customers;
import com.sqli.ecomAnalytics.entity.Segments;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@Testcontainers
public class CustomerAnalyticsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("ecom_analytics_test")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        registry.add("spring.flyway.url", postgresContainer::getJdbcUrl);
        registry.add("spring.flyway.user", postgresContainer::getUsername);
        registry.add("spring.flyway.password", postgresContainer::getPassword);
    }

    @Autowired
    private CustomerRepository customerRepository;

    @AfterEach
    void cleanup() {
        customerRepository.deleteAll();
    }

    private Customers createCustomerWithSegment(String code, String email, Segments seg) {
        Customers customer = new Customers();
        customer.setCustomerCode(code);
        customer.setFirstName("First");
        customer.setLastName("Last");
        customer.setEmail(email);
        customer.setPhone("000000");
        customer.setAge(30);
        customer.setCountry("Morocco");
        customer.setRegistrationDate(LocalDateTime.now());
        customer.setTotalSpent(BigDecimal.ZERO);
        customer.setOrderCount(0);

        CustomerSegments cs = new CustomerSegments();
        cs.setSegmentLabel(seg);

        cs.setFrequency(BigDecimal.ZERO);
        cs.setMonetary(BigDecimal.ZERO);
        cs.setRecency(0);
        cs.setSegmentScore(0);
        cs.setLastCalculated(LocalDateTime.now());

        cs.setCustomer(customer);
        customer.setCustomerSegment(cs);

        return customer;
    }

    @Test
    void getCustomerCountBySegments(){
        Customers champ  = createCustomerWithSegment("CUST-Champ","cust1@email.com",Segments.CHAMPION);
        Customers newCust = createCustomerWithSegment("CUST-New","cust2@email.com",Segments.NEW);

        customerRepository.saveAndFlush(champ);
        customerRepository.saveAndFlush(newCust);

        List<Object[]> r = customerRepository.getCustomerCountBySegment();

        assertThat(r).isNotNull();
        assertThat(r).hasSize(2);

        Map<Segments, Long> segmentCounts = r.stream()
                .collect(Collectors.toMap(
                        row -> (Segments) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        assertThat(segmentCounts.get(Segments.CHAMPION)).isEqualTo(1L);
        assertThat(segmentCounts.get(Segments.NEW)).isEqualTo(1L);
    }

    @Test
    void findHighSpendingCustomers(){
        BigDecimal threshold = new BigDecimal("1000.00");
        Customers highSpender = createCustomerWithSegment("CUST-HighSpend","highspend@email.com",Segments.CHAMPION);
        highSpender.setTotalSpent(new BigDecimal("1500.00"));

        Customers lowSpender = createCustomerWithSegment("CUST-LowSpend","lowspend@email.com",Segments.NEW);
        lowSpender.setTotalSpent(new BigDecimal("100.00"));

        customerRepository.saveAndFlush(highSpender);
        customerRepository.saveAndFlush(lowSpender);

        List<Customers> r = customerRepository.findHighSpendingCustomers(threshold);

        assertThat(r).hasSize(1);
        assertThat(r.get(0).getCustomerCode()).isEqualTo("CUST-HighSpend");
        assertThat(r.get(0).getTotalSpent()).isGreaterThanOrEqualTo(threshold);
    }

}
