package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonPropertyOrder({"brageRole", "role", "identity"})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Contributor {

    private Identity identity;
    private String role;
    private String brageRole;

    @JsonCreator
    public Contributor(@JsonProperty("identity") Identity identity,
                       @JsonProperty("role") String role,
                       @JsonProperty("brageRole") String brageRole) {
        this.identity = identity;
        this.role = role;
        this.brageRole = brageRole;
    }

    @JsonProperty("role")
    public String getRole() {
        return role;
    }

    @JacocoGenerated
    public void setRole(String role) {
        this.role = role;
    }

    @JsonProperty("identity")
    public Identity getIdentity() {
        return identity;
    }

    @JacocoGenerated
    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    @JacocoGenerated
    public String getBrageRole() {
        return brageRole;
    }

    @JacocoGenerated
    public void setBrageRole(String brageRole) {
        this.brageRole = brageRole;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(identity, role, brageRole);
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
        Contributor that = (Contributor) o;
        return Objects.equals(brageRole, that.brageRole)
               && Objects.equals(identity, that.identity)
               && Objects.equals(role, that.role);
    }
}
