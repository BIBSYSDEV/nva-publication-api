package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.publication.events.handlers.batch.Comparator.CONTAINS;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ManuallyUpdatePublicationUtil {

    private static final Logger logger = LoggerFactory.getLogger(ManuallyUpdatePublicationUtil.class);
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
            case PUBLISHER -> updateResources(resources, request, this::hasPublisher, this::updatePublisher);
            case SERIAL_PUBLICATION -> updateResources(resources, request, this::hasSerialPublication, this::updateSeriesOrJournal);
            case LICENSE -> updateLicenseFiles(resources, request);
            case UNCONFIRMED_PUBLISHER -> updateResources(resources, request,
                                                          (r, val) -> hasUnconfirmedPublisher(r, val, request.comparator()),
                                                          (r, req) -> updateUnconfirmedToConfirmed(r, req, PUBLISHER, this::createBookWithPublisher));
            case UNCONFIRMED_SERIES -> updateResources(resources, request, (r, val) -> hasUnconfirmedSeries(r, val, request.comparator()),
                                                       (r, req) -> updateUnconfirmedToConfirmed(r, req, SERIAL_PUBLICATION, this::createBookWithSeries));
            case UNCONFIRMED_JOURNAL -> updateResources(resources, request, (r, val) -> hasUnconfirmedJournal(r, val,
                                                                                                        request.comparator()), this::updateUnconfirmedJournalToConfirmed);
            case CONTRIBUTOR_IDENTIFIER -> updateResources(resources, request, this::hasContributor, this::updateContributorIdentifier);
        }
    }

    private void updateResources(List<Resource> resources, ManuallyUpdatePublicationsRequest request,
                                 BiPredicate<Resource, String> filter,
                                 BiFunction<Resource, ManuallyUpdatePublicationsRequest, Resource> updater) {
        var publicationsToUpdate = resources.stream()
                                       .filter(resource -> filter.test(resource, request.oldValue()))
                                       .map(resource -> updater.apply(resource, request))
                                       .toList();

        logUpdate(request, publicationsToUpdate);
        publicationsToUpdate.forEach(resource ->
                                         resourceService.updateResource(resource, UserInstance.fromPublication(resource.toPublication())));
    }

    private Resource updateContributorIdentifier(Resource resource, ManuallyUpdatePublicationsRequest request) {
        var contributors = new ArrayList<>(resource.getEntityDescription().getContributors());
        var contributorToUpdate = contributors.stream()
                                      .filter(c -> c.getIdentity().getId().toString().contains(request.oldValue()))
                                      .findFirst()
                                      .orElseThrow();

        contributors.remove(contributorToUpdate);
        contributorToUpdate.getIdentity().setId(buildUri(CRISTIN, PERSON, request.newValue()));
        contributors.add(contributorToUpdate);
        resource.getEntityDescription().setContributors(contributors);
        return resource;
    }

    private Resource updateUnconfirmedToConfirmed(Resource resource, ManuallyUpdatePublicationsRequest request,
                                                  String channelType, BiFunction<Book, URI, Book> bookUpdater) {
        var book = (Book) resource.getEntityDescription().getReference().getPublicationContext();
        var year = resource.getEntityDescription().getPublicationDate().getYear();
        var channelUri = buildPublicationChannelUri(channelType, year, request.newValue());
        var newBook = bookUpdater.apply(book, channelUri);
        resource.getEntityDescription().getReference().setPublicationContext(newBook);
        return resource;
    }

    private Book createBookWithPublisher(Book book, URI publisherUri) {
        return book.copy().withPublisher(new Publisher(publisherUri)).build();
    }

    private Book createBookWithSeries(Book book, URI seriesUri) {
        return book.copy().withSeries(new Series(seriesUri)).build();
    }

    private Resource updateUnconfirmedJournalToConfirmed(Resource resource, ManuallyUpdatePublicationsRequest request) {
        var year = resource.getEntityDescription().getPublicationDate().getYear();
        var journalUri = buildPublicationChannelUri(SERIAL_PUBLICATION, year, request.newValue());
        resource.getEntityDescription().getReference().setPublicationContext(new Journal(journalUri));
        return resource;
    }

    private boolean hasUnconfirmedPublisher(Resource resource, String publisherName, Comparator comparator) {
        return getPublishingHouse(resource, UnconfirmedPublisher.class)
                   .map(UnconfirmedPublisher::getName)
                   .filter(value -> CONTAINS.equals(comparator) ? value.contains(publisherName) : value.equals(publisherName))
                   .isPresent();
    }

    private boolean hasUnconfirmedSeries(Resource resource, String seriesTitle, Comparator comparator) {
        return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
                   .filter(Book.class::isInstance)
                   .map(Book.class::cast)
                   .map(Book::getSeries)
                   .filter(UnconfirmedSeries.class::isInstance)
                   .map(UnconfirmedSeries.class::cast)
                   .map(UnconfirmedSeries::getTitle)
                   .filter(value -> CONTAINS.equals(comparator) ? value.contains(seriesTitle) : value.equals(seriesTitle))
                   .isPresent();
    }

    private boolean hasUnconfirmedJournal(Resource resource, String journalTitle, Comparator comparator) {
        return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
                   .filter(UnconfirmedJournal.class::isInstance)
                   .map(UnconfirmedJournal.class::cast)
                   .map(UnconfirmedJournal::getTitle)
                   .filter(value -> CONTAINS.equals(comparator) ? value.contains(journalTitle) : value.equals(journalTitle))
                   .isPresent();
    }

    private boolean hasContributor(Resource resource, String contributorId) {
        return Optional.ofNullable(resource.getEntityDescription())
                   .map(EntityDescription::getContributors)
                   .stream()
                   .flatMap(List::stream)
                   .map(Contributor::getIdentity)
                   .map(Identity::getId)
                   .map(URI::toString)
                   .anyMatch(id -> id.contains(contributorId));
    }

    private boolean hasSerialPublication(Resource resource, String value) {
        var context = resource.getEntityDescription().getReference().getPublicationContext();
        if (context instanceof Book book && book.getSeries() instanceof Series series) {
            return series.getId().toString().contains(value);
        }
        return context instanceof Journal journal && journal.getId().toString().contains(value);
    }

    private Resource updateSeriesOrJournal(Resource resource, ManuallyUpdatePublicationsRequest request) {
        var context = resource.getEntityDescription().getReference().getPublicationContext();
        var reference = resource.getEntityDescription().getReference();

        if (context instanceof Book book && book.getSeries() instanceof Series series) {
            var newSeriesUri = URI.create(series.getId().toString().replace(request.oldValue(), request.newValue()));
            reference.setPublicationContext(book.copy().withSeries(new Series(newSeriesUri)).build());
        } else if (context instanceof Journal journal) {
            var newJournalUri = URI.create(journal.getId().toString().replace(request.oldValue(), request.newValue()));
            reference.setPublicationContext(new Journal(newJournalUri));
        }
        return resource;
    }

    private Resource updatePublisher(Resource resource, ManuallyUpdatePublicationsRequest request) {
        var book = (Book) resource.getEntityDescription().getReference().getPublicationContext();
        var publisherUri = getPublishingHouse(resource, Publisher.class)
                               .map(Publisher::getId)
                               .map(URI::toString)
                               .map(uri -> uri.replace(request.oldValue(), request.newValue()))
                               .map(URI::create)
                               .orElseThrow();

        resource.getEntityDescription().getReference()
            .setPublicationContext(book.copy().withPublisher(new Publisher(publisherUri)).build());
        return resource;
    }

    private boolean hasPublisher(Resource resource, String publisher) {
        return getPublishingHouse(resource, Publisher.class)
                   .map(Publisher::getId)
                   .map(URI::toString)
                   .filter(uri -> uri.contains(publisher))
                   .isPresent();
    }

    private <T extends PublishingHouse> Optional<T> getPublishingHouse(Resource resource, Class<T> type) {
        return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
                   .filter(Book.class::isInstance)
                   .map(Book.class::cast)
                   .map(Book::getPublisher)
                   .filter(type::isInstance)
                   .map(type::cast);
    }

    private void updateLicenseFiles(List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
        resources.forEach(resource -> resource.getFileEntries().stream()
                                          .filter(file -> file.getFile().getLicense().toString().equals(request.oldValue()))
                                          .forEach(file -> updateFileLicense(file, resource, request.newValue())));
    }

    private void updateFileLicense(FileEntry fileEntry, Resource resource, String license) {
        var updatedFile = fileEntry.getFile().copy().withLicense(URI.create(license)).build(fileEntry.getFile().getClass());
        fileEntry.update(updatedFile, UserInstance.fromPublication(resource.toPublication()), resourceService);
    }

    private URI buildPublicationChannelUri(String type, String year, String pid) {
        return buildUri(PUBLICATION_CHANNELS_V2_PATH_PARAM, type, pid, year);
    }

    private URI buildUri(String... pathSegments) {
        var builder = UriWrapper.fromHost(environment.readEnv(API_HOST));
        for (String segment : pathSegments) {
            builder = builder.addChild(segment);
        }
        return builder.getUri();
    }

    private void logUpdate(ManuallyUpdatePublicationsRequest request, List<Resource> resources) {
        logger.info("Updating {} from {} to {} for {} resources",
                    request.type(), request.oldValue(), request.newValue(), resources.size());
    }
}
