package com.sqli.ecomAnalytics.configuration;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "data-generation")
public class DataGenerationProp {
    private boolean enabled;
    private Customers customers = new Customers();
    private Products products = new Products();
    private Orders orders = new Orders();

    @Getter
    @Setter
    public static class Customers {
        @Min(1)
        private int count = 30;
        private GeogDist geogDist = new GeogDist();

        @Getter
        @Setter
        public static class GeogDist {
            @Min(0)
            private int moroccoPerc = 15;
            @Min(0)
            private int francePerc = 35;
            @Min(0)
            private int usaPerc = 25;
            @Min(0)
            private int canadaPerc = 10;
            @Min(0)
            private int otherPerc = 15;
        }
    }

    @Getter
    @Setter
    public static class Products {
        @Min(1)
        private int count = 60;
        private CategoryDistribution categoryDistribution = new CategoryDistribution();

        @Getter
        @Setter
        public static class CategoryDistribution {
            private int smartphones = 10;
            private int laptops = 10;
            private int tablets = 10;
            private int gaming = 10;
            private int accessories = 10;
            private int others = 10;
        }
    }

    @Getter
    @Setter
    @Validated
    public static class Orders {
        @Min(1)
        private int months = 24;
        @Min(1)
        private int dailyVolume = 4;
    }
}


