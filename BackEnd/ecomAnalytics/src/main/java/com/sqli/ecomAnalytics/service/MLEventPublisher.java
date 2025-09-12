package com.sqli.ecomAnalytics.service;


import com.sqli.ecomAnalytics.dto.ForecastRequestDto;
import com.sqli.ecomAnalytics.events.Events;
import com.sqli.ecomAnalytics.events.MLEvents;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class MLEventPublisher {
    private final KafkaTemplate<String, MLEvents> kafkaTemplate;
    private static final String TOPIC = "ml-events";
    private boolean eventsEnabled = false;

    public void enableEvents () {
        this.eventsEnabled = true;
        log.info("ML Event publishing enabled");
    }

    public void disableEvents () {
        this.eventsEnabled = false;
        log.info("ML Event publishing disabled");
    }

    public boolean areEventsEnabled() {
        return eventsEnabled;
    }

    public MLEventPublisher(KafkaTemplate<String, MLEvents> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private void publishEvent(MLEvents event) {

        if (!eventsEnabled) {
            log.debug("ML events disabled - skipping event: {} for customer: {}",event.getEventType(), event.getCustomerId());
            return;
        }

        try {
            CompletableFuture<SendResult<String, MLEvents>> future =
                    kafkaTemplate.send(TOPIC, String.valueOf(event.getCustomerId()), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published event: {} for customer: {}",
                            event.getEventType(),
                            event.getCustomerId());
                } else {
                    log.error("Failed to publish event: {} for customer: {}",
                            event.getEventType(),
                            event.getCustomerId(),
                            ex);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing event: {} for customer: {}",
                    event.getEventType(),
                    event.getCustomerId(),
                    e);
        }
    }

    public void publishCustomerCreated(int customerId) {
        publishEvent(new MLEvents(customerId, Events.CUSTOMER_CREATED));
    }

    public void publishCustomerUpdated(int customerId) {
        publishEvent(new MLEvents(customerId, Events.CUSTOMER_UPDATED));
    }

    public void publishOrderCreated(int customerId, int orderId) {
        MLEvents event = new MLEvents(customerId, Events.ORDER_CREATED);
        event.setData(orderId);
        publishEvent(event);
    }

    public void publishOrderUpdated(int customerId, int orderId) {
        MLEvents event = new MLEvents(customerId, Events.ORDER_UPDATED);
        event.setData(orderId);
        publishEvent(event);
    }

    public void publishForecastRequest(ForecastRequestDto request) {
        if (!eventsEnabled) {
            log.debug("ML events disabled - skipping forecast request event: {}", request.getRequestId());
            return;
        }

        MLEvents event = new MLEvents(0, Events.FORECAST_REQUESTED);
        event.setData(request);

        try {
            CompletableFuture<SendResult<String, MLEvents>> future =
                    kafkaTemplate.send(TOPIC, request.getRequestId(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published event: {} with requestId: {}", event.getEventType(), request.getRequestId());
                } else {
                    log.error("Failed to publish event: {} with requestId: {}", event.getEventType(), request.getRequestId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing forecast request event with requestId: {}", request.getRequestId(), e);
            throw e;
        }
    }

    public void publishInitialData() {
        MLEvents event = new MLEvents(0, Events.INITIAL_DATA_GENERATED);
        try {
            CompletableFuture<SendResult<String, MLEvents>> future =
                    kafkaTemplate.send(TOPIC, "0", event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published INITIAL_DATA_GENERATED event");
                } else {
                    log.error("Failed to publish INITIAL_DATA_GENERATED event", ex);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing INITIAL_DATA_GENERATED event", e);
        }
        log.info("Initial data loaded event published - ML service will process ALL customers");
    }
}
