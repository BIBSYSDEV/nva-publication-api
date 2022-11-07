package no.unit.nva.schemaorg.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
public class Person implements PersonI {
    public static final String NAME_FIELD = "name";
    @JsonProperty(NAME_FIELD)
    private final String name;

    public Person(@JsonProperty(NAME_FIELD) String name) {
        this.name = name;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Person)) {
            return false;
        }
        Person person = (Person) o;
        return Objects.equals(name, person.name);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(name);
    }
}
