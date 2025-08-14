package com.sqli.ecomAnalytics.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerUpdateDto {
    private String firstName;
    private String lastName;
    private int age;
    private String country;
    @Email(message = "Please provide a valid email address")
    private String email;
    private String phone;
}
