package com.sqli.ecomAnalytics.service;

import com.sqli.ecomAnalytics.dto.ForecastResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class ForecastingResponseService {
    private final Map<String, CompletableFuture<ForecastResponse>> pendingRequests = new ConcurrentHashMap<>();

    @KafkaListener(topics = "forecast-responses")
    public void listenForForecastResponse(@Payload ForecastResponse response,
                                          @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.info("Received forecast response for key: {}", key);
        CompletableFuture<ForecastResponse> future = pendingRequests.remove(key);
        if (future != null) {
            future.complete(response);
            log.info("Completed future for request: {}", key);
        } else {
            log.warn("No pending request found for key: {}", key);
        }
    }

    public void registerPendingRequest(String requestId) {
        log.info("Registering pending request: {}", requestId);
        pendingRequests.put(requestId, new CompletableFuture<>());
    }

    public ForecastResponse waitForForecastResults(String requestId) {
        try {
            log.info("Waiting for forecast results for request: {}", requestId);
            CompletableFuture<ForecastResponse> future = pendingRequests.get(requestId);
            if (future == null) {
                throw new RuntimeException("No pending request found for ID: " + requestId);
            }
            return future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Forecast generation timed out for request: {}", requestId);
            pendingRequests.remove(requestId);
            throw new RuntimeException("Forecast generation timed out");
        } catch (Exception e) {
            log.error("Error waiting for forecast results for request: {}", requestId, e);
            pendingRequests.remove(requestId);
            throw new RuntimeException("Error waiting for forecast results", e);
        }
    }
}
