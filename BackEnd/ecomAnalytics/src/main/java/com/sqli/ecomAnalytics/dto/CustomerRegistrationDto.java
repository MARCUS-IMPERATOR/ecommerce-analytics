package com.sqli.ecomAnalytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerRegistrationDto {
    @Schema(description = "First name of the customer", example = "John")
    @NotBlank(message = "First name is required")
    private String firstName;

    @Schema(description = "Last name of the customer", example = "Doe")
    @NotBlank(message = "Last name is required")
    private String lastName;

    @Schema(description = "Age of the customer", example = "24")
    @NotNull(message = "Age is required")
    private int age;

    @Schema(description = "Country of the customer", example = "Morocco")
    @NotBlank(message = "Country is required")
    private String country;

    @Schema(description = "E-mail of the customer", example = "jonh@email.com")
    @NotBlank(message = "E-mail is required")
    @Email(message = "E-mail address must be valid")
    private String email;

    @Schema(description = "Phone of the customer", example = "+2127012345678")
    @NotBlank(message = "Phone number is required")
    private String phone;

    @Builder.Default
    @Schema(description = "Registration Date", example = "2025-08-09T14:45:30.123")
    private LocalDateTime registerDate = LocalDateTime.now();
}
