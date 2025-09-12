package com.sqli.ecomAnalytics.controller;

import com.sqli.ecomAnalytics.dto.ForecastRequestDto;
import com.sqli.ecomAnalytics.dto.ForecastResponse;
import com.sqli.ecomAnalytics.service.ForecastingResponseService;
import com.sqli.ecomAnalytics.service.MLEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/forecasting")
@Slf4j
public class ForecastingController {

    private final MLEventPublisher mlEventPublisher;
    private final ForecastingResponseService forecastingService;

    public ForecastingController(MLEventPublisher mlEventPublisher, ForecastingResponseService forecastingService) {
        this.mlEventPublisher = mlEventPublisher;
        this.forecastingService = forecastingService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ForecastResponse> generateForecast(@RequestBody ForecastRequestDto request) {
        try {
            log.info("Generating Forecast Request....");
            String requestId = UUID.randomUUID().toString();
            request.setRequestId(requestId);

            forecastingService.registerPendingRequest(requestId);

            mlEventPublisher.publishForecastRequest(request);

            ForecastResponse response = forecastingService.waitForForecastResults(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Generating Forecast Request Failed...", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ForecastResponse.builder()
                            .success(false)
                            .error("Failed to generate forecast: " + e.getMessage())
                            .build());
        }
    }
}
