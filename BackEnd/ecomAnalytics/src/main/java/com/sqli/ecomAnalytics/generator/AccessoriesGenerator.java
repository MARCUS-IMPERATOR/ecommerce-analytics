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
public class AccessoriesGenerator extends BaseProductGenerator {
    private static final Map<String, Double> BRAND_DIST = Map.of(
            "Anker", 0.30, "Belkin", 0.25, "Logitech", 0.20, "Other", 0.25
    );

    private static final Map<String, String> BRAND_CODES = Map.of(
            "Anker", "ANK", "Belkin", "BLK", "Logitech", "LOG", "Other", "OTH"
    );

    private static final Map<String, Double> PRICE_MULTIPLIERS = Map.of(
            "logitech", 1.1, "other", 0.85
    );

    private static final Random RANDOM = new Random();
    private static int skuCounter = 8000;

    public static ProductCreateDto generateAccessory() {
        String brand = weightedRandomBrand(BRAND_DIST);
        String sku = generateSKU("AC", brand);
        String name = generateName(brand);
        BigDecimal price = generatePrice(25, brand, PRICE_MULTIPLIERS);
        int stock = generateHighStockQuantity();

        ProductCreateDto product = new ProductCreateDto();
        product.setSku(sku);
        product.setCategory(ProductCategory.ACCESSORIES);
        product.setBrand(brand);
        product.setProductName(name);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setDescription(name + " by " + brand + " provides reliable quality.");

        return product;
    }

    private static String generateSKU(String prefix, String brand) {
        return String.format("%s-%s-%04d", prefix, BRAND_CODES.getOrDefault(brand, "OTH"), ++skuCounter);
    }

    private static String generateName(String brand) {
        List<String> models = List.of("Case", "Cable", "Stand", "Charger", "Adapter","Screen Protector","Controller","Game","Mouse","Keyboard");
        int number = RANDOM.nextInt(500) + 1;
        return brand + " " + models.get(RANDOM.nextInt(models.size())) + " " + number;
    }
}
