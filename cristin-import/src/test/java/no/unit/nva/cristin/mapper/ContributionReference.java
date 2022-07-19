package no.unit.nva.cristin.mapper;

import java.util.Objects;

/**
 * Helper class when testing for correct mapping of Contribution sequences. During the mapping from Cristin objects to
 * NVA objects, we assume that we can identify uniquely a contribution by a resource identifier (Cristin result
 * identifier), a Cristin person identifier and a sequence number. That is to say, we expect to be able to identify a
 * Contribution sequence by saying that the "X person is the ith contributor in the Y publication".
 */
public class ContributionReference {
    
    private final Integer cristinResultId;
    private final Integer cristinPersonId;
    private final Integer sequence;
    
    public ContributionReference(Integer cristinResultId, Integer cristinPersonId, Integer sequence) {
        this.cristinResultId = cristinResultId;
        this.cristinPersonId = cristinPersonId;
        this.sequence = sequence;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(cristinResultId, cristinPersonId, sequence);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContributionReference)) {
            return false;
        }
        ContributionReference that = (ContributionReference) o;
        return Objects.equals(cristinResultId, that.cristinResultId)
               && Objects.equals(cristinPersonId, that.cristinPersonId)
               && Objects.equals(sequence, that.sequence);
    }
}
