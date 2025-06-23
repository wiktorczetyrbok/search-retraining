package com.griddynamics.productindexer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateResult {
    private int totalEvents;
    private int processedEvents;
    private int skippedUnchanged;
    private int skippedUnindexed;
    private int updated;
}
