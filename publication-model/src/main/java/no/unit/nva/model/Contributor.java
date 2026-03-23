package no.unit.nva.model;

import static no.unit.nva.model.util.SerializationUtils.nullListAsEmpty;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import no.unit.nva.model.role.RoleType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record Contributor(
    Identity identity,
    List<Corporation> affiliations,
    RoleType role,
    Integer sequence,
    boolean correspondingAuthor) {

  public Contributor {
    affiliations = nullListAsEmpty(affiliations);
  }

  public static final class Builder {

    private Identity identity;
    private List<Corporation> affiliations;
    private RoleType role;
    private Integer sequence;
    private boolean correspondingAuthor;

    public Builder withIdentity(Identity identity) {
      this.identity = identity;
      return this;
    }

    public Builder withAffiliations(List<Corporation> affiliations) {
      this.affiliations = affiliations;
      return this;
    }

    public Builder withRole(RoleType role) {
      this.role = role;
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
      return new Contributor(identity, affiliations, role, sequence, correspondingAuthor);
    }
  }

  public Builder copy() {
    return new Builder()
        .withAffiliations(this.affiliations())
        .withSequence(this.sequence())
        .withCorrespondingAuthor(this.correspondingAuthor())
        .withRole(this.role())
        .withIdentity(this.identity());
  }
}
