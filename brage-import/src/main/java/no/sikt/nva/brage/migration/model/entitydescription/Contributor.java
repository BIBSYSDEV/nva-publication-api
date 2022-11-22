package no.sikt.nva.brage.migration.model.entitydescription;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;


@JacocoGenerated
public class Contributor {

    private String type;
    private Identity identity;
    private String role;

    private String brageRole;

    public Contributor() {
        
    }

    public Contributor(String type, Identity identity, String role, String brageRole) {
        this.type = type;
        this.identity = identity;
        this.role = role;
        this.brageRole = brageRole;
    }

    @JsonProperty("role")
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("identity")
    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public String getBrageRole() {
        return brageRole;
    }

    public void setBrageRole(String brageRole) {
        this.brageRole = brageRole;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(type, identity, role, brageRole);
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
        return Objects.equals(type, that.type)
               && Objects.equals(brageRole, that.brageRole)
               && Objects.equals(identity, that.identity)
               && Objects.equals(role, that.role);
    }
}
