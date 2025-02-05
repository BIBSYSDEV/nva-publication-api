package no.unit.nva.publication.events.handlers.batch;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;

public final class ManuallyUpdatePublicationUtil {

    private final ResourceService resourceService;

    private ManuallyUpdatePublicationUtil(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public static ManuallyUpdatePublicationUtil create(ResourceService resourceService) {
        return new ManuallyUpdatePublicationUtil(resourceService);
    }

    public void update(List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
        switch (request.type()) {
            case PUBLISHER -> updatePublisher(resources, request);
            case LICENSE -> updateLicense(resources, request);
        };
    }

    private static Resource update(Resource resource, String oldPublisher, String newPublisher) {
        var publicationContext = (Book) resource.getEntityDescription().getReference().getPublicationContext();
        var newPublicationContext = publicationContext.copy()
                                        .withPublisher(createNewPublisher(resource, oldPublisher, newPublisher))
                                        .build();
        resource.getEntityDescription().getReference().setPublicationContext(newPublicationContext);
        return resource;
    }

    private static Publisher createNewPublisher(Resource resource, String oldPublisher, String newPublisher) {
        return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
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

    private static boolean hasPublisher(Resource resource, String publisher) {
        return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
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

    private void updateLicense(List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
        resources.forEach(resource -> updateFiles(resource, request));
    }

    private void updateFiles(Resource resource, ManuallyUpdatePublicationsRequest request) {
        resource.getFileEntries().stream()
            .filter(fileEntry -> hasLicense(fileEntry, request.oldValue()))
            .forEach(fileEntry -> updateLicense(fileEntry, request.newValue()));
    }

    private void updateLicense(FileEntry fileEntry, String license) {
        var file = fileEntry.getFile().copy()
                       .withLicense(URI.create(license))
                       .build(fileEntry.getFile().getClass());
        fileEntry.update(file, resourceService);
    }

    private boolean hasLicense(FileEntry fileEntry, String oldValue) {
        return fileEntry.getFile().getLicense().toString().equals(oldValue);
    }

    private void updatePublisher(List<Resource> resources,
                                              ManuallyUpdatePublicationsRequest request) {
        resources.stream()
            .filter(resource -> hasPublisher(resource, request.oldValue()))
            .map(resource -> update(resource, request.oldValue(), request.newValue()))
            .forEach(resourceService::updateResource);
    }
}
