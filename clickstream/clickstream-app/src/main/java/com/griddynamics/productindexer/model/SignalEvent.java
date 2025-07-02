package com.griddynamics.productindexer.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SignalEvent {
    private String query;
    private String category;
    private List<String> shownProductIds;
    private String productId;
    private int position;
    private String timestamp;
    private String userId;
    private String sessionId;
    private String eventType;
}
