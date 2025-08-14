package com.sqli.ecomAnalytics.Generators;

import com.sqli.ecomAnalytics.configuration.DataGenerationProp;
import com.sqli.ecomAnalytics.dto.CustomerRegistrationDto;
import com.sqli.ecomAnalytics.generator.CustomerGenerator;
import com.sqli.ecomAnalytics.service.CustomersService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerGeneratorTest {

    @Mock
    private CustomersService customersService;

    @Mock
    private DataGenerationProp prop;

    @Mock
    private DataGenerationProp.Customers customersConfig;

    @Mock
    private DataGenerationProp.Customers.GeogDist geogDist;

    @InjectMocks
    private CustomerGenerator customerGenerator;

    @Test
    void generateCustomersNumberOfCustomers() {

        when(prop.getCustomers()).thenReturn(customersConfig);
        when(customersConfig.getGeogDist()).thenReturn(geogDist);
        when(customersConfig.getCount()).thenReturn(100);

        customerGenerator.generateCustomers();


        verify(customersService, times(100)).registerCustomer(any(CustomerRegistrationDto.class));
    }

    @Test
    void generateRandomRegistrationDate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twelveMonthsAgo = now.minusMonths(12);

        LocalDateTime result = CustomerGenerator.generateRandomRegistrationDate(12);

        LocalDateTime endOfCurrentMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);

        assertThat(result).isBetween(twelveMonthsAgo, endOfCurrentMonth);
    }


}
