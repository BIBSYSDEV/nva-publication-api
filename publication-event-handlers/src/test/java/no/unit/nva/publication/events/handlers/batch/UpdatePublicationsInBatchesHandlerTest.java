package no.unit.nva.publication.events.handlers.batch;

import static java.util.UUID.randomUUID;
import static no.unit.nva.model.testing.PublicationGenerator.randomContributorWithId;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.events.handlers.batch.Comparator.CONTAINS;
import static no.unit.nva.publication.events.handlers.batch.Comparator.MATCHES;
import static no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest.DEFAULT_LIMIT;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Revision;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.RelationType;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Book.BookBuilder;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.SearchService;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogRecorder;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpdatePublicationsInBatchesHandlerTest extends ResourcesLocalTest {

  private static final Context CONTEXT = mock(Context.class);
  private static final String SERIAL_PUBLICATION = "serial-publication";
  private static final String PUBLISHER = "publisher";
  private static final String CRISTIN = "cristin";
  private static final String API_HOST = new Environment().readEnv("API_HOST");
  private static final int TOTAL_HITS = 4321;
  private static final int TWO_PAGES = 2;
  private static final int SINGLE_RESOURCE = 1;
  private static final int LIMIT_ABOVE_ALL_HITS = 1_000;
  private static final Integer NO_LIMIT = null;
  private static final Integer NO_PAGE_SIZE = null;
  private static final int SMALL_PAGE_SIZE = 4;
  private static final String SMALL_PAGE_SIZE_PARAM = "size=4";
  private static final String SIZE_PARAM = "size";
  private static final String SINGLE_HIT_PAGE_PARAM = "size=1";
  private static final String SORT_BY_IDENTIFIER_PARAM = "sort=identifier";
  private static final String NO_AGGREGATION_PARAM = "aggregation=none";
  private static final String EVENT_WITHOUT_SEARCH_PARAMS =
      """
      {"type":"%s","oldValue":"%s","newValue":"%s","comparator":"MATCHES","dryRun":false}
      """;
  private static final URI NEXT_PAGE_URI =
      URI.create("https://%s/search/resources?sort=identifier&search_after=1".formatted(API_HOST));
  private static final String PUBLISHER_ID_PATH =
      "/entityDescription/reference/publicationContext/publisher/id";
  private ManuallyUpdatePublicationsHandler handler;
  private ByteArrayOutputStream output;
  private ResourceService resourceService;
  private UriRetriever uriRetriever;

  @BeforeEach
  public void setUp() {
    super.init();
    output = new ByteArrayOutputStream();
    resourceService = getResourceService(client);
    uriRetriever = mock(UriRetriever.class);
    handler =
        new ManuallyUpdatePublicationsHandler(
            SearchService.create(uriRetriever, resourceService), resourceService);
  }

  @Test
  void
      shouldUpdatePublicationPublisherIdWhenUpdateTypeIsPublisherAndPublisherIdIsProvidedInRequest()
          throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var newPublisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER, publisherIdentifier, newPublisherIdentifier, MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedPublisher = getPublisher(updatedPublication);
          var id =
              URI.create(
                  publisherId.toString().replace(publisherIdentifier, newPublisherIdentifier));

          assertEquals(new Publisher(id), updatedPublisher);
        });
  }

  @Test
  void
      shouldUpdatePublicationJournalIdWhenUpdateTypeIsSerialPublicationAndJournalIdIsProvidedInRequest()
          throws IOException {
    var serialPublicationIdentifier = randomUUID().toString();
    var newSerialPublicationIdentifier = randomUUID().toString();
    var serialPublicationId =
        createChannelIdWithIdentifier(
            serialPublicationIdentifier, randomInteger().toString(), SERIAL_PUBLICATION);
    var publicationsToUpdate = createMultiplePublicationsWithSerialPublication(serialPublicationId);
    var event =
        createEvent(
            ManualUpdateType.SERIAL_PUBLICATION,
            serialPublicationIdentifier,
            newSerialPublicationIdentifier,
            MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedPublicationContext =
              updatedPublication.getEntityDescription().getReference().getPublicationContext();
          var updatedChannelId = getSerialPublicationId(updatedPublicationContext);
          var expectedJournal =
              serialPublicationId
                  .toString()
                  .replace(serialPublicationIdentifier, newSerialPublicationIdentifier);

          assertEquals(URI.create(expectedJournal), updatedChannelId);
        });
  }

  @Test
  void
      shouldNotUpdatePublicationJournalIdWhenUpdateTypeIsSerialPublicationAndPublicationAndRequestHaveDifferentSerialPublications()
          throws IOException {
    var journal =
        new Journal(
            createChannelIdWithIdentifier(
                randomUUID().toString(), randomInteger().toString(), SERIAL_PUBLICATION));
    var publicationsToUpdate = createMultiplePublicationsWithJournal(journal);
    var event =
        createEvent(
            ManualUpdateType.SERIAL_PUBLICATION,
            randomUUID().toString(),
            randomUUID().toString(),
            MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var publicationContext =
              (Journal)
                  updatedPublication.getEntityDescription().getReference().getPublicationContext();

          assertEquals(journal, publicationContext);
        });
  }

  @Test
  void shouldNotUpdatePublisherWhenPublisherUpdateAndPublicationAndRequestHaveDifferentPublishers()
      throws IOException {
    var publisherIdToKeep =
        createChannelIdWithIdentifier(
            randomUUID().toString(), randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate =
        createMultiplePublicationsWithPublisher(new Publisher(publisherIdToKeep));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER, randomUUID().toString(), randomUUID().toString(), MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var publisher = (Publisher) getPublisher(updatedPublication);

          assertEquals(publisherIdToKeep, publisher.getId());
        });
  }

  @Test
  void
      shouldUpdateFileLicenseUriWhenUpdateTypeIsLicenseAndLicenseUriIsProvidedInRequestAndMatchesFileLicense()
          throws IOException {
    var license = randomUri();
    var newLicense = randomUri();
    var publicationsToUpdate = createMultiplePublicationsWithLicense(license);
    var event =
        createEvent(ManualUpdateType.LICENSE, license.toString(), newLicense.toString(), MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);

          assertEquals(
              publication.getAssociatedArtifacts().size(),
              updatedPublication.getAssociatedArtifacts().size());

          var updatedFiles = getFiles(updatedPublication);

          updatedFiles.forEach(file -> assertEquals(newLicense, file.getLicense()));
        });
  }

  @Test
  void shouldSkipFilesWithoutLicenseWhenUpdateTypeIsLicense() throws IOException {
    var license = randomUri();
    var newLicense = randomUri();
    var publicationsToUpdate = createMultiplePublicationsWithLicensedAndUnlicensedFile(license);
    var event =
        createEvent(ManualUpdateType.LICENSE, license.toString(), newLicense.toString(), MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication ->
            assertThat(
                getFiles(getPublicationByIdentifier(publication)),
                containsInAnyOrder(hasLicense(newLicense), hasLicense(null))));
  }

  @Test
  void
      shouldNotUpdateFileLicenseUriWhenUpdateTypeIsLicenseAndLicenseUriIsNotEqualProvidedInRequestLicense()
          throws IOException {
    var license = randomUri();
    var publicationsToUpdate = createMultiplePublicationsWithLicense(license);
    var event = createEvent(ManualUpdateType.LICENSE, randomString(), randomString(), MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);

          assertEquals(
              publication.getAssociatedArtifacts().size(),
              updatedPublication.getAssociatedArtifacts().size());

          var updatedFiles = getFiles(updatedPublication);

          updatedFiles.forEach(file -> assertEquals(license, file.getLicense()));
        });
  }

  @Test
  void
      shouldUpdatePublicationWithUnconfirmedPublisherToConfirmedWhenUpdateTypeIsUnconfirmedPublisher()
          throws IOException {
    var publisherName = randomString();
    var publisherIdentifier = randomUUID().toString();
    var publicationsToUpdate =
        createMultiplePublicationsWithPublisher(new UnconfirmedPublisher(publisherName));
    var event =
        createEvent(
            ManualUpdateType.UNCONFIRMED_PUBLISHER, publisherName, publisherIdentifier, MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedPublisher = (Publisher) getPublisher(updatedPublication);
          var expectedPublisher =
              createChannelIdWithIdentifier(publisherIdentifier, getYear(publication), PUBLISHER);

          assertEquals(expectedPublisher, updatedPublisher.getId());
        });
  }

  @Test
  void
      shouldUpdatePublicationWithUnconfirmedPublisherToConfirmedWhenUpdateTypeIsUnconfirmedPublisherAndPublisherValueContainsProvidedPublisher()
          throws IOException {
    var publisherName = randomString();
    var publisherIdentifier = randomUUID().toString();
    var publicationsToUpdate =
        createMultiplePublicationsWithPublisher(
            new UnconfirmedPublisher(publisherName + randomString()));
    var event =
        createEvent(
            ManualUpdateType.UNCONFIRMED_PUBLISHER, publisherName, publisherIdentifier, CONTAINS);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedPublisher = (Publisher) getPublisher(updatedPublication);
          var expectedPublisher =
              createChannelIdWithIdentifier(publisherIdentifier, getYear(publication), PUBLISHER);

          assertEquals(expectedPublisher, updatedPublisher.getId());
        });
  }

  @Test
  void
      shouldNotUpdatePublicationWithUnconfirmedPublisherToConfirmedWhenProvidedPublisherDoesNotMatchExisting()
          throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var unconfirmedPublisher = new UnconfirmedPublisher(randomString());
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(unconfirmedPublisher);
    var event =
        createEvent(
            ManualUpdateType.UNCONFIRMED_PUBLISHER, randomString(), publisherIdentifier, MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedPublisher = getPublisher(updatedPublication);

          assertEquals(unconfirmedPublisher, updatedPublisher);
        });
  }

  @Test
  void
      shouldNotUpdatePublicationWithUnconfirmedPublisherToConfirmedWhenProvidedPublisherDoesNotContainsExisting()
          throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var unconfirmedPublisher = new UnconfirmedPublisher(randomString());
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(unconfirmedPublisher);
    var event =
        createEvent(
            ManualUpdateType.UNCONFIRMED_PUBLISHER, randomString(), publisherIdentifier, CONTAINS);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedPublisher = getPublisher(updatedPublication);

          assertEquals(unconfirmedPublisher, updatedPublisher);
        });
  }

  @Test
  void shouldUpdatePublicationWithUnconfirmedSeriesToConfirmedWhenUpdateTypeIsUnconfirmedSeries()
      throws IOException, InvalidIssnException {
    var seriesTitle = randomString();
    var seriesIdentifier = randomUUID().toString();
    var publicationsToUpdate =
        createMultiplePublicationsWithSeries(new UnconfirmedSeries(seriesTitle, null, null));
    var event =
        createEvent(ManualUpdateType.UNCONFIRMED_SERIES, seriesTitle, seriesIdentifier, MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedBook =
              (Book)
                  updatedPublication.getEntityDescription().getReference().getPublicationContext();
          var updatedSeries = (Series) updatedBook.getSeries();
          var expectedSeriesId =
              createChannelIdWithIdentifier(
                  seriesIdentifier, getYear(publication), SERIAL_PUBLICATION);

          assertEquals(expectedSeriesId, updatedSeries.getId());
        });
  }

  @Test
  void
      shouldUpdatePublicationWithUnconfirmedSeriesToConfirmedWhenUpdateTypeIsUnconfirmedSeriesAndValueContainsProvidedSeries()
          throws IOException, InvalidIssnException {
    var seriesTitle = randomString();
    var seriesIdentifier = randomUUID().toString();
    var publicationsToUpdate =
        createMultiplePublicationsWithSeries(
            new UnconfirmedSeries(seriesTitle + randomString(), null, null));
    var event =
        createEvent(ManualUpdateType.UNCONFIRMED_SERIES, seriesTitle, seriesIdentifier, CONTAINS);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedBook =
              (Book)
                  updatedPublication.getEntityDescription().getReference().getPublicationContext();
          var updatedSeries = (Series) updatedBook.getSeries();
          var expectedSeriesId =
              createChannelIdWithIdentifier(
                  seriesIdentifier, getYear(publication), SERIAL_PUBLICATION);

          assertEquals(expectedSeriesId, updatedSeries.getId());
        });
  }

  @Test
  void
      shouldNotUpdatePublicationWithUnconfirmedSeriesToConfirmedWhenProvidedSeriesDoesNotMatchExisting()
          throws IOException, InvalidIssnException {
    var unconfirmedSeries = new UnconfirmedSeries(randomString(), null, null);
    var publicationsToUpdate = createMultiplePublicationsWithSeries(unconfirmedSeries);
    var event =
        createEvent(ManualUpdateType.UNCONFIRMED_SERIES, randomString(), randomString(), MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedBook =
              (Book)
                  updatedPublication.getEntityDescription().getReference().getPublicationContext();

          assertEquals(unconfirmedSeries, updatedBook.getSeries());
        });
  }

  @Test
  void
      shouldNotUpdatePublicationWithUnconfirmedSeriesToConfirmedWhenProvidedSeriesDoesNotContainsExisting()
          throws IOException, InvalidIssnException {
    var unconfirmedSeries = new UnconfirmedSeries(randomString(), null, null);
    var publicationsToUpdate = createMultiplePublicationsWithSeries(unconfirmedSeries);
    var event =
        createEvent(ManualUpdateType.UNCONFIRMED_SERIES, randomString(), randomString(), CONTAINS);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedBook =
              (Book)
                  updatedPublication.getEntityDescription().getReference().getPublicationContext();

          assertEquals(unconfirmedSeries, updatedBook.getSeries());
        });
  }

  @Test
  void shouldUpdatePublicationWithUnconfirmedJournalToConfirmedWhenUpdateTypeIsUnconfirmedJournal()
      throws IOException, InvalidIssnException {
    var journalTitle = randomString();
    var journalIdentifier = randomUUID().toString();
    var publicationsToUpdate =
        createMultiplePublicationsWithJournal(new UnconfirmedJournal(journalTitle, null, null));
    var event =
        createEvent(ManualUpdateType.UNCONFIRMED_JOURNAL, journalTitle, journalIdentifier, MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedJournal =
              (Journal)
                  updatedPublication.getEntityDescription().getReference().getPublicationContext();
          var expectedJournalId =
              createChannelIdWithIdentifier(
                  journalIdentifier, getYear(publication), SERIAL_PUBLICATION);

          assertEquals(expectedJournalId, updatedJournal.getId());
        });
  }

  @Test
  void
      shouldNotUpdatePublicationWithUnconfirmedJournalToConfirmedWhenProvidedJournalDoesNotMatchExisting()
          throws IOException, InvalidIssnException {
    var unconfirmedJournal = new UnconfirmedJournal(randomString(), null, null);
    var publicationsToUpdate = createMultiplePublicationsWithJournal(unconfirmedJournal);
    var event =
        createEvent(ManualUpdateType.UNCONFIRMED_JOURNAL, randomString(), randomString(), MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedJournal =
              (UnconfirmedJournal)
                  updatedPublication.getEntityDescription().getReference().getPublicationContext();

          assertEquals(unconfirmedJournal, updatedJournal);
        });
  }

  @Test
  void shouldUpdateContributorIdForPublicationsWhereContributorWithProvidedIdIsPresent()
      throws IOException {
    var oldContributorIdentifier = randomInteger().toString();
    var contributorId = createContributorIdentifier(oldContributorIdentifier);
    var publicationsToUpdate =
        createMultiplePublicationsWithContributor(randomContributorWithId(contributorId));
    var newContributorIdentifier = randomInteger().toString();
    var event =
        createEvent(
            ManualUpdateType.CONTRIBUTOR_IDENTIFIER,
            oldContributorIdentifier,
            newContributorIdentifier,
            MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);
          var updatedContributorIdentifier =
              findContributorByIdentifier(updatedPublication, newContributorIdentifier);
          var contributorsToKeepUnchanged =
              publication.getContributors().stream()
                  .filter(contributor -> !hasIdentifier(contributor, oldContributorIdentifier))
                  .toList();
          assertEquals(
              newContributorIdentifier, getContributorIdentifier(updatedContributorIdentifier));
          assertEquals(
              publication.getContributors().size(), updatedPublication.getContributors().size());
          assertTrue(updatedPublication.getContributors().containsAll(contributorsToKeepUnchanged));
        });
  }

  @Test
  void shouldNotUpdateContributorIdForPublicationsWhereContributorWithProvidedIdIsNotPresent()
      throws IOException {
    var publicationsToUpdate =
        createMultiplePublicationsWithContributor(randomContributorWithId(randomUri()));
    var event =
        createEvent(
            ManualUpdateType.CONTRIBUTOR_IDENTIFIER, randomString(), randomString(), MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication -> {
          var updatedPublication = getPublicationByIdentifier(publication);

          assertTrue(
              updatedPublication.getContributors().containsAll(publication.getContributors()));
        });
  }

  @Test
  void shouldReportSearchHitsAndMatchedResourcesInReport() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER,
            publisherIdentifier,
            randomUUID().toString(),
            MATCHES,
            true);

    mockSearchApiResponseWithPublications(publicationsToUpdate, TOTAL_HITS);

    handler.handleRequest(event, output, CONTEXT);

    var report = readReport();
    assertEquals(TOTAL_HITS, report.totalHits());
    assertEquals(publicationsToUpdate.size(), report.hitsReturned());
    assertEquals(publicationsToUpdate.size(), report.resourcesFetched());
    assertEquals(publicationsToUpdate.size(), report.resourcesMatched());
  }

  @Test
  void shouldNotPersistChangesWhenDryRunIsRequested() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER,
            publisherIdentifier,
            randomUUID().toString(),
            MATCHES,
            true);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication ->
            assertEquals(
                new Publisher(publisherId), getPublisher(getPublicationByIdentifier(publication))));
  }

  @Test
  void shouldReportProposedPublisherChangeWhenDryRunIsRequested() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var newPublisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER, publisherIdentifier, newPublisherIdentifier, MATCHES, true);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    var report = readReport();
    assertTrue(report.dryRun());
    assertEquals(publicationsToUpdate.size(), report.changes().size());
    report
        .changes()
        .forEach(
            change ->
                assertThat(
                    change.fieldChanges(),
                    hasItem(
                        new FieldChange(
                            PUBLISHER_ID_PATH,
                            publisherId.toString(),
                            publisherId
                                .toString()
                                .replace(publisherIdentifier, newPublisherIdentifier)))));
  }

  @Test
  void shouldNotPersistLicenseChangesWhenDryRunIsRequested() throws IOException {
    var license = randomUri();
    var newLicense = randomUri();
    var publicationsToUpdate = createMultiplePublicationsWithLicense(license);
    var event =
        createEvent(
            ManualUpdateType.LICENSE, license.toString(), newLicense.toString(), MATCHES, true);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    publicationsToUpdate.forEach(
        publication ->
            getFiles(getPublicationByIdentifier(publication))
                .forEach(file -> assertEquals(license, file.getLicense())));
    assertEquals(publicationsToUpdate.size(), readReport().resourcesChanged());
  }

  @Test
  void shouldReportNoChangesWhenLicenseIsAlreadyTheRequestedOne() throws IOException {
    var license = randomUri();
    var publicationsToUpdate = createMultiplePublicationsWithLicense(license);
    var event =
        createEvent(
            ManualUpdateType.LICENSE, license.toString(), license.toString(), MATCHES, true);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    var report = readReport();
    assertEquals(publicationsToUpdate.size(), report.resourcesMatched());
    assertEquals(0, report.resourcesChanged());
  }

  @Test
  void shouldFollowSearchAfterCursorUntilPageWithoutCursor() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var firstPage = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var lastPage = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER,
            publisherIdentifier,
            randomUUID().toString(),
            MATCHES,
            false,
            LIMIT_ABOVE_ALL_HITS);

    mockSearchApiPages(firstPage, lastPage);

    handler.handleRequest(event, output, CONTEXT);

    var report = readReport();
    assertEquals(TWO_PAGES, report.pagesFetched());
    assertEquals(firstPage.size() + lastPage.size(), report.resourcesChanged());
  }

  @Test
  void shouldRequestNextPageUsingCursorProvidedBySearchApi() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var firstPage = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var lastPage = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER,
            publisherIdentifier,
            randomUUID().toString(),
            MATCHES,
            false,
            LIMIT_ABOVE_ALL_HITS);

    mockSearchApiPages(firstPage, lastPage);

    handler.handleRequest(event, output, CONTEXT);

    assertEquals(NEXT_PAGE_URI, capturedSearchUris().getLast());
  }

  @Test
  void shouldSortFirstPageByIdentifierWithoutAggregations() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER, publisherIdentifier, randomUUID().toString(), MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    var query = capturedSearchUris().getFirst().getQuery();
    assertThat(query, containsString(SORT_BY_IDENTIFIER_PARAM));
    assertThat(query, containsString(NO_AGGREGATION_PARAM));
  }

  @Test
  void shouldStopChangingResourcesWhenLimitIsReached() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var newPublisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER,
            publisherIdentifier,
            newPublisherIdentifier,
            MATCHES,
            false,
            SINGLE_RESOURCE);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    var report = readReport();
    assertEquals(SINGLE_RESOURCE, report.resourcesChanged());
    assertTrue(report.limitReached());
    var newPublisherId =
        URI.create(publisherId.toString().replace(publisherIdentifier, newPublisherIdentifier));
    assertEquals(
        SINGLE_RESOURCE, countPublicationsWithPublisher(publicationsToUpdate, newPublisherId));
  }

  @Test
  void shouldNotRequestLargerPagesThanTheRequestedLimit() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER,
            publisherIdentifier,
            randomUUID().toString(),
            MATCHES,
            false,
            SINGLE_RESOURCE);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    assertThat(capturedSearchUris().getFirst().getQuery(), containsString(SINGLE_HIT_PAGE_PARAM));
  }

  @Test
  void shouldReportDefaultLimitWhenNoLimitIsRequested() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER, publisherIdentifier, randomUUID().toString(), MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    assertEquals(DEFAULT_LIMIT, readReport().limit());
  }

  @Test
  void shouldReportThatLimitWasNotReachedWhenRunFitsWithinIt() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER,
            publisherIdentifier,
            randomUUID().toString(),
            MATCHES,
            false,
            LIMIT_ABOVE_ALL_HITS);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    var report = readReport();
    assertFalse(report.limitReached());
    assertEquals(publicationsToUpdate.size(), report.resourcesChanged());
  }

  @Test
  void shouldTreatSizeSearchParamAsLimit() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var newPublisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEventWithSize(
            ManualUpdateType.PUBLISHER,
            publisherIdentifier,
            newPublisherIdentifier,
            SINGLE_RESOURCE);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    var report = readReport();
    assertEquals(SINGLE_RESOURCE, report.limit());
    assertEquals(SINGLE_RESOURCE, report.resourcesChanged());
    assertTrue(report.limitReached());
  }

  @Test
  void shouldRequestPageSizeFromRequest() throws IOException {
    var publisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER,
            publisherIdentifier,
            randomUUID().toString(),
            MATCHES,
            false,
            NO_LIMIT,
            SMALL_PAGE_SIZE);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    assertThat(capturedSearchUris().getFirst().getQuery(), containsString(SMALL_PAGE_SIZE_PARAM));
    assertEquals(SMALL_PAGE_SIZE, readReport().pageSize());
  }

  @Test
  void shouldRejectRequestWithoutSearchParams() {
    var event =
        createEventWithoutSearchParams(
            ManualUpdateType.PUBLISHER, randomUUID().toString(), randomUUID().toString());

    assertThrows(
        JsonProcessingException.class, () -> handler.handleRequest(event, output, CONTEXT));
    verifyNoInteractions(uriRetriever);
  }

  @Test
  void shouldLogChangesPerPageAndKeepSummaryFreeOfChangeDetails() throws IOException {
    var logRecorder = LogRecorder.forClass(UpdateLog.class);
    var summaryRecorder = LogRecorder.forClass(ManuallyUpdatePublicationsHandler.class);
    var publisherIdentifier = randomUUID().toString();
    var publisherId =
        createChannelIdWithIdentifier(publisherIdentifier, randomInteger().toString(), PUBLISHER);
    var publicationsToUpdate = createMultiplePublicationsWithPublisher(new Publisher(publisherId));
    var event =
        createEvent(
            ManualUpdateType.PUBLISHER, publisherIdentifier, randomUUID().toString(), MATCHES);

    mockSearchApiResponseWithPublications(publicationsToUpdate);

    handler.handleRequest(event, output, CONTEXT);

    assertThat(logRecorder.asString(), containsString(PUBLISHER_ID_PATH));
    assertThat(summaryRecorder.asString(), not(containsString(PUBLISHER_ID_PATH)));
  }

  private long countPublicationsWithPublisher(
      Collection<Publication> publications, URI publisherId) {
    var expectedPublisher = new Publisher(publisherId);
    return publications.stream()
        .map(this::getPublicationByIdentifier)
        .map(UpdatePublicationsInBatchesHandlerTest::getPublisher)
        .filter(expectedPublisher::equals)
        .count();
  }

  private ManuallyUpdatePublicationsReport readReport() throws IOException {
    return JsonUtils.dtoObjectMapper.readValue(
        output.toByteArray(), ManuallyUpdatePublicationsReport.class);
  }

  private static URI getSerialPublicationId(PublicationContext updatedPublicationContext) {
    return updatedPublicationContext instanceof Book book
        ? ((Series) book.getSeries()).getId()
        : ((Journal) updatedPublicationContext).getId();
  }

  private static String getContributorIdentifier(Contributor contributor) {
    return UriWrapper.fromUri(contributor.identity().getId()).getLastPathElement();
  }

  private static Contributor findContributorByIdentifier(
      Publication publication, String contributorIdentifier) {
    return publication.getContributors().stream()
        .filter(contributor -> hasIdentifier(contributor, contributorIdentifier))
        .findFirst()
        .orElseThrow();
  }

  private static boolean hasIdentifier(Contributor contributor, String oldContributorIdentifier) {
    return Optional.ofNullable(contributor)
        .map(Contributor::identity)
        .map(Identity::getId)
        .map(URI::toString)
        .map(oldContributorIdentifier::contains)
        .isPresent();
  }

  private static URI createContributorIdentifier(String contributorIdentifier) {
    return UriWrapper.fromUri(randomUri())
        .addChild(CRISTIN)
        .addChild("person")
        .addChild(contributorIdentifier)
        .getUri();
  }

  private static String getYear(Publication publication) {
    return publication.getEntityDescription().getPublicationDate().getYear();
  }

  private static List<File> getFiles(Publication updatedPublication) {
    return updatedPublication.getAssociatedArtifacts().stream()
        .filter(File.class::isInstance)
        .map(File.class::cast)
        .toList();
  }

  private static InputStream createEvent(
      ManualUpdateType type, String oldValue, String newValue, Comparator comparator) {
    return createEvent(type, oldValue, newValue, comparator, false);
  }

  private static InputStream createEvent(
      ManualUpdateType type,
      String oldValue,
      String newValue,
      Comparator comparator,
      boolean dryRun) {
    return createEvent(type, oldValue, newValue, comparator, dryRun, NO_LIMIT);
  }

  private static InputStream createEvent(
      ManualUpdateType type,
      String oldValue,
      String newValue,
      Comparator comparator,
      boolean dryRun,
      Integer limit) {
    return createEvent(type, oldValue, newValue, comparator, dryRun, limit, NO_PAGE_SIZE);
  }

  private static InputStream createEvent(
      ManualUpdateType type,
      String oldValue,
      String newValue,
      Comparator comparator,
      boolean dryRun,
      Integer limit,
      Integer pageSize) {
    return IoUtils.stringToStream(
        new ManuallyUpdatePublicationsRequest(
                type,
                oldValue,
                newValue,
                Map.of("publisher", oldValue),
                comparator,
                dryRun,
                limit,
                pageSize)
            .toJsonString());
  }

  private static InputStream createEventWithSize(
      ManualUpdateType type, String oldValue, String newValue, int size) {
    var searchParams = Map.of(PUBLISHER, oldValue, SIZE_PARAM, String.valueOf(size));
    return IoUtils.stringToStream(
        new ManuallyUpdatePublicationsRequest(
                type, oldValue, newValue, searchParams, MATCHES, false, NO_LIMIT, NO_PAGE_SIZE)
            .toJsonString());
  }

  private static InputStream createEventWithoutSearchParams(
      ManualUpdateType type, String oldValue, String newValue) {
    return IoUtils.stringToStream(EVENT_WITHOUT_SEARCH_PARAMS.formatted(type, oldValue, newValue));
  }

  private static URI createChannelIdWithIdentifier(
      String channelIdentifier, String year, String type) {
    return UriWrapper.fromHost(API_HOST)
        .addChild("publication-channels-v2")
        .addChild(type)
        .addChild(channelIdentifier)
        .addChild(year)
        .getUri();
  }

  private static PublishingHouse getPublisher(Publication updatedPublication) {
    var book =
        (Book) updatedPublication.getEntityDescription().getReference().getPublicationContext();
    return book.getPublisher();
  }

  private static URI createPublicationId(String identifier) {
    return UriWrapper.fromUri(randomUri()).addChild(identifier).getUri();
  }

  private static List<ResourceWithId> convertToResourcesWithId(List<Publication> publicationList) {
    return publicationList.stream()
        .map(Publication::getIdentifier)
        .map(SortableIdentifier::toString)
        .map(UpdatePublicationsInBatchesHandlerTest::createPublicationId)
        .map(ResourceWithId::new)
        .toList();
  }

  private Publication getPublicationByIdentifier(Publication publication) {
    return attempt(() -> resourceService.getPublicationByIdentifier(publication.getIdentifier()))
        .orElseThrow();
  }

  private List<Publication> createMultiplePublicationsWithLicense(URI license) {
    return IntStream.range(0, 10).boxed().map(i -> createPublicationWithLicense(license)).toList();
  }

  private Publication createPublicationWithLicense(URI license) {
    var publication = randomPublication();
    publication.setAssociatedArtifacts(new AssociatedArtifactList(randomFileWithLicense(license)));
    return attempt(
            () ->
                resourceService.createPublication(
                    UserInstance.fromPublication(publication), publication))
        .orElseThrow();
  }

  private List<Publication> createMultiplePublicationsWithLicensedAndUnlicensedFile(URI license) {
    return IntStream.range(0, 10)
        .boxed()
        .map(index -> createPublicationWithLicensedAndUnlicensedFile(license))
        .toList();
  }

  private Publication createPublicationWithLicensedAndUnlicensedFile(URI license) {
    var publication = randomPublication();
    publication.setAssociatedArtifacts(
        new AssociatedArtifactList(
            List.of(
                File.builder().withLicense(license).withIdentifier(randomUUID()).buildOpenFile(),
                File.builder().withIdentifier(randomUUID()).buildOpenFile())));
    return attempt(
            () ->
                resourceService.createPublication(
                    UserInstance.fromPublication(publication), publication))
        .orElseThrow();
  }

  private Matcher<File> hasLicense(URI license) {
    return new FeatureMatcher<>(equalTo(license), "file with license", "license") {
      @Override
      protected URI featureValueOf(File file) {
        return file.getLicense();
      }
    };
  }

  private List<AssociatedArtifact> randomFileWithLicense(URI license) {
    return List.of(
        File.builder().withLicense(license).withIdentifier(randomUUID()).buildOpenFile(),
        File.builder().withLicense(license).withIdentifier(randomUUID()).buildInternalFile(),
        File.builder().withLicense(license).withIdentifier(randomUUID()).buildPendingInternalFile(),
        new AssociatedLink(randomUri(), randomString(), randomString(), RelationType.SAME_AS));
  }

  private List<Publication> createMultiplePublicationsWithPublisher(
      PublishingHouse publishingHouse) {
    return IntStream.range(0, 10)
        .boxed()
        .map(i -> createPublicationWithPublisher(publishingHouse))
        .toList();
  }

  private List<Publication> createMultiplePublicationsWithSeries(BookSeries bookSeries) {
    return IntStream.range(0, 10)
        .boxed()
        .map(i -> createPublicationWithSeries(bookSeries))
        .toList();
  }

  private List<Publication> createMultiplePublicationsWithJournal(Periodical periodical) {
    return IntStream.range(0, 10)
        .boxed()
        .map(i -> attempt(() -> createPublicationWithJournal(periodical)).orElseThrow())
        .toList();
  }

  private List<Publication> createMultiplePublicationsWithContributor(Contributor contributor) {
    return IntStream.range(0, 10)
        .boxed()
        .map(i -> attempt(() -> createPublicationWithContributor(contributor)).orElseThrow())
        .toList();
  }

  private List<Publication> createMultiplePublicationsWithSerialPublication(
      URI serialPublicationId) {
    return IntStream.range(0, 10)
        .boxed()
        .map(
            i ->
                attempt(() -> createPublicationWithSerialPublication(serialPublicationId))
                    .orElseThrow())
        .toList();
  }

  private void mockSearchApiResponseWithPublications(List<Publication> publicationList) {
    mockSearchApiResponseWithPublications(publicationList, publicationList.size());
  }

  private void mockSearchApiResponseWithPublications(
      List<Publication> publicationList, int totalHits) {
    var resourcesWithId = convertToResourcesWithId(publicationList);
    var responseBody = new SearchResourceApiResponse(totalHits, resourcesWithId);
    var response = FakeHttpResponse.create(responseBody.toJsonString(), 200);
    when(uriRetriever.fetchResponse(any(), any())).thenReturn(Optional.of(response));
  }

  private void mockSearchApiPages(List<Publication> firstPage, List<Publication> lastPage) {
    var totalHits = firstPage.size() + lastPage.size();
    var firstPageBody =
        new SearchResourceApiResponse(
            totalHits, convertToResourcesWithId(firstPage), NEXT_PAGE_URI);
    var lastPageBody = new SearchResourceApiResponse(totalHits, convertToResourcesWithId(lastPage));
    when(uriRetriever.fetchResponse(any(), any()))
        .thenReturn(Optional.of(FakeHttpResponse.create(firstPageBody.toJsonString(), 200)))
        .thenReturn(Optional.of(FakeHttpResponse.create(lastPageBody.toJsonString(), 200)));
  }

  private List<URI> capturedSearchUris() {
    var searchUri = ArgumentCaptor.forClass(URI.class);
    verify(uriRetriever, atLeastOnce()).fetchResponse(searchUri.capture(), any());
    return searchUri.getAllValues();
  }

  private Publication createPublicationWithPublisher(PublishingHouse publishingHouse) {
    var publication = randomPublication();
    publication
        .getEntityDescription()
        .getReference()
        .setPublicationContext(new BookBuilder().withPublisher(publishingHouse).build());
    return attempt(
            () ->
                resourceService.createPublication(
                    UserInstance.fromPublication(publication), publication))
        .orElseThrow();
  }

  private Publication createPublicationWithSerialPublication(URI serialPublicationId) {
    if (randomBoolean()) {
      return createPublicationWithJournal(new Journal(serialPublicationId));
    } else {
      return createPublicationWithSeries(new Series(serialPublicationId));
    }
  }

  private Publication createPublicationWithJournal(Periodical periodical) {
    var publication = randomPublication(JournalArticle.class);
    publication.getEntityDescription().getReference().setPublicationContext(periodical);
    return attempt(
            () ->
                resourceService.createPublication(
                    UserInstance.fromPublication(publication), publication))
        .orElseThrow();
  }

  private Publication createPublicationWithContributor(Contributor contributor) {
    var publication = randomPublication();
    publication
        .getEntityDescription()
        .setContributors(
            List.of(
                new Contributor.Builder().build(),
                contributor,
                randomContributorWithId(randomUri()),
                new Contributor.Builder().withIdentity(new Identity.Builder().build()).build()));
    return attempt(
            () ->
                resourceService.createPublication(
                    UserInstance.fromPublication(publication), publication))
        .orElseThrow();
  }

  private Publication createPublicationWithSeries(BookSeries bookSeries) {
    var publication = randomPublication(AcademicMonograph.class);
    var seriesTitle =
        bookSeries instanceof UnconfirmedSeries unconfirmedSeries
            ? unconfirmedSeries.getTitle()
            : randomString();
    var publicationContext =
        attempt(
                () ->
                    new Book(
                        bookSeries,
                        seriesTitle,
                        randomString(),
                        null,
                        List.of(),
                        Revision.UNREVISED))
            .orElseThrow();
    publication.getEntityDescription().getReference().setPublicationContext(publicationContext);
    return attempt(
            () ->
                resourceService.createPublication(
                    UserInstance.fromPublication(publication), publication))
        .orElseThrow();
  }
}
