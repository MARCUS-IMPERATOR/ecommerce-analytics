package com.sqli.ecomAnalytics.Analytics;

import com.sqli.ecomAnalytics.dto.RecommendationAnalyticsDto;
import com.sqli.ecomAnalytics.repository.ProductRecommendationsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationAnalyticsService {

    private final ProductRecommendationsRepository prRepository;

    public RecommendationAnalyticsService(ProductRecommendationsRepository prRepository) {
        this.prRepository = prRepository;
    }

    public RecommendationAnalyticsDto getRecommendationAnalytics(){
        List<Object[]> productData = prRepository.findTopRecommendedProducts();

        List<RecommendationAnalyticsDto.ProductRecommendationDto> topRecommendedProducts =
                productData.stream()
                        .map(row -> new RecommendationAnalyticsDto.ProductRecommendationDto(
                                (Integer) row[0],
                                (String) row[1],
                                (Long) row[2],
                                (double) row[3]
                        ))
                        .collect(Collectors.toList());
        Long totlCustomers = prRepository.countCustomersWithRecommendations();
        double averageRecommendations = prRepository.averageRecommendationsPerCustomer();

        return new RecommendationAnalyticsDto(topRecommendedProducts, totlCustomers, averageRecommendations);
    }
}
