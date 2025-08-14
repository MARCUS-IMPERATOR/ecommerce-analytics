package com.sqli.ecomAnalytics.generator;

import com.sqli.ecomAnalytics.dto.OrderCreateDto;
import com.sqli.ecomAnalytics.dto.OrderItemsDto;
import com.sqli.ecomAnalytics.entity.*;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.service.OrdersService;
import com.sqli.ecomAnalytics.service.ProductsService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;


@Component
public class OrderDataGenerator {
    private final OrdersService ordersService;
    private final CustomerRepository customerRepository;
    private final ProductsService productService;
    private final Random random;

    private static final Map<String, Double> SEASONAL_MULTIPLIERS = Map.of(
            "BLACK_FRIDAY", 3.0,
            "HOLIDAY_SEASON", 2.0,
            "BACK_TO_SCHOOL", 1.5,
            "POST_HOLIDAY", 0.5,
            "SUMMER_SALES", 1.2
    );

    private static final Map<ProductCategory, List<String>> BUNDLE_SEARCH_TERMS = Map.of(
            ProductCategory.SMARTPHONES, List.of("case", "screen protector", "charger"),
            ProductCategory.GAMING_CONSOLES, List.of("controller", "game", "headset"),
            ProductCategory.LAPTOPS, List.of("mouse", "bag", "keyboard"),
            ProductCategory.TABLETS, List.of("case", "stylus", "keyboard")
    );

    public OrderDataGenerator(OrdersService ordersService,
                              CustomerRepository customerRepository,
                              ProductsService productService) {
        this.ordersService = ordersService;
        this.customerRepository = customerRepository;
        this.productService = productService;
        this.random = new Random();
    }

