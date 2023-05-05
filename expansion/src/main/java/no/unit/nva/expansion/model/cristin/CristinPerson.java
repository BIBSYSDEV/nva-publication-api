package no.unit.nva.expansion.model.cristin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CristinPerson {

    public static final String NAMES_FIELD = "names";
    @JsonProperty(NAMES_FIELD)
    private final List<NameType> names;

    @JsonCreator
    public CristinPerson(@JsonProperty(NAMES_FIELD) List<NameType> names) {
        this.names = names;
    }

    public Map<String, String> getNameTypeMap() {
        return names.stream().collect(Collectors.toMap(NameType::getType, NameType::getName));
    }

    public final static class NameType {

        public static final String TYPE_FIELD = "type";
        public static final String VALUE_FIELD = "value";
        @JsonProperty(TYPE_FIELD)
        private final String type;

        @JsonProperty(VALUE_FIELD)
        private final String name;

        @JsonCreator
        public NameType(@JsonProperty(TYPE_FIELD) String type,
                        @JsonProperty(VALUE_FIELD) String name) {
            this.type = type;
            this.name = name;
        }

        @JsonProperty("type")
        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }
}
