package no.unit.nva.service.impl;

import no.unit.nva.Environment;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationSummary;
import no.unit.nva.service.PublicationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DynamoDBPublicationService implements PublicationService {

    public static final String NOT_IMPLEMENTED = "Not implemented";

    /**
     * Constructor for DynamoDBPublicationService.
     *
     */
    public DynamoDBPublicationService() {

    }

    /**
     * Creator helper method for DynamoDBPublicationService.
     *
     * @return  DynamoDBPublicationService
     */
    public static DynamoDBPublicationService create(Environment environment) {
        return new DynamoDBPublicationService();
    }

    @Override
    public Optional<Publication> getPublication(UUID identifier, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public Publication updatePublication(Publication publication, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public List<PublicationSummary> getPublicationsByPublisher(String publisherId, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public List<PublicationSummary> getPublicationsByOwner(String owner, String publisherId, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }
}
