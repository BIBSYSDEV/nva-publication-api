package no.unit.nva.publication.model.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.stream.Collectors;
import no.unit.nva.publication.model.business.TicketStatus;
import nva.commons.core.SingletonCollector;

public enum KeyField {
    RESOURCE(ResourceDao.TYPE),
    MESSAGE(MessageDao.TYPE),
    TICKET(TicketDao.TICKETS_INDEXING_TYPE),
    DOI_REQUEST("DoiRequest");

    public static final String KEY_FIELDS_DELIMITER = ":";
    private static final String SEPARATOR = ",";

    private final String keyField;

    KeyField(String keyField) {
        this.keyField = keyField;
    }

    @JsonValue
    public String getKeyField() {
        return KEY_FIELDS_DELIMITER + keyField;
    }

    @JsonCreator
    public KeyField parse(String candidate) {
        return Arrays.stream(KeyField.values())
                   .filter(value -> value.toString().equalsIgnoreCase(candidate))
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow(fail -> handleParsingError());
    }

    private static IllegalArgumentException handleParsingError() {
        return new IllegalArgumentException("Invalid types. Valid types: " + validValues());
    }

    private static String validValues() {
        return Arrays.stream(TicketStatus.values())
                   .map(TicketStatus::toString)
                   .collect(Collectors.joining(SEPARATOR));
    }
}
