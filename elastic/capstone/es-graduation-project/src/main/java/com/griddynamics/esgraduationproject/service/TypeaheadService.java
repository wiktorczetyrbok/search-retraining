package com.griddynamics.esgraduationproject.service;

import com.griddynamics.esgraduationproject.model.TypeaheadServiceRequest;
import com.griddynamics.esgraduationproject.model.TypeaheadServiceResponse;

public interface TypeaheadService {
    TypeaheadServiceResponse getServiceResponse(TypeaheadServiceRequest request);

    void recreateIndex();
}
