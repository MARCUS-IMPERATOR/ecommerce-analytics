package com.sqli.ecomAnalytics.exceptions;

public class ProductStockInsufficient extends RuntimeException {
    public ProductStockInsufficient(String message) {
        super(message);
    }
}
