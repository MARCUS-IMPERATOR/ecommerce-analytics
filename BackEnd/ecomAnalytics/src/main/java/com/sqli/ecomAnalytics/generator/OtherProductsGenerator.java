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
public class OtherProductsGenerator extends BaseProductGenerator{
    private static final Map<String, Double> BRAND_DIST = Map.of(
            "Canon", 0.25, "Nikon", 0.20, "GoPro", 0.15, "DJI", 0.15, "Fitbit", 0.15, "Garmin", 0.10
    );

    private static final Map<String, String> BRAND_CODES = Map.of(
            "Canon", "CAN", "Nikon", "NKN", "GoPro", "GPR", "DJI", "DJI", "Fitbit", "FTB", "Garmin", "GAR"
    );

    private static final Map<String, Double> PRICE_MULTIPLIERS = Map.of(
            "canon", 1.2, "nikon", 1.2, "dji", 1.5, "fitbit", 0.8, "garmin", 0.9
    );

    private static final Random RANDOM = new Random();
    private static int skuCounter = 9000;

    public static ProductCreateDto generateOtherProduct() {
        String brand = weightedRandomBrand(BRAND_DIST);
        String sku = generateSKU(brand);
        String name = generateName(brand);
        BigDecimal price = generatePrice(200, brand, PRICE_MULTIPLIERS);
        int stock = generateStockQuantity();

        ProductCreateDto product = new ProductCreateDto();
        product.setSku(sku);
        product.setCategory(ProductCategory.OTHERS);
        product.setBrand(brand);
        product.setProductName(name);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setDescription(name + " by " + brand + " offers specialized functionality.");

        return product;
    }

    private static String generateSKU(String brand) {
        return String.format("%s-%s-%04d", "OTH", BRAND_CODES.getOrDefault(brand, "OTH"), ++skuCounter);
    }

    private static String generateName(String brand) {
        Map<String, List<String>> brandModels = Map.of(
                "Canon", List.of("EOS", "PowerShot", "PIXMA", "imageCLASS"),
                "Nikon", List.of("D-Series", "Z-Series", "COOLPIX", "KeyMission"),
                "GoPro", List.of("HERO", "MAX", "Session", "Fusion"),
                "DJI", List.of("Mavic", "Phantom", "Mini", "Air"),
                "Fitbit", List.of("Versa", "Charge", "Inspire", "Sense"),
                "Garmin", List.of("Forerunner", "Fenix", "Venu", "Instinct")
        );

        List<String> models = brandModels.getOrDefault(brand, List.of("Pro", "Plus", "Elite", "Max"));
        int number = RANDOM.nextInt(100) + 1;
        return brand + " " + models.get(RANDOM.nextInt(models.size())) + " " + number;
    }
}
