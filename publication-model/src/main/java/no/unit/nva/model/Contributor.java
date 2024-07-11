package no.unit.nva.model;

import static no.unit.nva.model.util.SerializationUtils.nullListAsEmpty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Contributor {

    private final Identity identity;
    private final List<Corporation> affiliations;
    private final RoleType role;
    private final Integer sequence;
    private final boolean correspondingAuthor;

    @JsonCreator
    public Contributor(@JsonProperty("identity") Identity identity,
                       @JsonProperty("affiliations") List<Corporation> affiliations,
                       @JsonProperty("role") Object role,
                       @JsonProperty("sequence") Integer sequence,
                       @JsonProperty("correspondingAuthor") boolean correspondingAuthor) {
        this.identity = identity;
        this.affiliations = nullListAsEmpty(affiliations);
        this.role = roleFromJson(role);
        this.sequence = sequence;
        this.correspondingAuthor = correspondingAuthor;
    }

    protected Contributor(Builder builder) {
        this(
            builder.identity,
            builder.affiliations,
            builder.role,
            builder.sequence,
            builder.correspondingAuthor
        );
    }

    public Identity getIdentity() {
        return identity;
    }

    public List<Corporation> getAffiliations() {
        return affiliations;
    }

    public Integer getSequence() {
        return sequence;
    }

    public RoleType getRole() {
        return role;
    }

    public boolean isCorrespondingAuthor() {
        return correspondingAuthor;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentity(),
                            getAffiliations(),
                            getRole(),
                            getSequence(),
                            isCorrespondingAuthor());
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
        return isCorrespondingAuthor() == that.isCorrespondingAuthor()
               && Objects.equals(getIdentity(), that.getIdentity())
               && Objects.equals(getAffiliations(), that.getAffiliations())
               && Objects.equals(getRole(), that.getRole())
               && Objects.equals(getSequence(), that.getSequence());
    }

    @Deprecated
    private static RoleType getRoleFromString(Object role) {
        return role instanceof String
                   ? new RoleType(Role.parse(role.toString()))
                   : (RoleType) role;
    }

    @Deprecated
    private static RoleType getType(Map<?, ?> role) {
        var tap = (String) role.get("type");
        var type = Role.parse(tap);
        var description = Optional.ofNullable(role.get("description"));
        var roleType = new RoleType(type);
        return description.isEmpty()
                   ? roleType
                   : roleType.createOther((String) description.get());
    }

    @Deprecated
    private RoleType roleFromJson(Object role) {
        return role instanceof LinkedHashMap
                   ? getType((LinkedHashMap<?, ?>) role)
                   : getRoleFromString(role);
    }

    public static final class Builder {

        private Identity identity;
        private List<Corporation> affiliations;
        private Integer sequence;
        private RoleType role;
        private boolean correspondingAuthor;

        public Builder() {
        }

        public Builder withIdentity(Identity identity) {
            this.identity = identity;
            return this;
        }

        public Builder withAffiliations(List<Corporation> affiliations) {
            this.affiliations = affiliations;
            return this;
        }

        public Builder withRole(RoleType type) {
            this.role = type;
            return this;
        }

        public Builder withSequence(Integer sequence) {
            this.sequence = sequence;
            return this;
        }

        public Builder withCorrespondingAuthor(boolean correspondingAuthor) {
            this.correspondingAuthor = correspondingAuthor;
            return this;
        }

        public Contributor build() {
            return new Contributor(this);
        }
    }

    public Builder copy() {
        return new Builder()
                .withAffiliations(this.getAffiliations())
                .withSequence(this.getSequence())
                .withCorrespondingAuthor(this.isCorrespondingAuthor())
                .withRole(this.getRole())
                .withIdentity(this.getIdentity());
    }
}
