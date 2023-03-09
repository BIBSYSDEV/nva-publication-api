package no.unit.nva.doi.service;

import static no.unit.nva.doi.handlers.ReserveDoiHandler.BAD_RESPONSE_ERROR_MESSAGE;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import no.unit.nva.doi.DataCiteReserveDoiClient;
import no.unit.nva.doi.ReserveDoiRequestValidator;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReserveDoiService {

    private static final Logger logger = LoggerFactory.getLogger(ReserveDoiService.class);
    private final ResourceService resourceService;
    private final DataCiteReserveDoiClient reserveDoiClient;

    public ReserveDoiService(ResourceService resourceService, DataCiteReserveDoiClient reserveDoiClient) {
        this.resourceService = resourceService;
        this.reserveDoiClient = reserveDoiClient;
    }

    public DoiResponse reserve(String owner, SortableIdentifier publicationIdentifier) throws ApiGatewayException {
        var publication = fetchPublication(publicationIdentifier);
        ReserveDoiRequestValidator.validateRequest(owner, publication);
        logger.info("Reserve doi request for publication: {}", publicationIdentifier);
        return attempt(() -> reserveDoiClient.generateDoi(publication))
                   .map(doi -> updatePublicationWithDoi(publication, doi))
                   .orElseThrow(failure -> new BadGatewayException(BAD_RESPONSE_ERROR_MESSAGE));
    }

    private Publication fetchPublication(SortableIdentifier identifier) throws ApiGatewayException {
        return resourceService.getPublicationByIdentifier(identifier);
    }

    private DoiResponse updatePublicationWithDoi(Publication publication, URI doi) {
        var updatedPublication = publication.copy()
                                     .withDoi(doi)
                                     .build();
        logger.info("Draft doi is: {}", doi);
        resourceService.updatePublication(updatedPublication);
        return new DoiResponse(doi);
    }
}
