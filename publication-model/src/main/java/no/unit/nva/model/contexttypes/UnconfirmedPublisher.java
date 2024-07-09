package no.unit.nva.model.contexttypes;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UnconfirmedPublisher implements PublishingHouse {
    private final String name;

    @JsonCreator
    public UnconfirmedPublisher(@JsonProperty("name") String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnconfirmedPublisher)) {
            return false;
        }
        UnconfirmedPublisher that = (UnconfirmedPublisher) o;
        return Objects.equals(getName(), that.getName());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public boolean isValid() {
        return nonNull(name);
    }
}
