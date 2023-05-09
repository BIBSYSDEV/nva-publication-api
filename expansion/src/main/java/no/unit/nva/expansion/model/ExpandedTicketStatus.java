package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ExpandedTicketStatus {
    NEW("New"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    CLOSED("Closed");

    private final String value;

    ExpandedTicketStatus(String value) {
        this.value = value;
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
