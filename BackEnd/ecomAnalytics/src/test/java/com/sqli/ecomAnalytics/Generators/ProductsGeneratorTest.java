package com.sqli.ecomAnalytics.Generators;

import com.sqli.ecomAnalytics.configuration.DataGenerationProp;
import com.sqli.ecomAnalytics.dto.ProductCreateDto;
import com.sqli.ecomAnalytics.entity.ProductCategory;
import com.sqli.ecomAnalytics.generator.*;
import com.sqli.ecomAnalytics.service.ProductsService;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductsGeneratorTest {
    @Mock
    private ProductsService productsService;

    @Mock
    private DataGenerationProp prop;

    @Mock
    private DataGenerationProp.Products productsConfig;

    @Mock
    private DataGenerationProp.Products.CategoryDistribution categoryDist;

    @InjectMocks
    private ProductGenerator productGenerator;


    @Test
    void generateProductsAllCategories(){
        when(prop.getProducts()).thenReturn(productsConfig);
        when(productsConfig.getCategoryDistribution()).thenReturn(categoryDist);

        when(categoryDist.getTablets()).thenReturn(10);
        when(categoryDist.getSmartphones()).thenReturn(10);
        when(categoryDist.getAccessories()).thenReturn(10);
        when(categoryDist.getOthers()).thenReturn(10);
        when(categoryDist.getLaptops()).thenReturn(10);
        when(categoryDist.getGaming()).thenReturn(10);

        productGenerator.generateAndSaveFullCatalog();
        verify(productsService,times(60)).createProduct(any(ProductCreateDto.class));
    }

    @Test
    void smartphoneGeneratorTest() {
        ProductCreateDto smartphone = SmartPhonesGenerator.generateSmartphone();

        assertThat(smartphone.getCategory()).isEqualTo(ProductCategory.SMARTPHONES);
        assertThat(smartphone.getSku()).startsWith("SM-");
        assertThat(smartphone.getProductName()).isNotNull();
        assertThat(smartphone.getPrice()).isGreaterThan(BigDecimal.ZERO);
        assertThat(smartphone.getStockQuantity()).isGreaterThan(0);
        assertThat(smartphone.getBrand()).isIn("Apple", "Samsung", "Google", "Other");
    }

    @Test
    void laptopGeneratorTest() {
        ProductCreateDto laptop = LaptopGenerator.generateLaptop();

        assertThat(laptop.getCategory()).isEqualTo(ProductCategory.LAPTOPS);
        assertThat(laptop.getSku()).startsWith("LP-");
        assertThat(laptop.getProductName()).isNotNull();
        assertThat(laptop.getPrice()).isGreaterThan(BigDecimal.ZERO);
        assertThat(laptop.getStockQuantity()).isGreaterThan(0);
        assertThat(laptop.getBrand()).isIn("Dell", "HP", "Apple", "Lenovo", "ASUS", "Other");
    }

    @Test
    void accessoryGeneratorTest() {
        ProductCreateDto accessory = AccessoriesGenerator.generateAccessory();

        assertThat(accessory.getCategory()).isEqualTo(ProductCategory.ACCESSORIES);
        assertThat(accessory.getSku()).startsWith("AC-");
        assertThat(accessory.getProductName()).isNotNull();
        assertThat(accessory.getPrice()).isGreaterThan(BigDecimal.ZERO);
        assertThat(accessory.getStockQuantity()).isGreaterThan(0);
        assertThat(accessory.getBrand()).isIn("Anker", "Belkin", "Logitech", "Other");
    }

    @Test
    void baseProductGeneratorWeighted() {
        Map<String, Double> weights = Map.of(
                "Brand1", 0.5,
                "Brand2", 0.3,
                "Brand3", 0.2
        );

        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            String brand = BaseProductGenerator.weightedRandomBrand(weights);
            counts.put(brand, counts.getOrDefault(brand, 0) + 1);
        }

        assertThat(counts.get("Brand1")).isBetween(4800, 5200);
        assertThat(counts.get("Brand2")).isBetween(2800, 3200);
        assertThat(counts.get("Brand3")).isBetween(1800, 2200);
    }

    @Test
    void baseProductGeneratorPriceTest() {
        Map<String, Double> multipliers = Map.of("premium", 1.5);

        BigDecimal premiumPrice = BaseProductGenerator.generatePrice(100, "premium", multipliers);
        BigDecimal regularPrice = BaseProductGenerator.generatePrice(100, "regular", multipliers);

        assertThat(premiumPrice).isGreaterThan(regularPrice);
        assertThat(premiumPrice.doubleValue()).isCloseTo(150, Percentage.withPercentage(10));
    }

}
