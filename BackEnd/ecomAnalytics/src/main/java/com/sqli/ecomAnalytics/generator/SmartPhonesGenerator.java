package com.sqli.ecomAnalytics.generator;

import com.sqli.ecomAnalytics.dto.ProductCreateDto;
import com.sqli.ecomAnalytics.entity.ProductCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Getter
@Setter
public class SmartPhonesGenerator extends  BaseProductGenerator {
    private static final Map<String, Double> BRAND_DIST = Map.of(
            "Apple", 0.40, "Samsung", 0.35, "Google", 0.15, "Other", 0.10
    );

    private static final Map<String, String> BRAND_CODES = Map.of(
            "Apple", "APL", "Samsung", "SMS", "Google", "GGL", "Other", "OTH"
    );

    private static final Map<String, Double> PRICE_MULTIPLIERS = Map.of(
            "apple", 1.3, "other", 0.8
    );

    private static final Random RANDOM = new Random();
    private static int skuCounter = 3000;

    public static ProductCreateDto generateSmartphone() {
        String brand = weightedRandomBrand(BRAND_DIST);
        String sku = generateSKU(brand);
        String name = generateName(brand);
        BigDecimal price = generatePrice(600, brand, PRICE_MULTIPLIERS);
        int stock = generateStockQuantity();

        ProductCreateDto product = new ProductCreateDto();
        product.setSku(sku);
        product.setCategory(ProductCategory.SMARTPHONES);
        product.setBrand(brand);
        product.setProductName(name);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setDescription(name + " is a premium smartphone by " + brand + " featuring cutting-edge chipset, multiple cameras, and OLED display.");

        return product;
    }

    private static String generateSKU(String brand) {
        return String.format("%s-%s-%04d", "SM", BRAND_CODES.getOrDefault(brand, "OTH"), ++skuCounter);
    }

    private static String generateName(String brand) {
        List<String> models = List.of("Ultra", "Pro", "Max", "S", "Note", "X", "Z");
        List<String> variants = List.of("2024", "Plus", "Mini", "SE", "XL");
        return brand + " " + models.get(RANDOM.nextInt(models.size())) + " " + variants.get(RANDOM.nextInt(variants.size()));
    }
}
