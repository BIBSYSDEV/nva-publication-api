package no.unit.nva.publication.model.business.importcandidate;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CandidateStatus {

    IMPORTED("IMPORTED"),
    NOT_IMPORTED("NOT_IMPORTED"),
    NOT_APPLICABLE("NOT_APPLICABLE");
    private String value;

    CandidateStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
