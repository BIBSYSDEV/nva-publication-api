package no.unit.nva.expansion.model.cristin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CristinPerson {

    @JsonProperty("names")
    private final List<NameType> names;

    @JsonCreator
    public CristinPerson(@JsonProperty("names") List<NameType> names) {
        this.names = names;
    }

    public Map<String, String> getNameTypeMap() {
        return names.stream().collect(Collectors.toMap(NameType::getType, NameType::getName));
    }

    public final static class NameType {

        @JsonProperty("type")
        private final String type;

        @JsonProperty("value")
        private final String name;

        @JsonCreator
        public NameType(@JsonProperty("type") String type,
                        @JsonProperty("value") String name) {
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
