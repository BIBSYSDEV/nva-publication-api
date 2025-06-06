package no.unit.nva.publication.events.handlers.batch;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
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
            case SERIAL_PUBLICATION -> updateSeriesOrJournal(resources, request);
            case LICENSE -> updateLicense(resources, request);
        }
    }

    private void updateSeriesOrJournal(List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
        resources.stream()
            .filter(resource -> hasSerialPublication(resource, request.oldValue()))
            .map(resource -> updateSeriesOrJournal(resource, request.oldValue(), request.newValue()))
            .forEach(resource -> resourceService.updateResource(resource, UserInstance.fromPublication(resource.toPublication())));
    }

    private boolean hasSerialPublication(Resource resource, String value) {
        var publicationContext = resource.getEntityDescription().getReference().getPublicationContext();
        if (publicationContext instanceof Book book && book.getSeries() instanceof Series series) {
            return series.getId().toString().contains(value);
        }
        if (publicationContext instanceof Journal journal) {
            return journal.getId().toString().contains(value);
        }
        return false;
    }

    private Resource updateSeriesOrJournal(Resource resource, String oldValue, String newValue) {
        var publicationContext = resource.getEntityDescription().getReference().getPublicationContext();
        if (publicationContext instanceof Book book && book.getSeries() instanceof Series series) {
            var newSeries = new Series(URI.create(series.getId().toString().replace(oldValue, newValue)));
            var newPublicationContext = book.copy().withSeries(newSeries).build();
            resource.getEntityDescription().getReference().setPublicationContext(newPublicationContext);
            return resource;
        }
        if (publicationContext instanceof Journal journal) {
            var newJournal = new Journal(URI.create(journal.getId().toString().replace(oldValue, newValue)));
            resource.getEntityDescription().getReference().setPublicationContext(newJournal);
            return resource;
        }
        return resource;
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

    private void updateLicense(FileEntry fileEntry, Resource resource, String license) {
        var file = fileEntry.getFile().copy().withLicense(URI.create(license)).build(fileEntry.getFile().getClass());
        fileEntry.update(file, UserInstance.fromPublication(resource.toPublication()), resourceService);
    }

    private void updateFiles(Resource resource, ManuallyUpdatePublicationsRequest request) {
        resource.getFileEntries()
            .stream()
            .filter(fileEntry -> hasLicense(fileEntry, request.oldValue()))
            .forEach(fileEntry -> updateLicense(fileEntry, resource, request.newValue()));
    }

    private boolean hasLicense(FileEntry fileEntry, String oldValue) {
        return fileEntry.getFile().getLicense().toString().equals(oldValue);
    }

    private void updatePublisher(List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
        resources.stream()
            .filter(resource -> hasPublisher(resource, request.oldValue()))
            .map(resource -> update(resource, request.oldValue(), request.newValue()))
            .forEach(resource -> resourceService.updateResource(resource, UserInstance.fromPublication(resource.toPublication())));
    }
}
