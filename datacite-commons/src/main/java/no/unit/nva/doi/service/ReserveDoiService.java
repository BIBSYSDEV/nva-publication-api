package no.unit.nva.doi.service;

import static no.unit.nva.doi.handlers.ReserveDoiHandler.BAD_RESPONSE_ERROR_MESSAGE;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import no.unit.nva.doi.DataCiteDoiClient;
import no.unit.nva.doi.ReserveDoiRequestValidator;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;

public class ReserveDoiService {

    private final ResourceService resourceService;
    private final DataCiteDoiClient reserveDoiClient;

    public ReserveDoiService(ResourceService resourceService, DataCiteDoiClient reserveDoiClient) {
        this.resourceService = resourceService;
        this.reserveDoiClient = reserveDoiClient;
    }

    public DoiResponse reserve(String owner, SortableIdentifier publicationIdentifier) throws ApiGatewayException {
        var publication = fetchPublication(publicationIdentifier);
        ReserveDoiRequestValidator.validateRequest(owner, publication);
        return attempt(() -> reserveDoiClient.generateDraftDoi(publication))
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
        resourceService.updatePublication(updatedPublication);
        return new DoiResponse(doi);
    }
}
