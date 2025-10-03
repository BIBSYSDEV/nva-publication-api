package no.unit.nva.publication.events.handlers.batch;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public final class ManuallyUpdatePublicationUtil {

    private static final String API_HOST = "API_HOST";
    private static final String PUBLICATION_CHANNELS_V2_PATH_PARAM = "publication-channels-v2";
    private static final String PUBLISHER = "publisher";
    private static final String SERIAL_PUBLICATION = "serial-publication";
    public static final String CRISTIN = "cristin";
    public static final String PERSON = "person";
    private final ResourceService resourceService;
    private final Environment environment;

    private ManuallyUpdatePublicationUtil(ResourceService resourceService, Environment environment) {
        this.resourceService = resourceService;
        this.environment = environment;
    }

    public static ManuallyUpdatePublicationUtil create(ResourceService resourceService, Environment environment) {
        return new ManuallyUpdatePublicationUtil(resourceService, environment);
    }

    public void update(List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
        switch (request.type()) {
            case PUBLISHER -> updatePublisher(resources, request);
            case SERIAL_PUBLICATION -> updateSeriesOrJournal(resources, request);
            case LICENSE -> updateLicense(resources, request);
            case UNCONFIRMED_PUBLISHER -> updateUnconfirmedPublisher(resources, request);
            case UNCONFIRMED_SERIES -> updateUnconfirmedSeries(resources, request);
            case UNCONFIRMED_JOURNAL -> updateUnconfirmedJournal(resources, request);
            case CONTRIBUTOR_IDENTIFIER -> updateContributorIdentifier(resources, request);
        }
    }

    private void updateContributorIdentifier(List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
        resources.stream()
            .filter(resource -> hasContributor(resource, request.oldValue()))
            .map(resource -> updateContributorIdentifier(resource, request.oldValue(), request.newValue()))
            .forEach(resource -> resourceService.updateResource(resource, UserInstance.fromPublication(resource.toPublication())));
    }

    private Resource updateContributorIdentifier(Resource resource, String oldContributorIdentifier,
                                                 String newContributorIdentifier) {
        var contributors = new ArrayList<>(resource.getEntityDescription().getContributors());
        var contributorToUpdate = resource.getEntityDescription().getContributors().stream()
                                      .filter(contributor -> contributor.getIdentity().getId().toString().contains(oldContributorIdentifier))
                                      .findFirst()
                                      .orElseThrow();
        contributors.remove(contributorToUpdate);
        contributorToUpdate.getIdentity().setId(createContributorId(newContributorIdentifier));
        contributors.add(contributorToUpdate);
        resource.getEntityDescription().setContributors(contributors);
        return resource;
    }

    private URI createContributorId(String newContributorIdentifier) {
        return UriWrapper.fromHost(environment.readEnv(API_HOST))
                   .addChild(CRISTIN)
                   .addChild(PERSON)
                   .addChild(newContributorIdentifier)
                   .getUri();
    }

    private void updateUnconfirmedSeries(List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
        resources.stream()
            .filter(resource -> hasUnconfirmedSeries(resource, request.oldValue()))
            .map(resource -> updateUnconfirmedSeriesToConfirmed(resource, request.newValue()))
            .forEach(resource -> resourceService.updateResource(resource, UserInstance.fromPublication(resource.toPublication())));
    }

    private Resource updateUnconfirmedSeriesToConfirmed(Resource resource, String pid) {
        var book = (Book) resource.getEntityDescription().getReference().getPublicationContext();
        var publicationYear = resource.getEntityDescription().getPublicationDate().getYear();
        var series = new Series(constructPublicationChannelUri(SERIAL_PUBLICATION, publicationYear, pid));
        var newBook = book.copy()
                          .withSeries(series)
                          .build();
        resource.getEntityDescription().getReference().setPublicationContext(newBook);
        return resource;
    }

    private boolean hasUnconfirmedSeries(Resource resource, String seriesTitle) {
        return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
                   .filter(Book.class::isInstance)
                   .map(Book.class::cast)
                   .map(Book::getSeries)
                   .filter(UnconfirmedSeries.class::isInstance)
                   .map(UnconfirmedSeries.class::cast)
                   .map(UnconfirmedSeries::getTitle)
                   .filter(value -> value.equals(seriesTitle))
                   .isPresent();
    }

    private boolean hasContributor(Resource resource, String contributorIdentifier) {
        return Optional.ofNullable(resource.getEntityDescription())
                   .map(EntityDescription::getContributors)
                   .stream()
                   .flatMap(List::stream)
                   .map(Contributor::getIdentity)
                   .map(Identity::getId)
                   .map(URI::toString)
                   .anyMatch(id -> id.contains(contributorIdentifier));
    }

    private void updateUnconfirmedJournal(List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
        resources.stream()
            .filter(resource -> hasUnconfirmedJournal(resource, request.oldValue()))
            .map(resource -> updateUnconfirmedJournalToConfirmed(resource, request.newValue()))
            .forEach(resource -> resourceService.updateResource(resource, UserInstance.fromPublication(resource.toPublication())));
    }

    private Resource updateUnconfirmedJournalToConfirmed(Resource resource, String pid) {
        var publicationYear = resource.getEntityDescription().getPublicationDate().getYear();
        var journal = new Journal(constructPublicationChannelUri(SERIAL_PUBLICATION, publicationYear, pid));
        resource.getEntityDescription().getReference().setPublicationContext(journal);
        return resource;
    }

    private boolean hasUnconfirmedJournal(Resource resource, String journalTitle) {
        return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
                   .filter(UnconfirmedJournal.class::isInstance)
                   .map(UnconfirmedJournal.class::cast)
                   .map(UnconfirmedJournal::getTitle)
                   .filter(value -> value.equals(journalTitle))
                   .isPresent();
    }

    private void updateUnconfirmedPublisher(List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
        resources.stream()
            .filter(resource -> hasUnconfirmedPublisher(resource, request.oldValue()))
            .map(resource -> updateUnconfirmedPublisherToConfirmed(resource, request.newValue()))
            .forEach(resource -> resourceService.updateResource(resource, UserInstance.fromPublication(resource.toPublication())));
    }

    private Resource updateUnconfirmedPublisherToConfirmed(Resource resource, String pid) {
        var book = (Book) resource.getEntityDescription().getReference().getPublicationContext();
        var publicationYear = resource.getEntityDescription().getPublicationDate().getYear();
        var publisher = new Publisher(constructPublicationChannelUri(PUBLISHER, publicationYear, pid));
        var newBook = book.copy()
                          .withPublisher(publisher)
                          .build();
        resource.getEntityDescription().getReference().setPublicationContext(newBook);
        return resource;
    }

    private URI constructPublicationChannelUri(String type, String year, String pid) {
        return UriWrapper.fromHost(environment.readEnv(API_HOST))
                   .addChild(PUBLICATION_CHANNELS_V2_PATH_PARAM)
                   .addChild(type)
                   .addChild(pid)
                   .addChild(year)
                   .getUri();
    }

    private boolean hasUnconfirmedPublisher(Resource resource, String publisherName) {
        return getPublisher(resource)
            .filter(UnconfirmedPublisher.class::isInstance)
            .map(UnconfirmedPublisher.class::cast)
            .map(UnconfirmedPublisher::getName)
            .filter(value -> value.equals(publisherName))
            .isPresent();
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
        return getPublisher(resource)
                   .map(Publisher.class::cast)
                   .map(Publisher::getId)
                   .map(URI::toString)
                   .map(value -> value.replace(oldPublisher, newPublisher))
                   .map(URI::create)
                   .map(Publisher::new)
                   .orElseThrow();
    }

    private static boolean hasPublisher(Resource resource, String publisher) {
        return getPublisher(resource)
                   .filter(Publisher.class::isInstance)
                   .map(Publisher.class::cast)
                   .map(Publisher::getId)
                   .map(URI::toString)
                   .filter(value -> value.contains(publisher))
                   .isPresent();
    }

    private static Optional<PublishingHouse> getPublisher(Resource resource) {
        return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
                   .filter(Book.class::isInstance)
                   .map(Book.class::cast)
                   .map(Book::getPublisher);
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
