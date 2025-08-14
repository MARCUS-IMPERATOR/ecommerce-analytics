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
public class LaptopGenerator extends BaseProductGenerator{

    private static final Map<String, Double> BRAND_DIST = Map.of(
            "Dell", 0.20, "HP", 0.30, "Apple", 0.20, "Lenovo", 0.15, "ASUS", 0.10, "Other", 0.05
    );

    private static final Map<String, String> BRAND_CODES = Map.of(
            "Dell", "DLL", "HP", "HPQ", "Apple", "APL", "Lenovo", "LNV", "ASUS", "ASU", "Other", "OTH"
    );

    private static final Map<String, Double> PRICE_MULTIPLIERS = Map.of(
            "apple", 1.2, "other", 0.85
    );

    private static final Random RANDOM = new Random();
    private static int skuCounter = 4000;

    public static ProductCreateDto generateLaptop() {
        String brand = weightedRandomBrand(BRAND_DIST);
        String sku = generateSKU("LP", brand);
        String name = generateName(brand);
        BigDecimal price = generatePrice(900, brand, PRICE_MULTIPLIERS);
        int stock = generateStockQuantity();

        ProductCreateDto product = new ProductCreateDto();
        product.setSku(sku);
        product.setCategory(ProductCategory.LAPTOPS);
        product.setBrand(brand);
        product.setProductName(name);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setDescription(name + " is a powerful laptop by " + brand + ", ideal for professionals and students.");

        return product;
    }

    private static String generateSKU(String prefix, String brand) {
        return String.format("%s-%s-%04d", prefix, BRAND_CODES.getOrDefault(brand, "OTH"), ++skuCounter);
    }

    private static String generateName(String brand) {
        List<String> models = List.of("ProBook", "EliteBook", "ThinkPad", "ZenBook", "MacBook", "Inspiron", "Pavilion");
        int year = 2020 + RANDOM.nextInt(5);
        return brand + " " + models.get(RANDOM.nextInt(models.size())) + " " + year;
    }
}
