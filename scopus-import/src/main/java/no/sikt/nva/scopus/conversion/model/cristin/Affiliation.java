package no.sikt.nva.scopus.conversion.model.cristin;

//Copied from nva-cristin-service

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Affiliation {

    private final URI organization;
    private final boolean active;
    private final Role role;

    /**
     * Creates an Affiliation for serialization to client.
     *
     * @param organization Identifier of Organization.
     * @param active       If this affiliation is currently active.
     * @param role         What roles this Person has at this affiliation.
     */
    @JsonCreator
    public Affiliation(@JsonProperty("organization") URI organization, @JsonProperty("active") Boolean active,
                       @JsonProperty("role") Role role) {
        this.organization = organization;
        this.active = active;
        this.role = role;
    }

    public URI getOrganization() {
        return organization;
    }

    public boolean isActive() {
        return active;
    }

    public Role getRole() {
        return role;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getOrganization(), isActive(), getRole());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Affiliation)) {
            return false;
        }
        Affiliation that = (Affiliation) o;
        return Objects.equals(getOrganization(), that.getOrganization())
               && Objects.equals(isActive(), that.isActive())
               && Objects.equals(getRole(), that.getRole());
    }
}
