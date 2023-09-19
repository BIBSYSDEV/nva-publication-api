package no.unit.nva.expansion.model.cristin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CristinPerson {
    private static final String TYPE_JSON_NAME = "type";
    private static final String VALUE_JSON_NAME = "value";

    public static final String NAMES_JSON_NAME = "names";
    @JsonProperty(NAMES_JSON_NAME)
    private final List<NameType> names;

    @JsonCreator
    public CristinPerson(@JsonProperty(NAMES_JSON_NAME) List<NameType> names) {
        this.names = names;
    }

    public Map<String, String> getNameTypeMap() {
        return names.stream().collect(Collectors.toMap(NameType::type, NameType::name));
    }

    public record NameType(@JsonProperty(TYPE_JSON_NAME) String type, @JsonProperty(VALUE_JSON_NAME) String name) {

    }
}
