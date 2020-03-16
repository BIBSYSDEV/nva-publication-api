package no.unit.nva.service;

import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationSummary;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicationService {

    Optional<Publication> getPublication(UUID identifier, String authorization)
            throws IOException, InterruptedException;

    Publication updatePublication(Publication publication, String authorization)
            throws IOException, InterruptedException;

    List<PublicationSummary> getPublicationsByPublisher(String publisherId, String authorization)
            throws IOException, InterruptedException;

    List<PublicationSummary> getPublicationsByOwner(String owner, String publisherId, String authorization)
            throws IOException, InterruptedException;

}
