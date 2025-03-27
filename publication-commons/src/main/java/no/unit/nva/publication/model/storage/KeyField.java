package no.unit.nva.publication.model.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.stream.Collectors;
import nva.commons.core.SingletonCollector;

public enum KeyField {
    RESOURCE(ResourceDao.TYPE),
    MESSAGE(MessageDao.TYPE),
    TICKET(TicketDao.TICKETS_INDEXING_TYPE),
    DOI_REQUEST(DoiRequestDao.TYPE),
    FILE_ENTRY(FileDao.TYPE);

    public static final String KEY_FIELDS_DELIMITER = ":";
    private static final String SEPARATOR = ",";
    private final String keyField;

    KeyField(String keyField) {
        this.keyField = keyField;
    }

    @JsonCreator
    public static KeyField parse(String candidate) {
        return Arrays.stream(KeyField.values())
                   .filter(value -> value.filterByNameOrValue(candidate))
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow(fail -> handleParsingError());
    }

    public String getKeyField() {
        return KEY_FIELDS_DELIMITER + keyField;
    }

    @Override
    @JsonValue
    public String toString() {
        return keyField;
    }

    private static IllegalArgumentException handleParsingError() {
        return new IllegalArgumentException("Invalid types. Valid types: " + validValues());
    }

    private static String validValues() {
        return Arrays.stream(KeyField.values())
                   .map(KeyField::toString)
                   .collect(Collectors.joining(SEPARATOR));
    }

    private boolean filterByNameOrValue(String candidate) {
        return toString().equalsIgnoreCase(candidate)
               || getKeyField().equalsIgnoreCase(candidate);
    }
}
