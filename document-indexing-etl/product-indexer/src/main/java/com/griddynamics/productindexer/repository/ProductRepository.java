package com.griddynamics.productindexer.repository;

import com.griddynamics.productindexer.model.Event;
import com.griddynamics.productindexer.model.UpdateResult;

import java.util.List;

public interface ProductRepository {

    void recreateIndex();

    UpdateResult processUpdateEvents(List<Event> events);
}
