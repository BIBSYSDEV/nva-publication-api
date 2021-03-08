package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface WithStatus {

    @JsonIgnore
    String getStatusString();
}
