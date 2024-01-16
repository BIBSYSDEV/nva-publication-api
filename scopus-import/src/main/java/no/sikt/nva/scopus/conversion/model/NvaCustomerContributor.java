package no.sikt.nva.scopus.conversion.model;

import java.util.List;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.role.RoleType;

public class NvaCustomerContributor extends Contributor {

    private final boolean hasNvaCustomer;

    public NvaCustomerContributor(Identity identity, List<Corporation> affiliations, Object role, Integer sequence,
                                  boolean correspondingAuthor, boolean hasNvaCustomer) {
        super(identity, affiliations, role, sequence, correspondingAuthor);
        this.hasNvaCustomer = hasNvaCustomer;
    }

    public boolean belongsToNvaCustomer() {
        return hasNvaCustomer;
    }

    public static final class Builder {

        private Identity identity;
        private List<Corporation> affiliations;
        private Integer sequence;
        private RoleType role;
        private boolean correspondingAuthor;
        private boolean belongsToNvaCustomer;

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

        public Builder withBelongsToNvaCustomer(boolean belongsToNvaCustomer) {
            this.belongsToNvaCustomer = belongsToNvaCustomer;
            return this;
        }

        public NvaCustomerContributor build() {
            return new NvaCustomerContributor(identity, affiliations, role,
                                              sequence, correspondingAuthor, belongsToNvaCustomer);
        }
    }
}
