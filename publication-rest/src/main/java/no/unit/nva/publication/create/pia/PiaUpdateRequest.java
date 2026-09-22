package no.unit.nva.publication.create.pia;

import static nva.commons.core.attempt.Try.attempt;

import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record PiaUpdateRequest(
    PiaPublication publication, Integer cristinId, String externalId, String orcid, int sequenceNr)
    implements JsonSerializable {

  private static final Logger logger = LoggerFactory.getLogger(PiaUpdateRequest.class);

  private static final String NOT_NUMERIC_CRISTIN_IDENTIFIER_MESSAGE =
      "Skipping cristinId for contributor, identity id is not numeric: {}";

  public static PiaUpdateRequest toPiaRequest(Contributor contributor, String scopusId) {
    return new PiaUpdateRequest(
        createPiaPublication(scopusId),
        extractContributorCristinIdentifier(contributor),
        extractScopusAuid(contributor),
        extractOrcidIdentifier(contributor),
        contributor.sequence());
  }

  @Override
  public String toString() {
    return toJsonString();
  }

  private static PiaPublication createPiaPublication(String scopusId) {
    return new PiaPublication(scopusId, "SCOPUS");
  }

  private static String extractScopusAuid(Contributor contributor) {
    return extractAuid(contributor).orElseThrow().value();
  }

  private static String extractOrcidIdentifier(Contributor contributor) {
    return Optional.ofNullable(contributor.identity().getOrcId())
        .map(UriWrapper::fromUri)
        .map(UriWrapper::getLastPathElement)
        .orElse(null);
  }

  private static Integer extractContributorCristinIdentifier(Contributor contributor) {
    return Optional.ofNullable(contributor.identity().getId())
        .map(UriWrapper::fromUri)
        .map(UriWrapper::getLastPathElement)
        .flatMap(PiaUpdateRequest::parseCristinIdentifier)
        .orElse(null);
  }

  private static Optional<Integer> parseCristinIdentifier(String identifier) {
    return attempt(() -> Integer.parseInt(identifier))
        .toOptional(failure -> logger.error(NOT_NUMERIC_CRISTIN_IDENTIFIER_MESSAGE, identifier));
  }

  private static Optional<AdditionalIdentifier> extractAuid(Contributor contributor) {
    return contributor.identity().getAdditionalIdentifiers().stream()
        .filter(
            additionalIdentifier ->
                "scopus-auid".equalsIgnoreCase(additionalIdentifier.sourceName()))
        .findFirst();
  }
}
