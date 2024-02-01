package no.unit.nva.publication.model.business.importcandidate;

import java.util.List;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class NvaCustomerContributor extends Contributor {

    private final NvaCustomer nvaCustomer;

    public NvaCustomerContributor(Identity identity, List<Corporation> affiliations, Object role, Integer sequence,
                                  boolean correspondingAuthor, NvaCustomer hasNvaCustomer) {
        super(identity, affiliations, role, sequence, correspondingAuthor);
        this.nvaCustomer = hasNvaCustomer;
    }

    public NvaCustomer getNvaCustomer() {
        return nvaCustomer;
    }

    public boolean belongsToNvaCustomer() {
        return nvaCustomer.isCustomer();
    }

    @JacocoGenerated
    public static final class Builder {

        private Identity identity;
        private List<Corporation> affiliations;
        private Integer sequence;
        private RoleType role;
        private boolean correspondingAuthor;
        private NvaCustomer nvaCustomer;

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

        public Builder withNvaCustomer(NvaCustomer nvaCustomer) {
            this.nvaCustomer = nvaCustomer;
            return this;
        }

        public NvaCustomerContributor build() {
            return new NvaCustomerContributor(identity, affiliations, role,
                                              sequence, correspondingAuthor, nvaCustomer);
        }
    }
}
