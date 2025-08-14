package com.sqli.ecomAnalytics.controller;

import com.sqli.ecomAnalytics.dto.CustomerRegistrationDto;
import com.sqli.ecomAnalytics.dto.CustomerUpdateDto;
import com.sqli.ecomAnalytics.entity.Customers;
import com.sqli.ecomAnalytics.entity.Segments;
import com.sqli.ecomAnalytics.service.CustomersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Customers operations")
public class CustomersController {
    private final CustomersService customersService;

    public CustomersController(CustomersService customersService) {
        this.customersService = customersService;
    }

    @Operation(summary = "Get all customers", responses = {
            @ApiResponse(responseCode = "200", description = "List of customers returned successfully")
    })
    @GetMapping("/all")
    public ResponseEntity<List<Customers>> getAllCustomers() {
        List<Customers> customers = customersService.findAllCustomers();
        return ResponseEntity.ok().body(customers);
    }

    @Operation(summary = "Get a customer by Id", responses = {
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404",description = "Customer not found")
    })
    @GetMapping("{customerId}")
    public ResponseEntity<Customers> getCustomerById(
            @Parameter(description = "ID of the customer to retrieve", example = "1")
            @PathVariable("customerId") int customerId) {
        Customers customer = customersService.findCustomerById(customerId);
        return ResponseEntity.ok().body(customer);
    }
    @Operation(summary = "Register a new customer",responses = {
            @ApiResponse(responseCode = "200",description = "Customer registered successfully"),
            @ApiResponse(responseCode = "400",description = "Invalid registration data provided"),
            @ApiResponse(responseCode = "409",description = "Customer with provided email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<Customers> registerCustomer(
            @Parameter(description = "Customer registration information", required = true)
            @RequestBody CustomerRegistrationDto customer) {
        Customers c = customersService.registerCustomer(customer);
        return ResponseEntity.ok().body(c);
    }

    @Operation(summary = "Update customer information",responses = {
            @ApiResponse(responseCode = "200",description = "Customer updated successfully"),
            @ApiResponse(responseCode = "400",description = "Invalid update data provided"),
            @ApiResponse(responseCode = "404",description = "Customer not found")
            })
    @PatchMapping("/{customerId}")
    public ResponseEntity<Customers> updateCustomer(
            @Parameter(description = "Customer update information", required = true)
            @RequestBody CustomerUpdateDto customer,
            @Parameter(description = "ID of the customer to update", example = "1", required = true)
            @PathVariable("customerId") int customerId) {
        Customers c = customersService.updateCustomer(customerId,customer);
        return ResponseEntity.ok().body(c);
    }

    @Operation(summary = "Calculate customer lifetime value",responses = {
            @ApiResponse(responseCode = "200",description = "LTV calculated successfully"),
            @ApiResponse(responseCode = "404",description = "Customer not found")
    })
    @GetMapping("/ltv/{customerId}")
    public ResponseEntity<BigDecimal> calculateCustomerLtv(
            @Parameter(description = "ID of the customer to calculate LTV for", example = "1", required = true)
            @PathVariable("customerId") int customerId) {
        Customers customer = customersService.findCustomerById(customerId);
        BigDecimal ltv = CustomersService.calculateLTV(customer);
        return ResponseEntity.ok().body(ltv);
    }

    @Operation(summary = "Get a customer by a customer code",responses = {
            @ApiResponse(responseCode = "200",description = "Customer returned successfully"),
            @ApiResponse(responseCode = "404",description = "Customer not found")
    })
    @GetMapping("/customerCode/{customerCode}")
    public ResponseEntity<Customers> getCustomerByCustomerCode(
            @Parameter(description = "Code of the customer")
            @PathVariable String customerCode) {
        return ResponseEntity.ok().body(customersService.findCustomerByCustomerCode(customerCode));
    }

    @Operation(summary = "Get a customer by a first and last name",responses = {
            @ApiResponse(responseCode = "200",description = "Customer returned successfully"),
            @ApiResponse(responseCode = "404",description = "Customer not found")
    })
    @GetMapping("/firstName/{firstName}/lastName/{lastName}")
    public ResponseEntity<Optional<Customers>> getCustomerByFirstNameAndLastName(
            @Parameter(description = "Customer's first name")
            @PathVariable String  firstName,
            @Parameter(description = "Customer's last name")
            @PathVariable String lastName) {
        Optional<Customers> customer = customersService.findCustomerByFirstNameAndLastName(firstName,lastName);
        return ResponseEntity.ok().body(customer);
    }

    @Operation(summary = "Get a list of the highest paying customers",responses = {
            @ApiResponse(responseCode = "200",description = "List of customers returned successfully")
    })
    @GetMapping("/highestPaying")
    public ResponseEntity<List<Customers>> getHighestPaying() {
        List<Customers> customers = customersService.findHighestPayingCustomers();
        return ResponseEntity.ok().body(customers);
    }

    @Operation(summary = "Get the segment of a customer by Id",responses = {
            @ApiResponse(responseCode = "200",description = "Customer's segment returned successfully"),
            @ApiResponse(responseCode = "404",description = "Customer not found"),
    })
    @GetMapping("/customerSegment/{customerId}")
    public ResponseEntity<Optional<Customers>> getSegmentCustomers(
            @Parameter(description = "Id of the customer")
            @PathVariable int customerId) {
        Optional<Customers> customer = customersService.findCustomerSegment(customerId);
        return ResponseEntity.ok().body(customer);
    }

    @Operation(summary = "Get a list of customers with a specific segment",responses = {
            @ApiResponse(responseCode = "200",description = "List of customers returned successfully")
    })
    @GetMapping("/segment/{segment}")
    public ResponseEntity<List<Customers>> getSegmentCustomers(
            @Parameter(description = "Customers segment")
            @PathVariable Segments segment) {
        List<Customers> customers = customersService.findBySegments(segment);
        return ResponseEntity.ok().body(customers);
    }

    @Operation(summary = "Get the highest paying customer",responses = {
            @ApiResponse(responseCode = "200",description = "Customer returned successfully")
    })
    @GetMapping("/topCustomerByTotalSpent")
    public ResponseEntity<Customers> getTopCustomerByTotalSpent(){
        return ResponseEntity.ok().body(customersService.findTopCustomerByTotalSpent());
    }
}
