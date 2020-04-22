package no.unit.publication.service;

import no.unit.nva.model.Publication;
import no.unit.publication.model.PublicationSummary;
import nva.commons.exceptions.ApiGatewayException;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicationService {

    Publication getPublication(UUID identifier, String authorization)
            throws ApiGatewayException;

    Publication updatePublication(Publication publication, String authorization)
            throws ApiGatewayException;

    List<PublicationSummary> getPublicationsByPublisher(URI publisherId, String authorization)
            throws ApiGatewayException;

    List<PublicationSummary> getPublicationsByOwner(String owner, URI publisherId, String authorization)
            throws ApiGatewayException;

}
