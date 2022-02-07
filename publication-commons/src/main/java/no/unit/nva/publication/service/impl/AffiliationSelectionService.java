package no.unit.nva.publication.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.publication.external.services.PersonApiClient;
import no.unit.nva.publication.utils.OrgUnitId;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AffiliationSelectionService {

    private final PersonApiClient personApiClient;

    private static final Logger logger = LoggerFactory.getLogger(AffiliationSelectionService.class);

    private AffiliationSelectionService(PersonApiClient personApiClient) {
        this.personApiClient = personApiClient;
    }

    public static AffiliationSelectionService create(HttpClient httpClientToExternalServices) {
        return new AffiliationSelectionService(new PersonApiClient(httpClientToExternalServices));
    }

    public Optional<URI> fetchAffiliation(String feideId)
        throws ApiGatewayException {
        logger.debug("Fetching affiliation feideid:{}", feideId);
        try {
            var affiliations = fetchAffiliationUris(feideId);
            return OrgUnitId.extractMostLikelyAffiliationForUser(affiliations)
                .map(OrgUnitId::getUnitId);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<OrgUnitId> fetchAffiliationUris(String feideId)
        throws IOException, ApiGatewayException, InterruptedException {
        return personApiClient.fetchAffiliationsForUser(feideId)
            .stream()
            .peek(uri -> logger.debug("Found affiliation uri:{}",uri))
            .map(OrgUnitId::new)
            .collect(Collectors.toList());
    }
}
