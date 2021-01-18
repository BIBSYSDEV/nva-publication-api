package no.unit.nva.publication.service;

import java.net.URI;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public interface PublicationService {

    Publication createPublication(Publication publication) throws ApiGatewayException;

    Publication getPublication(SortableIdentifier identifier)
        throws ApiGatewayException;

    Publication updatePublication(SortableIdentifier identifier, Publication publication)
        throws ApiGatewayException;

    List<PublicationSummary> getPublicationsByPublisher(URI publisherId)
        throws ApiGatewayException;

    List<PublicationSummary> getPublicationsByOwner(String owner, URI publisherId)
        throws ApiGatewayException;

    List<PublicationSummary> listPublishedPublicationsByDate(int pageSize)
            throws ApiGatewayException;

    PublishPublicationStatusResponse publishPublication(SortableIdentifier identifier)
        throws ApiGatewayException;

    void markPublicationForDeletion(SortableIdentifier identifier, String owner) throws ApiGatewayException;

    void deleteDraftPublication(SortableIdentifier identifier) throws ApiGatewayException;
}
