package com.sqli.ecomAnalytics.util;


import java.time.LocalDateTime;

public final class RedisCacheKeys {

    private RedisCacheKeys() {}

    public static String customerIdKey(int customerId) {
        return String.format("customer:profile:%d", customerId);
    }

    public static String customerCodeKey(String customerCode) {
        return String.format("customer:code:%s", customerCode);
    }

    public static String allCustomersKey() {
        return "customers:all";
    }

    public static String productCatalogKey(String category, int page) {
        return String.format("product:catalog:%s:%d", category, page);
    }

    public static String customerRecommendationsKey(String customerId) {
        return String.format("customer:recommendations:%s", customerId);
    }
    public static String productIdKey(int productId) {
        return String.format("product:id:%d", productId);
    }

    public static String productCategoryKey(String category) {
        return String.format("product:category:%s", category.toLowerCase());
    }

    public static String productBrandKey(String brand) {
        return String.format("product:brand:%s", brand.toLowerCase());
    }

    public static String productSearchKey(String searchTerm) {
        return String.format("product:search:%s", searchTerm.toLowerCase());
    }

    public static String customerSpentKey(int customerId) {
        return String.format("customer:spent:%d", customerId);
    }

    public static String customerOrderCountKey(int customerId) {
        return String.format("customer:orders:%d", customerId);
    }

    public static String customerAnalyticsKey(LocalDateTime start, LocalDateTime end, LocalDateTime thresholdDate) {
        return String.format("customer:analytics:start:%s:end:%s:threshold:%s", start, end, thresholdDate);
    }

    public static String kpiKeys(LocalDateTime start, LocalDateTime end) {
        return String.format("kpi:start:%s:end:%s", start, end);
    }

    public static String productPerformanceKeys(LocalDateTime start, LocalDateTime end, int lowStockThreshold) {
        return String.format("customer:analytics:start:%s:end:%s:lowStockThreshold:%s", start, end, lowStockThreshold);
    }

    public static String salesTrendKeys(LocalDateTime start, LocalDateTime end) {
        return String.format("sales:start:%s:end:%s", start, end);
    }
}
