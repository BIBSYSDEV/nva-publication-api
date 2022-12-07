package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class ResourceOwner {

    private final String owner;
    private final URI ownerAffiliation;

    @JsonCreator
    public ResourceOwner(@JsonProperty("owner") String owner,
                         @JsonProperty("ownerAffiliation") URI ownerAffiliation) {
        this.owner = owner;
        this.ownerAffiliation = ownerAffiliation;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getOwner(), getOwnerAffiliation());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceOwner that = (ResourceOwner) o;
        return Objects.equals(getOwner(), that.getOwner()) && Objects.equals(getOwnerAffiliation(),
                                                                             that.getOwnerAffiliation());
    }

    @JacocoGenerated
    public URI getOwnerAffiliation() {
        return ownerAffiliation;
    }

    @JacocoGenerated
    public String getOwner() {
        return owner;
    }
}
