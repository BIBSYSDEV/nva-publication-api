package no.unit.nva.publication.events.handlers.batch;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.publication.service.impl.ResourceService;

public final class ManuallyUpdatePublicationUtil {

    private final ResourceService resourceService;

    private ManuallyUpdatePublicationUtil(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public static ManuallyUpdatePublicationUtil create(ResourceService resourceService) {
        return new ManuallyUpdatePublicationUtil(resourceService);
    }

    public List<Publication> update(List<Publication> publications, ManuallyUpdatePublicationsRequest request) {
        return switch (request.type()) {
            case PUBLISHER -> updatePublisher(publications, request);
            case LICENSE -> updateLicense(publications, request);
        };
    }

    private static Publication update(Publication publication, String oldPublisher, String newPublisher) {
        var publicationContext = (Book) publication.getEntityDescription().getReference().getPublicationContext();
        var newPublicationContext = publicationContext.copy()
                                        .withPublisher(createNewPublisher(publication, oldPublisher, newPublisher))
                                        .build();
        publication.getEntityDescription().getReference().setPublicationContext(newPublicationContext);
        return publication;
    }

    private static Publisher createNewPublisher(Publication publication, String oldPublisher, String newPublisher) {
        return Optional.of(publication.getEntityDescription().getReference().getPublicationContext())
                   .map(Book.class::cast)
                   .map(Book::getPublisher)
                   .map(Publisher.class::cast)
                   .map(Publisher::getId)
                   .map(URI::toString)
                   .map(value -> value.replace(oldPublisher, newPublisher))
                   .map(URI::create)
                   .map(Publisher::new)
                   .orElseThrow();
    }

    private static boolean hasPublisher(Publication publication, String publisher) {
        return Optional.of(publication.getEntityDescription().getReference().getPublicationContext())
                   .filter(Book.class::isInstance)
                   .map(Book.class::cast)
                   .map(Book::getPublisher)
                   .filter(Publisher.class::isInstance)
                   .map(Publisher.class::cast)
                   .map(Publisher::getId)
                   .map(URI::toString)
                   .filter(value -> value.contains(publisher))
                   .isPresent();
    }

    private List<Publication> updateLicense(List<Publication> publications, ManuallyUpdatePublicationsRequest request) {
        return publications.stream()
                   .map(publication -> updateLicense(publication, request.oldValue(), request.newValue()))
                   .map(resourceService::updatePublication)
                   .toList();
    }

    private List<Publication> updatePublisher(List<Publication> publications,
                                              ManuallyUpdatePublicationsRequest request) {
        return publications.stream()
                   .filter(publication -> hasPublisher(publication, request.oldValue()))
                   .map(publication -> update(publication, request.oldValue(), request.newValue()))
                   .map(resourceService::updatePublication)
                   .toList();
    }

    private Publication updateLicense(Publication publication, String oldLicense, String newLicense) {
        var filesToUpdate = publication.getAssociatedArtifacts()
                                .stream()
                                .filter(File.class::isInstance)
                                .map(File.class::cast)
                                .filter(File::hasLicense)
                                .filter(file -> file.getLicense().toString().equals(oldLicense))
                                .toList();
        publication.getAssociatedArtifacts().removeAll(filesToUpdate);
        var updatedFiles = filesToUpdate.stream()
                               .map(file -> file.copy().withLicense(URI.create(newLicense)).build(file.getClass()))
                               .toList();
        publication.getAssociatedArtifacts().addAll(updatedFiles);
        return publication;
    }
}