    public void generateOrdersForDateRange(LocalDate startDate, LocalDate endDate, int baseVolume) {
        List<Customers> allCustomers = customerRepository.findAll();

        if (allCustomers.isEmpty()) {
            throw new IllegalStateException("No customers found in database");
        }

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            double seasonalMultiplier = getSeasonalMultiplier(currentDate);
            int dailyOrderCount = (int) (baseVolume * seasonalMultiplier);

            for (int i = 0; i < dailyOrderCount; i++) {
                try {
                    Customers customer = selectCustomer(allCustomers);
                    OrderCreateDto orderDto = generateOrderForCustomer(customer, currentDate);
                    Orders createdOrder = ordersService.createOrder(orderDto);

                    double randStatus = random.nextDouble();
                    OrderStatus status;
                    if (randStatus < 0.1) {
                        status = OrderStatus.PENDING;
                    } else if (randStatus < 0.95) {
                        status = OrderStatus.DELIVERED;
                    } else {
                        status = OrderStatus.CANCELLED;
                    }

                    if (status != OrderStatus.PENDING) {
                        ordersService.updateOrderStatus(createdOrder.getOrderId(), status);
                    }

                } catch (Exception e) {
                    System.err.println("Failed to create order: " + e.getMessage());
                }
            }
            currentDate = currentDate.plusDays(1);
        }
    }

    private Customers selectCustomer(List<Customers> customers) {
        double rand = random.nextDouble();

        if (rand < 0.4) {
            return customers.get(random.nextInt(Math.min(customers.size(), customers.size() / 2)));
        } else if (rand < 0.85) {
            return customers.get(random.nextInt(customers.size()));
        } else {
            return customers.get(random.nextInt(Math.min(customers.size(), customers.size() / 5)));
        }
    }

    private OrderCreateDto generateOrderForCustomer(Customers customer, LocalDate orderDate) {
        Segments segment = getCustomerSegment(customer);
        List<OrderItemsDto> orderItems = new ArrayList<>();

        List<Products> allProducts = productService.findAllProducts();
        Products mainProduct = selectMainProduct(allProducts, segment);

        if (mainProduct != null) {
            orderItems.add(OrderItemsDto.builder()
                    .productId(mainProduct.getProductId())
                    .quantity(getQuantity(segment))
                    .build());

            orderItems.addAll(getBundleProducts(mainProduct, segment));
        }

        return OrderCreateDto.builder()
                .customerId(customer.getCustomerId())
                .orderDate(orderDate.atTime(
                        random.nextInt(24),
                        random.nextInt(60),
                        random.nextInt(60)
                ))
                .orderItems(orderItems.isEmpty() ? getFallbackOrder(allProducts) : orderItems)
                .build();
    }

    private Products selectMainProduct(List<Products> products, Segments segment) {
        List<Products> availableProducts = products.stream()
                .filter(p -> p.getStockQuantity() > 0)
                .toList();

        if (availableProducts.isEmpty()) return null;

        return switch (segment) {
            case NEW -> availableProducts.stream()
                    .filter(p -> p.getPrice().compareTo(new BigDecimal("200")) <= 0)
                    .findAny().orElse(availableProducts.get(random.nextInt(availableProducts.size())));
            case CHAMPION -> availableProducts.stream()
                    .filter(p -> p.getPrice().compareTo(new BigDecimal("500")) > 0)
                    .findAny().orElse(availableProducts.get(random.nextInt(availableProducts.size())));
            default -> availableProducts.get(random.nextInt(availableProducts.size()));
        };
    }

    private List<OrderItemsDto> getBundleProducts(Products mainProduct, Segments segment) {
        List<OrderItemsDto> bundleItems = new ArrayList<>();
        ProductCategory category = mainProduct.getCategory();

        List<String> searchTerms = BUNDLE_SEARCH_TERMS.get(category);
        if (searchTerms == null) return bundleItems;

        double bundleProbability = getBundleProbability(segment);
        int maxBundles = getMaxBundles(segment);

        for (String searchTerm : searchTerms) {
            if (bundleItems.size() >= maxBundles) break;

            if (random.nextDouble() < bundleProbability) {
                List<Products> correlatedProducts = productService.searchProducts(searchTerm);
                if (!correlatedProducts.isEmpty()) {
                    correlatedProducts.stream()
                            .filter(p -> p.getStockQuantity() > 0 && !(p.getProductId() == (mainProduct.getProductId())))
                            .findFirst().ifPresent(bundleProduct -> bundleItems.add(OrderItemsDto.builder()
                                    .productId(bundleProduct.getProductId())
                                    .quantity(1)
                                    .build()));

                }
            }
        }

        return bundleItems;
    }

    private double getBundleProbability(Segments segment) {
        return switch (segment) {
            case CHAMPION -> 0.95;
            case LOYAL -> 0.80;
            case NEW -> 0.30;
            case AT_RISK -> 0.50;
        };
    }

    private int getMaxBundles(Segments segment) {
        return switch (segment) {
            case CHAMPION -> 3;
            case LOYAL -> 2;
            case NEW -> 1;
            case AT_RISK -> 1;
        };
    }

    private Segments getCustomerSegment(Customers customer) {
        int customerId = customer.getCustomerId();
        double rand = random.nextDouble();

        if (customerId % 15 == 0) return Segments.CHAMPION;
        if (customerId % 8 == 0) return Segments.LOYAL;
        if (rand < 0.25) return Segments.NEW;
        return Segments.AT_RISK;
    }

    private int getQuantity(Segments segment) {
        return switch (segment) {
            case NEW -> 1;
            case LOYAL -> 1 + random.nextInt(3);
            case CHAMPION -> 2 + random.nextInt(4);
            case AT_RISK -> 1;
        };
    }

    private double getSeasonalMultiplier(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 11 && day >= 24 && day <= 27) return SEASONAL_MULTIPLIERS.get("BLACK_FRIDAY");
        if ((month == 11 && day >= 15) || month == 12) return SEASONAL_MULTIPLIERS.get("HOLIDAY_SEASON");
        if (month == 1) return SEASONAL_MULTIPLIERS.get("POST_HOLIDAY");
        if (month == 8) return SEASONAL_MULTIPLIERS.get("BACK_TO_SCHOOL");
        if ((month == 6 && day >= 15) || (month == 7 && day <= 15)) return SEASONAL_MULTIPLIERS.get("SUMMER_SALES");

        return 1.0;
    }

    private List<OrderItemsDto> getFallbackOrder(List<Products> products) {
        Products product = products.stream()
                .filter(p -> p.getStockQuantity() > 0)
                .findFirst()
                .orElse(products.getFirst());

        return List.of(OrderItemsDto.builder()
                .productId(product.getProductId())
                .quantity(1)
                .build());
    }
}