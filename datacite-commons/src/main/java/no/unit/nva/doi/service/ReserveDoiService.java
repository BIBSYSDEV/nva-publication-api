package no.unit.nva.doi.service;

import static no.unit.nva.doi.handlers.ReserveDoiHandler.BAD_RESPONSE_ERROR_MESSAGE;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.doi.DataCiteDoiClient;
import no.unit.nva.doi.ReserveDoiRequestValidator;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationstate.DoiReservedEvent;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class ReserveDoiService {

    public static final String NOT_FOUND_MESSAGE = "Could not find resource ";
    private final ResourceService resourceService;
    private final DataCiteDoiClient reserveDoiClient;

    public ReserveDoiService(ResourceService resourceService, DataCiteDoiClient reserveDoiClient) {
        this.resourceService = resourceService;
        this.reserveDoiClient = reserveDoiClient;
    }

    public DoiResponse reserve(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        var resource = fetchResource(resourceIdentifier);
        ReserveDoiRequestValidator.validateRequest(userInstance, resource);
        return attempt(() -> reserveDoiClient.generateDraftDoi(resource))
                   .map(doi -> updatePublicationWithDoi(resource, doi, userInstance))
                   .orElseThrow(failure -> new BadGatewayException(BAD_RESPONSE_ERROR_MESSAGE));
    }

    private Resource fetchResource(SortableIdentifier resourceIdentifier) throws NotFoundException {
        return Resource.resourceQueryObject(resourceIdentifier)
                   .fetch(resourceService)
                   .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE + resourceIdentifier));
    }

    private DoiResponse updatePublicationWithDoi(Resource resource, URI doi, UserInstance userInstance) {
        var updatedResource = resource.copy()
                                  .withDoi(doi)
                                  .withResourceEvent(DoiReservedEvent.create(userInstance, Instant.now()))
                                  .build();
        resourceService.updateResource(updatedResource, userInstance);
        return new DoiResponse(doi);
    }
}
