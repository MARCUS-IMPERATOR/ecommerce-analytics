package com.sqli.ecomAnalytics.generator;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Random;

@Getter
@Setter
@Component
public class BaseProductGenerator {
    private static final Random RANDOM = new Random();

    public static String weightedRandomBrand(Map<String, Double> brandWeights) {
        double p = RANDOM.nextDouble();
        double cumulative = 0.0;
        for (Map.Entry<String, Double> entry : brandWeights.entrySet()) {
            cumulative += entry.getValue();
            if (p <= cumulative) return entry.getKey();
        }
        return brandWeights.keySet().iterator().next();
    }

    public static int generateStockQuantity() {
        double p = RANDOM.nextDouble();
        if (p <= 0.8) return 200 + RANDOM.nextInt(500);
        else if (p <= 0.9) return 20+ RANDOM.nextInt(50);
        else return 500 + RANDOM.nextInt(800);
    }

    public static int generateHighStockQuantity() {
        double p = RANDOM.nextDouble();
        if (p <= 0.8) return 50 + RANDOM.nextInt(200);
        else if (p <= 0.9) return RANDOM.nextInt(50);
        else return 201 + RANDOM.nextInt(500);
    }

    public static int generateLimitedStockQuantity() {
        double p = RANDOM.nextDouble();
        if (p <= 0.8) return 10 + RANDOM.nextInt(40);
        else if (p <= 0.9) return RANDOM.nextInt(10);
        else return 51 + RANDOM.nextInt(150);
    }

    public static BigDecimal generatePrice(double basePrice, String brand, Map<String, Double> multipliers) {
        double multiplier = multipliers.getOrDefault(brand.toLowerCase(), 1.0);
        double variance = basePrice * 0.2 * (RANDOM.nextDouble() - 0.5);
        double price = (basePrice * multiplier) + variance;
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
    }
}
