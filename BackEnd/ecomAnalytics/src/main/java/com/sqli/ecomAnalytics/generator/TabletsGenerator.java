package com.sqli.ecomAnalytics.generator;

import com.sqli.ecomAnalytics.dto.ProductCreateDto;
import com.sqli.ecomAnalytics.entity.ProductCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
public class TabletsGenerator extends  BaseProductGenerator {
    private static final Map<String, Double> BRAND_DIST = Map.of(
            "Apple", 0.4, "Samsung", 0.3, "Lenovo", 0.15, "Microsoft", 0.1, "Others", 0.05
    );

    private static final Map<String, String> BRAND_CODES = Map.of(
            "Apple", "APL", "Samsung", "SMG", "Lenovo", "LNV", "Microsoft", "MSF", "Others", "OTH"
    );

    private static final Map<String, Double> PRICE_MULTIPLIERS = Map.of(
            "apple", 1.3, "microsoft", 1.2, "others", 0.8
    );

    private static final Random RANDOM = new Random();
    private static int skuCounter = 6000;

    public static ProductCreateDto generateTablet() {
        String brand = weightedRandomBrand(BRAND_DIST);
        String sku = generateSKU(brand);
        String name = generateName(brand);
        BigDecimal price = generatePrice(400, brand, PRICE_MULTIPLIERS);
        int stock = generateStockQuantity();

        ProductCreateDto product = new ProductCreateDto();
        product.setSku(sku);
        product.setCategory(ProductCategory.TABLETS);
        product.setBrand(brand);
        product.setProductName(name);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setDescription(name + " by " + brand + " offers portability and performance.");

        return product;
    }

    private static String generateSKU(String brand) {
        return String.format("%s-%s-%04d", "TAB", BRAND_CODES.getOrDefault(brand, "OTH"), ++skuCounter);
    }

    private static String generateName(String brand) {
        List<String> models = List.of("iPad", "Tab", "Surface", "MatePad", "Pad");
        List<String> variants = List.of("Pro", "Air", "Mini", "Plus", "Ultra");
        return brand + " " + models.get(RANDOM.nextInt(models.size())) + " " + variants.get(RANDOM.nextInt(variants.size()));
    }
}
