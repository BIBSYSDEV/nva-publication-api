package no.unit.nva.publication.events.handlers.batch;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
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

@SuppressWarnings("PMD.GodClass")
public final class ManuallyUpdatePublicationUtil {

    private static final Logger logger = LoggerFactory.getLogger(ManuallyUpdatePublicationUtil.class);
    private static final String API_HOST = "API_HOST";
    private static final String PUBLICATION_CHANNELS_V2_PATH_PARAM = "publication-channels-v2";
    private static final String PUBLISHER = "publisher";
    private static final String SERIAL_PUBLICATION = "serial-publication";
    private static final String CRISTIN = "cristin";
    private static final String PERSON = "person";
    public static final String ORGANIZATION = "organization";

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
            case SERIAL_PUBLICATION ->
                updateResources(resources, request, this::hasSerialPublication, this::updateSeriesOrJournal);
            case LICENSE -> updateLicenseFiles(resources, request);
            case UNCONFIRMED_PUBLISHER ->
                updateResources(resources, request, unconfirmedPublisherFilter(request), updateUnconfirmedPublisher());
            case UNCONFIRMED_SERIES ->
                updateResources(resources, request, unconfirmedSeriesFilter(request), updateUnconfirmedSeries());
            case UNCONFIRMED_JOURNAL -> updateResources(resources, request, unconfirmedJournalFilter(request),
                                                        this::updateUnconfirmedJournalToConfirmed);
            case CONTRIBUTOR_IDENTIFIER ->
                updateResources(resources, request, this::hasContributor, this::updateContributorIdentifier);
            case CONTRIBUTOR_AFFILIATION -> updateResources(resources, request, this::hasContributorWithAffiliation,
                                                              this::updateContributorAffiliation);
        }
    }

    private Resource updateContributorAffiliation(Resource resource,
                                                  ManuallyUpdatePublicationsRequest request) {
        var updatedContributors = resource.getEntityDescription().getContributors().stream()
                                      .map(contributor -> updateContributor(contributor, request))
                                      .toList();

        resource.getEntityDescription().setContributors(updatedContributors);
        return resource;
    }

    private Contributor updateContributor(Contributor contributor,
                                          ManuallyUpdatePublicationsRequest request) {
        var updatedAffiliations = contributor.getAffiliations().stream()
                                      .map(corporation -> updateAffiliation(corporation, request))
                                      .toList();

        return contributor.copy().withAffiliations(updatedAffiliations).build();
    }

    private Corporation updateAffiliation(Corporation corporation,
                                          ManuallyUpdatePublicationsRequest request) {
        if (corporation instanceof Organization organization
            && hasAffiliationIdentifier(organization, request.oldValue())) {
            return Organization.fromUri(buildUri(CRISTIN, ORGANIZATION, request.newValue()));
        }
        return corporation;
    }

    private boolean hasContributorWithAffiliation(Resource resource, String affiliationIdentifier) {
        return resource.getEntityDescription().getContributors().stream()
                   .anyMatch(contributor -> hasAffiliation(contributor, affiliationIdentifier));
    }

    private boolean hasAffiliation(Contributor contributor, String affiliationIdentifier) {
        return contributor.getAffiliations().stream()
                   .filter(Organization.class::isInstance)
                   .map(Organization.class::cast)
                   .anyMatch(organization -> hasAffiliationIdentifier(organization, affiliationIdentifier));
    }

    private boolean hasAffiliationIdentifier(Organization organization, String affiliationIdentifier) {
        return Optional.ofNullable(organization)
                   .map(Organization::getId)
                   .map(UriWrapper::fromUri)
                   .map(UriWrapper::getLastPathElement)
                   .filter(affiliationIdentifier::equals)
                   .isPresent();
    }

    private static boolean hasLicense(String license, FileEntry file) {
        return file.getFile().getLicense().toString().equals(license);
    }

    private BiPredicate<Resource, String> unconfirmedJournalFilter(ManuallyUpdatePublicationsRequest request) {
        return (resource, val) -> unconfirmedJournalFilter(resource, val, request.comparator());
    }

    private BiFunction<Resource, ManuallyUpdatePublicationsRequest, Resource> updateUnconfirmedSeries() {
        return (resource, req) -> updateUnconfirmedToConfirmed(resource, req, SERIAL_PUBLICATION,
                                                               this::createBookWithSeries);
    }

    private BiPredicate<Resource, String> unconfirmedSeriesFilter(ManuallyUpdatePublicationsRequest request) {
        return (resource, val) -> unconfirmedSeriesFilter(resource, val, request.comparator());
    }

    private BiFunction<Resource, ManuallyUpdatePublicationsRequest, Resource> updateUnconfirmedPublisher() {
        return (resource, req) -> updateUnconfirmedToConfirmed(resource, req, PUBLISHER, this::createBookWithPublisher);
    }

    private BiPredicate<Resource, String> unconfirmedPublisherFilter(ManuallyUpdatePublicationsRequest request) {
        return (resource, publisherName) -> unconfirmedPublisherFilter(resource, publisherName, request.comparator());
    }

    private void updateResources(List<Resource> resources, ManuallyUpdatePublicationsRequest request,
                                 BiPredicate<Resource, String> filter,
                                 BiFunction<Resource, ManuallyUpdatePublicationsRequest, Resource> updater) {
        var publicationsToUpdate = resources.stream()
                                       .filter(resource -> filter.test(resource, request.oldValue()))
                                       .map(resource -> updater.apply(resource, request))
                                       .toList();

        logUpdate(request, publicationsToUpdate);
        publicationsToUpdate.forEach(resource -> resourceService.updateResource(resource, UserInstance.fromPublication(
            resource.toPublication())));
    }

    private Resource updateContributorIdentifier(Resource resource, ManuallyUpdatePublicationsRequest request) {
        var contributors = new ArrayList<>(resource.getEntityDescription().getContributors());
        var contributorToUpdate = contributors.stream()
                                      .filter(contributor -> hasIdentifier(contributor, request.oldValue()))
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

    private boolean unconfirmedPublisherFilter(Resource resource, String publisherName, Comparator comparator) {
        return getPublishingHouse(resource, UnconfirmedPublisher.class).map(UnconfirmedPublisher::getName)
                   .filter(value -> matches(value, publisherName, comparator))
                   .isPresent();
    }

    private boolean matches(String actual, String expected, Comparator comparator) {
        return switch (comparator) {
            case CONTAINS -> actual.contains(expected);
            case MATCHES -> actual.equals(expected);
        };
    }

    private boolean unconfirmedSeriesFilter(Resource resource, String seriesTitle, Comparator comparator) {
        return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
                   .filter(Book.class::isInstance)
                   .map(Book.class::cast)
                   .map(Book::getSeries)
                   .filter(UnconfirmedSeries.class::isInstance)
                   .map(UnconfirmedSeries.class::cast)
                   .map(UnconfirmedSeries::getTitle)
                   .filter(value -> matches(value, seriesTitle, comparator))
                   .isPresent();
    }

    private boolean unconfirmedJournalFilter(Resource resource, String journalTitle, Comparator comparator) {
        return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
                   .filter(UnconfirmedJournal.class::isInstance)
                   .map(UnconfirmedJournal.class::cast)
                   .map(UnconfirmedJournal::getTitle)
                   .filter(value -> matches(value, journalTitle, comparator))
                   .isPresent();
    }

    private boolean hasContributor(Resource resource, String contributorId) {
        return Optional.ofNullable(resource.getEntityDescription())
                   .map(EntityDescription::getContributors)
                   .stream()
                   .flatMap(List::stream)
                   .anyMatch(contributor -> hasIdentifier(contributor, contributorId));
    }

    private boolean hasIdentifier(Contributor contributor, String contributorIdentifier) {
        return Optional.ofNullable(contributor)
                   .map(Contributor::getIdentity)
                   .map(Identity::getId)
                   .map(UriWrapper::fromUri)
                   .map(UriWrapper::getLastPathElement)
                   .filter(contributorIdentifier::equals)
                   .isPresent();
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
        var publisherUri = getPublishingHouse(resource, Publisher.class).map(Publisher::getId)
                               .map(URI::toString)
                               .map(uri -> uri.replace(request.oldValue(), request.newValue()))
                               .map(URI::create)
                               .orElseThrow();

        resource.getEntityDescription()
            .getReference()
            .setPublicationContext(book.copy().withPublisher(new Publisher(publisherUri)).build());
        return resource;
    }

    private boolean hasPublisher(Resource resource, String publisher) {
        return getPublishingHouse(resource, Publisher.class).map(Publisher::getId)
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
        resources.forEach(resource -> resource.getFileEntries()
                                          .stream()
                                          .filter(file -> hasLicense(request.oldValue(), file))
                                          .forEach(file -> updateFileLicense(file, resource, request.newValue())));
    }

    private void updateFileLicense(FileEntry fileEntry, Resource resource, String license) {
        var updatedFile = fileEntry.getFile()
                              .copy()
                              .withLicense(URI.create(license))
                              .build(fileEntry.getFile().getClass());
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
        logger.info("Updating {} from {} to {} for {} resources", request.type(), request.oldValue(),
                    request.newValue(), resources.size());
    }
}
