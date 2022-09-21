package no.unit.nva.publication.tickets.update;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import nva.commons.core.SingletonCollector;

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
    
    public static String legalValues() {
        return Arrays.toString(ViewStatus.values());
    }
    
    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
