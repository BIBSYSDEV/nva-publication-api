package no.unit.nva.service.impl;

import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationSummary;
import no.unit.nva.service.PublicationService;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DynamoDBPublicationService implements PublicationService {

    public static final String NOT_IMPLEMENTED = "Not implemented";

    @Override
    public Optional<Publication> getPublication(UUID identifier, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public Publication updatePublication(Publication publication, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public List<PublicationSummary> getPublicationsByPublisher(URI publisherId, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public List<PublicationSummary> getPublicationsByOwner(String owner, URI publisherId, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }
}
