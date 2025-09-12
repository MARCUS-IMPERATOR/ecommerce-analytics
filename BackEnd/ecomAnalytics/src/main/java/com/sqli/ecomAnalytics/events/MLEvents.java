package com.sqli.ecomAnalytics.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MLEvents {
    private int customerId;
    private Events eventType;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private Object data;

    public MLEvents(int customerId, Events eventType) {
        this.customerId = customerId;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
    }
}
