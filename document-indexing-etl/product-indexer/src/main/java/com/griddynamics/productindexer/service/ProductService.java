package com.griddynamics.productindexer.service;

import com.griddynamics.productindexer.model.Event;
import com.griddynamics.productindexer.model.UpdateResult;

import java.util.List;

public interface ProductService {

    void recreateIndex();

    UpdateResult applyUpdateEvents(List<Event> events);

}
