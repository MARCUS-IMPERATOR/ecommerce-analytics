package com.sqli.ecomAnalytics.dto;

import com.sqli.ecomAnalytics.entity.CustomerSegments;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponseDto {
    private int customerId;
    private String customerCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDateTime registrationDate;
    private BigDecimal totalSpent;
    private Integer orderCount;
    private LocalDateTime lastOrderDate;
    private CustomerSegments customerSegment;
}
