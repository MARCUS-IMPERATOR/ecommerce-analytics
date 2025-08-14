package com.sqli.ecomAnalytics.generator;

import com.sqli.ecomAnalytics.configuration.DataGenerationProp;
import com.sqli.ecomAnalytics.dto.ProductCreateDto;
import com.sqli.ecomAnalytics.exceptions.ProductAlreadyExistsException;
import com.sqli.ecomAnalytics.service.ProductsService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;


@Component
public class ProductGenerator {

    private final DataGenerationProp prop;
    private final ProductsService productService;


    public ProductGenerator(DataGenerationProp prop, ProductsService productService) {
        this.prop = prop;
        this.productService = productService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateAndSaveFullCatalog() {
        var categoryDist = prop.getProducts().getCategoryDistribution();

        generateAndSaveProducts(categoryDist.getSmartphones(), SmartPhonesGenerator::generateSmartphone);
        generateAndSaveProducts(categoryDist.getLaptops(), LaptopGenerator::generateLaptop);
        generateAndSaveProducts(categoryDist.getGaming(), GamingConsoleGenerator::generateGamingProduct);
        generateAndSaveProducts(categoryDist.getTablets(), TabletsGenerator::generateTablet);
        generateAndSaveProducts(categoryDist.getAccessories(), AccessoriesGenerator::generateAccessory);
        generateAndSaveProducts(categoryDist.getOthers(), OtherProductsGenerator::generateOtherProduct);
    }

    private void generateAndSaveProducts(int count, Supplier<ProductCreateDto> generator) {
        for (int i = 0; i < count; i++) {
            try {
                ProductCreateDto productDto = generator.get();
                productService.createProduct(productDto);
            } catch (ProductAlreadyExistsException e) {
                i--;
            }
        }
    }
}
