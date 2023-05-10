package no.unit.nva.publication.ticket.update;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import java.util.Arrays;

public enum ViewStatus {
    READ("Read"),
    UNREAD("Unread");
    
    private final String value;
    
    ViewStatus(String value) {
        this.value = value;
    }
    
    @JsonCreator
    public static ViewStatus parse(String candidate) {
        return Arrays.stream(ViewStatus.values())
                   .filter(value -> value.toString().equalsIgnoreCase(candidate))
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow();
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
