package com.griddynamics.esgraduationproject.repository;

import com.griddynamics.esgraduationproject.model.TypeaheadServiceRequest;
import com.griddynamics.esgraduationproject.model.TypeaheadServiceResponse;

public interface TypeaheadRepository {
    TypeaheadServiceResponse getAllTypeaheads(TypeaheadServiceRequest request);
    TypeaheadServiceResponse getTypeaheadsByQuery(TypeaheadServiceRequest request);

    void recreateIndex();
}
