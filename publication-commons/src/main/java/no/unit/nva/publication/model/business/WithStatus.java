package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface WithStatus {
    
    @JsonIgnore
    String getStatusString();
}
