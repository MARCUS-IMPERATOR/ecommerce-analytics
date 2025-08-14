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
public class GamingConsoleGenerator extends BaseProductGenerator {
    private static final Map<String, Double> BRAND_DIST = Map.of(
            "Sony", 0.40, "Microsoft", 0.35, "Nintendo", 0.25
    );

    private static final Map<String, String> BRAND_CODES = Map.of(
            "Sony", "SNY", "Microsoft", "MSF", "Nintendo", "NTD"
    );

    private static final Map<String, Double> PRICE_MULTIPLIERS = Map.of(
            "sony", 1.2, "microsoft", 1.2, "nintendo", 0.9
    );

    private static final Random RANDOM = new Random();
    private static int skuCounter = 5000;

    public static ProductCreateDto generateGamingProduct() {
        String brand = weightedRandomBrand(BRAND_DIST);
        String sku = generateSKU("GM", brand);
        String name = generateName(brand);
        BigDecimal price = generatePrice(400, brand, PRICE_MULTIPLIERS);
        int stock = generateLimitedStockQuantity();

        ProductCreateDto product = new ProductCreateDto();
        product.setSku(sku);
        product.setCategory(ProductCategory.GAMING_CONSOLES);
        product.setBrand(brand);
        product.setProductName(name);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setDescription(name + " by " + brand + " delivers immersive gaming experience.");

        return product;
    }

    private static String generateSKU(String prefix, String brand) {
        return String.format("%s-%s-%04d", prefix, BRAND_CODES.getOrDefault(brand, "OTH"), ++skuCounter);
    }

    private static String generateName(String brand) {
        List<String> models = List.of("Console", "Pro", "Slim", "Edition", "Series");
        int number = 100 + RANDOM.nextInt(50);
        return brand + " " + models.get(RANDOM.nextInt(models.size())) + " " + number;
    }
}
