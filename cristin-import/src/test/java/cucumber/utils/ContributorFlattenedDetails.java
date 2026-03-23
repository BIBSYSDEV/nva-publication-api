package cucumber.utils;

import cucumber.utils.transformers.CristinContributorTransformer;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import nva.commons.core.SingletonCollector;

public class ContributorFlattenedDetails implements JsonSerializable {

  private final String name;
  private final int sequence;
  private final URI affiliationUri;

  private ContributorFlattenedDetails(String name, int sequence, URI affiliationUri) {
    this.name = name;
    this.sequence = sequence;
    this.affiliationUri = affiliationUri;
  }

  public static ContributorFlattenedDetails extractNameAndSequence(Contributor contributor) {
    var name = contributor.identity().getName();
    var sequence = contributor.sequence();
    return new ContributorFlattenedDetails(name, sequence, null);
  }

  public static ContributorFlattenedDetails extractNameSequenceAndAffiliationUri(
      Contributor contributor) {
    var name = contributor.identity().getName();
    var sequence = contributor.sequence();
    var affiliationUri =
        ((Organization) contributor.affiliations().stream().collect(SingletonCollector.collect()))
            .getId();
    return new ContributorFlattenedDetails(name, sequence, affiliationUri);
  }

  public static ContributorFlattenedDetails fromDataTableMapEntry(Map<String, String> mapEntry) {
    var name = mapEntry.get(CristinContributorTransformer.TABLE_FIELD_FOR_EXPECTED_NVA_NAME);
    var sequence =
        Integer.parseInt(mapEntry.get(CristinContributorTransformer.CONTRIBUTOR_ORDINAL_NUMBER));
    var uri =
        Optional.ofNullable(
                mapEntry.get(
                    CristinContributorTransformer.TABLE_FIELD_FOR_EXPECTED_AFFILIATION_URI))
            .map(URI::create)
            .orElse(null);
    return new ContributorFlattenedDetails(name, sequence, uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getSequence(), getAffiliationUri());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ContributorFlattenedDetails that)) {
      return false;
    }
      return getSequence() == that.getSequence()
        && Objects.equals(getName(), that.getName())
        && Objects.equals(getAffiliationUri(), that.getAffiliationUri());
  }

  @Override
  public String toString() {
    return toJsonString();
  }

  public String getName() {
    return name;
  }

  public int getSequence() {
    return sequence;
  }

  public URI getAffiliationUri() {
    return affiliationUri;
  }
}
