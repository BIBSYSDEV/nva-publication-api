package no.sikt.nva.brage.migration.lambda;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.brage.migration.NvaType.ANTHOLOGY;
import static no.sikt.nva.brage.migration.NvaType.CRISTIN_RECORD;
import static no.sikt.nva.brage.migration.NvaType.EDITORIAL;
import static no.sikt.nva.brage.migration.NvaType.EXHIBITION_CATALOGUE;
import static no.sikt.nva.brage.migration.NvaType.FILM;
import static no.sikt.nva.brage.migration.NvaType.LITERARY_ARTS;
import static no.sikt.nva.brage.migration.NvaType.PERFORMING_ARTS;
import static no.sikt.nva.brage.migration.NvaType.POPULAR_SCIENCE_ARTICLE;
import static no.sikt.nva.brage.migration.NvaType.POPULAR_SCIENCE_MONOGRAPH;
import static no.sikt.nva.brage.migration.NvaType.PROFESSIONAL_ARTICLE;
import static no.sikt.nva.brage.migration.NvaType.READER_OPINION;
import static no.sikt.nva.brage.migration.NvaType.TEXTBOOK;
import static no.sikt.nva.brage.migration.NvaType.VISUAL_ARTS;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.CRISTIN_RECORD_EXCEPTION;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.ERROR_BUCKET_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.HANDLE_REPORTS_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.SOURCE_CRISTIN;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.UPDATED_PUBLICATIONS_REPORTS_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.UPDATE_REPORTS_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.YYYY_MM_DD_HH_FORMAT;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.NOT_SUPPORTED_TYPE;
import static no.sikt.nva.brage.migration.merger.AssociatedArtifactMover.COULD_NOT_COPY_ASSOCIATED_ARTEFACT_EXCEPTION_MESSAGE;
import static no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger.DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.testing.PublicationGenerator.randomAdditionalIdentifier;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.model.storage.ResourceDao.CRISTIN_SOURCE;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn10;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn13;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.AssertionsKt.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.kms.model.NotFoundException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jdk.jfr.Description;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.mapper.InvalidIsmnRuntimeException;
import no.sikt.nva.brage.migration.merger.AssociatedArtifactException;
import no.sikt.nva.brage.migration.merger.BrageMergingReport;
import no.sikt.nva.brage.migration.merger.DiscardedFilesReport;
import no.sikt.nva.brage.migration.merger.PublicationMergeReport;
import no.sikt.nva.brage.migration.merger.UnmappableCristinRecordException;
import no.sikt.nva.brage.migration.merger.findexistingpublication.DuplicateDetectionCause;
import no.sikt.nva.brage.migration.record.Affiliation;
import no.sikt.nva.brage.migration.record.Contributor;
import no.sikt.nva.brage.migration.record.Customer;
import no.sikt.nva.brage.migration.record.EntityDescription;
import no.sikt.nva.brage.migration.record.Identity;
import no.sikt.nva.brage.migration.record.Pages;
import no.sikt.nva.brage.migration.record.PartOfSeries;
import no.sikt.nva.brage.migration.record.PublicationDate;
import no.sikt.nva.brage.migration.record.PublicationDateNva;
import no.sikt.nva.brage.migration.record.PublisherAuthority;
import no.sikt.nva.brage.migration.record.Range;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.ResourceOwner;
import no.sikt.nva.brage.migration.record.Type;
import no.sikt.nva.brage.migration.record.UnknownCustomerException;
import no.sikt.nva.brage.migration.record.content.ContentFile;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import no.sikt.nva.brage.migration.record.content.ResourceContent.BundleType;
import no.sikt.nva.brage.migration.record.license.License;
import no.sikt.nva.brage.migration.record.license.NvaLicense;
import no.sikt.nva.brage.migration.testutils.ExtendedFakeS3Client;
import no.sikt.nva.brage.migration.testutils.FakeResourceServiceThrowingException;
import no.sikt.nva.brage.migration.testutils.FakeS3ClientThrowingExceptionWhenCopying;
import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.UnconfirmedCourse;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.additionalidentifiers.CristinIdentifier;
import no.unit.nva.model.additionalidentifiers.HandleIdentifier;
import no.unit.nva.model.additionalidentifiers.SourceName;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.music.InvalidIsmnException;
import no.unit.nva.model.instancetypes.artistic.music.Ismn;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.music.MusicScore;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;
import no.unit.nva.model.instancetypes.event.ConferencePoster;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportBookOfAbstract;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationstate.FileImportedEvent;
import no.unit.nva.publication.model.business.publicationstate.ImportedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.MergedResourceEvent;
import no.unit.nva.publication.model.utils.CustomerList;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class BrageEntryEventConsumerTest extends ResourcesLocalTest {

    public static final UUID UUID = randomUUID();
    public static final Context CONTEXT = mock(Context.class);
    public static final long SOME_FILE_SIZE = 100L;
    public static final Type TYPE_BOOK = new Type(List.of(NvaType.BOOK.getValue()), NvaType.BOOK.getValue());
    public static final Type TYPE_CONFERENCE_REPORT = new Type(List.of(NvaType.CONFERENCE_REPORT.getValue()),
                                                               NvaType.CONFERENCE_REPORT.getValue());
    public static final Type TYPE_PROFESSIONAL_ARTICLE = new Type(List.of(PROFESSIONAL_ARTICLE.getValue()),
                                                                  PROFESSIONAL_ARTICLE.getValue());
    public static final Type TYPE_PERFORMING_ARTS = new Type(List.of(PERFORMING_ARTS.getValue()),
                                                             PERFORMING_ARTS.getValue());
    public static final Type TYPE_VISUAL_ARTS = new Type(List.of(VISUAL_ARTS.getValue()), VISUAL_ARTS.getValue());
    public static final Type TYPE_READER_OPINION = new Type(List.of(READER_OPINION.getValue()),
                                                            READER_OPINION.getValue());
    public static final Type TYPE_ANTHOLOGY = new Type(List.of(ANTHOLOGY.getValue()), ANTHOLOGY.getValue());
    public static final Type TYPE_CRISTIN_RECORD = new Type(List.of(CRISTIN_RECORD.getValue()),
                                                            CRISTIN_RECORD.getValue());
    public static final Type TYPE_TEXTBOOK = new Type(List.of(TEXTBOOK.getValue()), TEXTBOOK.getValue());
    public static final Type TYPE_FILM = new Type(List.of(FILM.getValue()), FILM.getValue());
    public static final Type TYPE_LITERARY_ARTS = new Type(List.of(LITERARY_ARTS.getValue()), LITERARY_ARTS.getValue());
    public static final Type TYPE_EXHIBITION_CATALOGUE = new Type(List.of(EXHIBITION_CATALOGUE.getValue()),
                                                                  EXHIBITION_CATALOGUE.getValue());
    public static final Type TYPE_POPULAR_SCIENCE_MONOGRAPH = new Type(List.of(POPULAR_SCIENCE_MONOGRAPH.getValue()),
                                                                       POPULAR_SCIENCE_MONOGRAPH.getValue());
    public static final Type TYPE_EDITORIAL = new Type(List.of(EDITORIAL.getValue()), EDITORIAL.getValue());
    public static final Type TYPE_POPULAR_SCIENCE_ARTICLE = new Type(List.of(POPULAR_SCIENCE_ARTICLE.getValue()),
                                                                     POPULAR_SCIENCE_ARTICLE.getValue());
    public static final Type TYPE_MUSIC = new Type(List.of(NvaType.RECORDING_MUSICAL.getValue()),
                                                   NvaType.RECORDING_MUSICAL.getValue());
    public static final Type TYPE_DESIGN_PRODUCT = new Type(List.of(NvaType.DESIGN_PRODUCT.getValue()),
                                                            NvaType.DESIGN_PRODUCT.getValue());
    public static final Type TYPE_PLAN_OR_BLUEPRINT = new Type(List.of(NvaType.PLAN_OR_BLUEPRINT.getValue()),
                                                               NvaType.PLAN_OR_BLUEPRINT.getValue());
    public static final Type TYPE_MAP = new Type(List.of(NvaType.MAP.getValue()), NvaType.MAP.getValue());
    public static final Type TYPE_BOOK_OF_ABSTRACTS = new Type(List.of(NvaType.BOOK_OF_ABSTRACTS.getValue()),
                                                               NvaType.BOOK_OF_ABSTRACTS.getValue());
    public static final Type TYPE_JOURNAL_ISSUE = new Type(List.of(NvaType.JOURNAL_ISSUE.getValue()),
                                                           NvaType.JOURNAL_ISSUE.getValue());
    public static final Type TYPE_CONFERENCE_LECTURE = new Type(List.of(NvaType.CONFERENCE_LECTURE.getValue()),
                                                                NvaType.CONFERENCE_LECTURE.getValue());
    public static final Type TYPE_REPORT = new Type(List.of(NvaType.REPORT.getValue()), NvaType.REPORT.getValue());
    public static final Type TYPE_RESEARCH_REPORT = new Type(List.of(NvaType.RESEARCH_REPORT.getValue()),
                                                             NvaType.RESEARCH_REPORT.getValue());

    public static final Type TYPE_BACHELOR = new Type(List.of(NvaType.BACHELOR_THESIS.getValue()),
                                                      NvaType.BACHELOR_THESIS.getValue());
    public static final Type TYPE_MASTER = new Type(List.of(NvaType.MASTER_THESIS.getValue()),
                                                    NvaType.MASTER_THESIS.getValue());
    public static final Type TYPE_PHD = new Type(List.of(NvaType.DOCTORAL_THESIS.getValue()),
                                                 NvaType.DOCTORAL_THESIS.getValue());
    public static final Type TYPE_STUDENT_PAPER_OTHERS = new Type(List.of(NvaType.STUDENT_PAPER_OTHERS.getValue()),
                                                                  NvaType.STUDENT_PAPER_OTHERS.getValue());
    public static final Type TYPE_OTHER_STUDENT_WORK = new Type(List.of(NvaType.STUDENT_PAPER.getValue()),
                                                                NvaType.STUDENT_PAPER.getValue());
    public static final Type TYPE_CONFERENCE_POSTER = new Type(List.of(NvaType.CONFERENCE_POSTER.getValue()),
                                                               NvaType.CONFERENCE_POSTER.getValue());
    public static final Type TYPE_SCIENTIFIC_MONOGRAPH = new Type(List.of(NvaType.SCIENTIFIC_MONOGRAPH.getValue()),
                                                                  NvaType.SCIENTIFIC_MONOGRAPH.getValue());
    public static final Type TYPE_INTERVIEW = new Type(List.of(NvaType.INTERVIEW.getValue()),
                                                       NvaType.INTERVIEW.getValue());
    public static final Type TYPE_PRESENTATION_OTHER = new Type(List.of(NvaType.PRESENTATION_OTHER.getValue()),
                                                                NvaType.PRESENTATION_OTHER.getValue());
    public static final Type TYPE_DATASET = new Type(List.of(NvaType.DATASET.getValue()), NvaType.DATASET.getValue());
    public static final Type TYPE_JOURNAL_ARTICLE = new Type(List.of(NvaType.JOURNAL_ARTICLE.getValue()),
                                                             NvaType.JOURNAL_ARTICLE.getValue());

    public static final Type TYPE_SCIENTIFIC_ARTICLE = new Type(List.of(NvaType.SCIENTIFIC_ARTICLE.getValue()),
                                                                NvaType.SCIENTIFIC_ARTICLE.getValue());
    public static final Instant EMBARGO_DATE = Instant.now();
    public static final PublicationDate PUBLICATION_DATE =
        new PublicationDate("2020", new PublicationDateNva.Builder().withYear("2020").build());
    public static final Organization TEST_ORGANIZATION = new Organization.Builder().withId(
        URI.create("https://api.nva.unit.no/customer/test")).build();
    public static final String FILENAME = "filename";
    public static final String HARD_CODED_CRISTIN_IDENTIFIER = "12345";
    public static final String RESOURCE_EXCEPTION_MESSAGE = "resourceExceptionMessage";
    public static final URI LICENSE_URI = URI.create("http://creativecommons.org/licenses/by-nc/4.0/");
    private static final Type TYPE_REPORT_WORKING_PAPER = new Type(List.of(NvaType.WORKING_PAPER.getValue()),
                                                                   NvaType.WORKING_PAPER.getValue());
    private static final Type TYPE_LECTURE = new Type(List.of(NvaType.LECTURE.getValue()), NvaType.LECTURE.getValue());
    private static final Type TYPE_CHAPTER = new Type(List.of(NvaType.CHAPTER.getValue()), NvaType.CHAPTER.getValue());
    private static final Type TYPE_SCIENTIFIC_CHAPTER = new Type(List.of(NvaType.SCIENTIFIC_CHAPTER.getValue()),
                                                                 NvaType.SCIENTIFIC_CHAPTER.getValue());
    private static final Type TYPE_MEDIA_FEATURE_ARTICLE = new Type(List.of(NvaType.MEDIA_FEATURE_ARTICLE.getValue()),
                                                                    NvaType.MEDIA_FEATURE_ARTICLE.getValue());
    private static final Type TYPE_SOFTWARE = new Type(List.of(NvaType.SOFTWARE.getValue()),
                                                       NvaType.SOFTWARE.getValue());
    private static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    private static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    private static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    private static final String INPUT_BUCKET_NAME = "some-input-bucket-name";
    public static final int ALMOST_HUNDRED_YEARS = 36487;
    public static final ResourceOwner RESOURCE_OWNER = new ResourceOwner("someOwner", randomUri());
    private static final String NTNU_CUSTOMER_NAME = "ntnu";
    public static final String NTNU_SHORT_NAME = "NTNU";
    private final String persistedStorageBucket = new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");
    private BrageEntryEventConsumer handler;
    private S3Driver s3Driver;
    private FakeS3Client s3Client;
    private ResourceService resourceService;

    public static Stream<Arguments> emptyPublicationInstanceSupplier() {
        return Stream.of(Arguments.of(new DegreePhd(null, null, Set.of()), TYPE_PHD),
                         Arguments.of(new DegreeBachelor(null, null), TYPE_BACHELOR),
                         Arguments.of(new DegreeMaster(null, null), TYPE_MASTER),
                         Arguments.of(new OtherStudentWork(null, null), TYPE_OTHER_STUDENT_WORK),
                         Arguments.of(new ConferenceReport(null), TYPE_CONFERENCE_REPORT),
                         Arguments.of(new ReportResearch(null), TYPE_RESEARCH_REPORT),
                         Arguments.of(new ReportBasic(null), TYPE_REPORT),
                         Arguments.of(new ReportWorkingPaper(null), TYPE_REPORT_WORKING_PAPER),
                         Arguments.of(new ReportBookOfAbstract(null), TYPE_BOOK_OF_ABSTRACTS));
    }

    @BeforeEach
    public void init() {
        super.init();
        when(customerService.fetchCustomers()).thenReturn(new CustomerList(List.of()));
        when(CONTEXT.getRemainingTimeInMillis()).thenReturn(100000);
        this.resourceService = getResourceServiceBuilder(client).build();
        this.s3Client = new ExtendedFakeS3Client();
        this.s3Driver = new S3Driver(s3Client, INPUT_BUCKET_NAME);
        mockSingleHitSearchApiResponse(SortableIdentifier.next(), 502);
        this.handler = new BrageEntryEventConsumer(s3Client, resourceService, uriRetriever);
    }

    @Test
    void shouldConvertBrageRecordToNvaPublicationWithCorrectCustomer() throws IOException {
        var brageGenerator = buildGeneratorForRecord();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), brageGenerator.getNvaPublication());
    }

    @Test
    void shouldSetResourceEventInstitutionToPublicationResourceOwnerAffiliationWhenImportingPublication()
        throws IOException {
        var brageGenerator = buildGeneratorForRecord();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT).publication();

        var resourceEvent = Resource.fromPublication(actualPublication)
                                .fetch(resourceService)
                                .orElseThrow()
                                .getResourceEvent();

        assertEquals(actualPublication.getResourceOwner().getOwnerAffiliation(), resourceEvent.institution());
    }

    @Test
    void shouldSetInstitutionShortNameForResourceEventWhenImportingPublicationFromBrage() throws IOException {
        var brageGenerator = buildGeneratorForRecord();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var publicationRepresentation = handler.handleRequest(s3Event, CONTEXT);

        var resource = Resource.fromPublication(publicationRepresentation.publication())
                           .fetch(resourceService)
                           .orElseThrow();
        var resourceEvent = (ImportedResourceEvent) resource.getResourceEvent();

        assertEquals(NTNU_SHORT_NAME, resourceEvent.importSource().getArchive());
    }

    @Test
    void shouldNotConvertBrageRecordMissingHandle() throws IOException {
        var brageGenerator = buildGeneratorForRecord();
        var record = brageGenerator.getBrageRecord();
        record.setId(UriWrapper.fromUri("").getUri());
        var s3Event = createNewBrageRecordEvent(record);
        handler.handleRequest(s3Event, CONTEXT);
        var actualErrorReport =
            extractActualReportFromS3Client(s3Event,
                                            IllegalArgumentException.class.getSimpleName(),
                                            record);

        var exception = actualErrorReport.get("exception").asText();
        assertThat(exception, containsString("Record must contain a handle"));
    }

    @Test
    void shouldAttachCertainMetadataFieldsToExistingPublicationWhenExistingPublicationDoesNotHaveThoseFields()
        throws IOException {
        // The metadata fields are currently Description, Abstract and handle
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_REPORT_WORKING_PAPER)
                                 .withCristinIdentifier("123456")
                                 .withDescription(List.of("My description"))
                                 .withAbstracts(List.of("My abstract"))
                                 .withResourceContent(createResourceContent())
                                 .build();
        var s3Driver = new S3Driver(s3Client, persistedStorageBucket);
        var file = new java.io.File("src/test/resources/testFile.txt");
        putAssociatedArtifactsToResourceStorage(brageGenerator, s3Driver, file);
        var cristinPublication = copyPublication(brageGenerator);
        cristinPublication.getEntityDescription().setDescription(null);
        cristinPublication.getEntityDescription().setAbstract(null);
        cristinPublication.setHandle(null);
        cristinPublication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("cristin", "123456")));
        resourceService.createPublicationFromImportedEntry(cristinPublication,
                                                           ImportSource.fromBrageArchive(randomString()));
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication.publication().getEntityDescription().getDescription(),
                   is(equalTo(brageGenerator.getNvaPublication().getEntityDescription().getDescription())));
        assertThat(actualPublication.publication().getEntityDescription().getAbstract(),
                   is(equalTo(brageGenerator.getNvaPublication().getEntityDescription().getAbstract())));
        assertThat(actualPublication.publication().getHandle(),
                   is(equalTo(brageGenerator.getNvaPublication().getHandle())));
    }

    @Test
    void shouldPrioritizeCristinImportedMetadataWhenMergingPublications() throws IOException {
        // The metadata fields are currently Description, Abstract and handle
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_REPORT_WORKING_PAPER)
                                 .withCristinIdentifier("123456")
                                 .withDescription(List.of("My description"))
                                 .withAbstracts(List.of("My abstract"))
                                 .build();
        var s3Driver = new S3Driver(s3Client, persistedStorageBucket);
        var file = new java.io.File("src/test/resources/testFile.txt");
        putAssociatedArtifactsToResourceStorage(brageGenerator, s3Driver, file);
        var cristinPublication = copyPublication(brageGenerator);
        var cristinDescription = randomString();
        var cristinAbstract = randomString();
        cristinPublication.getEntityDescription().setDescription(cristinDescription);
        cristinPublication.getEntityDescription().setAbstract(cristinAbstract);
        cristinPublication.setHandle(null);
        cristinPublication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier(CRISTIN_SOURCE, "123456")));
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(cristinPublication,
                                                               ImportSource.fromSource(Source.CRISTIN));
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication.publication().getIdentifier(), is(equalTo(existingPublication.getIdentifier())));
        assertThat(actualPublication.publication().getEntityDescription().getDescription(),
                   is(equalTo(cristinDescription)));
        assertThat(actualPublication.publication().getEntityDescription().getAbstract(), is(equalTo(cristinAbstract)));
    }

    @Test
    void shouldCreateNewPublicationWhenPublicationHasCristinIdWhichIsNotPresentInNva()
        throws IOException, NotFoundException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_REPORT_WORKING_PAPER)
                                 .withCristinIdentifier("123456")
                                 .withResourceContent(createResourceContent())
                                 .withResourceOwner(RESOURCE_OWNER)
                                 .withAssociatedArtifacts(createCorrespondingAssociatedArtifactWithLegalNote(null))
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldCreateNewPublicationWhenPublicationHasNoCristinId() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_REPORT_WORKING_PAPER).build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), brageGenerator.getNvaPublication());
    }

    @Test
    void shouldUpdatePublicationAndPersistDuplicateWarningWhenThereIsMultipleSearchResultsOnCristinId()
        throws IOException {
        var record = buildGeneratorObjectWithCristinId().getBrageRecord();
        var s3Event = createNewBrageRecordEvent(record);
        var existingPublicationIdentifiers = persistMultiplePublicationWithSameCristinId(record);
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        //Check that we updated one of the existing publications and not created a new one.
        assertThat(existingPublicationIdentifiers, hasItem(actualPublication.publication().getIdentifier()));

        // Assert that all the duplicates have been reported.
        var actualWarnReport = extractWarnReportFromS3Client(record);
        existingPublicationIdentifiers.forEach(
            identifier -> assertThat(actualWarnReport, containsString(identifier.toString())));
    }

    @Test
    void shouldConvertBookToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForBook();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldNotConvertSeriesNumberWithoutNumber() throws IOException {
        var brageGenerator = buildGeneratorForBookWithoutValidSeriesNumber();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertMapToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForMap();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertConferenceLectureToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_CONFERENCE_LECTURE).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertBookOfAbstractsToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_BOOK_OF_ABSTRACTS)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertJournalIssueToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_JOURNAL_ISSUE).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertReportToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForReport();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertReportWithUnconfirmedSeriesToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForReportWithUnconfirmedSeries();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertResearchReportToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForResearchReport();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertReportWorkingPaperToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_REPORT_WORKING_PAPER).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertBachelorToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForBachelor();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertMasterToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForMaster();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertPhdToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_PHD)
                                 .withPublicationDate(PUBLICATION_DATE)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertPhdWithPartsToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_PHD)
                                 .withPublicationDate(PUBLICATION_DATE)
                                 .withHasPart(List.of(randomString(), randomString()))
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertDatasetToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForDataset();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertJournalArticleToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForJournalArticle();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertJournalArticleWithoutJournalIdToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForJournalArticleWithoutId();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertJournalArticleWithUnconfirmedJournalToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForJournalArticleWithUnconfirmedJournal();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertScientificArticleToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_SCIENTIFIC_ARTICLE)
                                 .withJournalTitle("Journal")
                                 .withJournalId("id")
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertMediaFeatureArticleToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MEDIA_FEATURE_ARTICLE)
                                 .withJournalId("journal")
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertLectureToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_LECTURE).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertChapterToNvaPublicationAndPersistPartOfReport() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_CHAPTER)
                                 .withIsbn(randomIsbn10())
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);

        var partOfReport = getPartOfReport(s3Event, brageGenerator, actualPublication.publication());
        var expectedPartOfReport = new PartOfReport(actualPublication.publication(),
                                                    brageGenerator.getBrageRecord()).toJsonString();
        assertThat(partOfReport, is(equalTo(expectedPartOfReport)));
    }

    @Test
    void shouldConvertScientificChapterToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_SCIENTIFIC_CHAPTER).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertStudentPaperToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_STUDENT_PAPER_OTHERS).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertOtherStudentWorkToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_OTHER_STUDENT_WORK).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertConferencePosterToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_CONFERENCE_POSTER).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertDesignProductToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_DESIGN_PRODUCT).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertPlanOrBluePrintToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_PLAN_OR_BLUEPRINT).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertMusicToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MUSIC).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertScientificMonographToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_SCIENTIFIC_MONOGRAPH).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertInterviewToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_INTERVIEW).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertPresentationOtherToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_PRESENTATION_OTHER).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertWhenPublicationContextIsNull() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withPublishedDate(null)
                                 .withIsbn(randomIsbn10())
                                 .withType(TYPE_BOOK)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertToBookAnthologyWhenBrageRecordIsBookAndHasEditor() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withIsbn(randomIsbn10())
                                 .withType(TYPE_BOOK)
                                 .withContributor(new Contributor(new Identity(randomString(), randomString(), null),
                                                                  "Editor",
                                                                  null,
                                                                  List.of()))
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertWhenConferenceReport() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withPublishedDate(null)
                                 .withIsbn(randomIsbn10())
                                 .withType(TYPE_CONFERENCE_REPORT)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertProfessionalArticleToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withJournalTitle("Journal")
                                 .withJournalId("id")
                                 .withType(TYPE_PROFESSIONAL_ARTICLE)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertPerformingArtsToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_PERFORMING_ARTS).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertVisualArtsToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_VISUAL_ARTS).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertReaderOpinionToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_READER_OPINION).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertAnthologyToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_ANTHOLOGY).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertTextbookToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_TEXTBOOK).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertFilmToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_FILM).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertLiteraryArtsToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_LITERARY_ARTS).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertExhibitionCatalogueToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_EXHIBITION_CATALOGUE)
                                 .withNoContributors(true).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertPopularScienceMonographToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_POPULAR_SCIENCE_MONOGRAPH)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertEditorialToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_EDITORIAL)
                                 .withJournalTitle(randomString())
                                 .withVolume(randomString())
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertPopularScienceArticle() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_POPULAR_SCIENCE_ARTICLE)
                                 .withJournalTitle(randomString())
                                 .withVolume(randomString())
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldConvertToPublicationWithUnconfirmedJournalWhenJournalIdIsNotPresent() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MEDIA_FEATURE_ARTICLE)
                                 .withJournalTitle("Some Very Popular Journal")
                                 .withIssn(List.of(randomIssn()))
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldAdd100YearEmbargoToPublicationWhenLegalNoteRequiresEmbargo() throws IOException {
        var accessCode = "Klausulert: Kan bare siteres etter nærmere avtale med forfatter";
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_TEXTBOOK)
                                 .withAccessCode(accessCode)
                                 .withResourceContent(createResourceContent())
                                 .withAssociatedArtifacts(createCorrespondingAssociatedArtifactWithLegalNote(null))
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        var file = (File) actualPublication.publication().getAssociatedArtifacts().getFirst();

        var embargoToBeAfterThisDate = Instant.now().plus(Duration.ofDays(ALMOST_HUNDRED_YEARS));

        assertThat(file.getLegalNote(), is(equalTo(accessCode)));
        assertThat(file.getEmbargoDate().orElseThrow().isAfter(embargoToBeAfterThisDate), is(true));
    }

    @Test
    void shouldConvertCristinRecordToPublicationAndMergeWithExistingPublicationWithTheSameCristinId()
        throws IOException {
        var cristinIdentifier = randomString();
        var publication = randomPublication(ConferencePoster.class);
        publication.setAdditionalIdentifiers(getDefaultIdentifiers(cristinIdentifier));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Cristin", cristinIdentifier)));
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription()
                               .getReference().getPublicationInstance().getInstanceType();
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());

        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withMainTitle("Dynamic Response of Floating Wind Turbines")
                            .withContributor(brageContributor)
                            .withPublicationDate(new PublicationDate("2023",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2023")
                                                                         .build()))
                            .withType(new Type(List.of(), instanceType)).build();
        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication.publication().getEntityDescription().getReference().getPublicationContext(),
                   is(not(nullValue())));
    }

    @Test
    void shouldAddBrageHandleAsAdditionalIdentifierAndWhenMergingDegreeWithPublicationThatHasHandleInAdditionalIdentifiers()
        throws IOException {
        var publicationInstance = new DegreeBachelor(new MonographPages.Builder().build(), null);
        var handle = randomUri();
        var expectedAdditionalIdentifier = new HandleIdentifier(SourceName.fromBrage("ntnu"), randomUri());
        var brageTestRecord =
            generateBrageRecordAndPersistDuplicateByCristinIdentifier(publicationInstance,
                                                                      TYPE_BACHELOR,
                                                                      expectedAdditionalIdentifier);
        var existingPublication = brageTestRecord.getExistingPublication();

        var s3Event = createNewBrageRecordEvent(brageTestRecord.getGeneratorBuilder()
                                                    .withHandle(handle).build().getBrageRecord());
        var publicationRepresentation = handler.handleRequest(s3Event, CONTEXT);
        var handles = publicationRepresentation.publication()
                          .getAdditionalIdentifiers().stream()
                          .filter(HandleIdentifier.class::isInstance)
                          .toList();

        assertThat(handles, hasItem(expectedAdditionalIdentifier));
        var set = new HashSet<>(existingPublication.getAdditionalIdentifiers());
        set.add(
            new HandleIdentifier(SourceName.fromBrage(brageTestRecord.getGeneratorBuilder().getCustomer().getName()),
                                 handle));
        var expectedUpdatedPublication = existingPublication.copy()
                                             .withAdditionalIdentifiers(set)
                                             .withModifiedDate(publicationRepresentation.publication()
                                                                   .getModifiedDate())
                                             .withImportDetails(publicationRepresentation.publication()
                                                                    .getImportDetails())
                                             .build();

        assertThat(publicationRepresentation.publication().getAdditionalIdentifiers(),
                   is(equalTo(expectedUpdatedPublication.getAdditionalIdentifiers())));
        assertThat(publicationRepresentation.publication(), is(equalTo(expectedUpdatedPublication)));
    }

    @Test
    void shouldNotMergeDegreeWithABookWhenFetchedByTitleSearch() throws IOException,
                                                       nva.commons.apigateway.exceptions.NotFoundException {
        var publication = randomPublication(DegreeBachelor.class);
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());

        String mainTitle = "Dynamic Response of Floating Wind Turbines";
        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withMainTitle(mainTitle)
                            .withContributor(brageContributor)
                            .withPublicationDate(new PublicationDate("2023",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2023")
                                                                         .build()))
                            .withType(new Type(List.of(), "Book")).build();

        mockSingleHitSearchApiResponseByTitleAndTypes(existingPublication.getIdentifier(), 200, mainTitle, "Book");

        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);
        var notUpdatedPublication = resourceService.getPublicationByIdentifier(existingPublication.getIdentifier());
        assertThat(notUpdatedPublication, is(equalTo(existingPublication)));
    }

    @Test
    void shouldMergeBookWithADegreeWhenFetchedByCristinIdentifier() throws IOException,
                                                       nva.commons.apigateway.exceptions.NotFoundException {
        var cristinIdentifier = randomString();
        var publication = randomPublication(NonFictionMonograph.class);
        publication.getEntityDescription().getReference().setDoi(null);
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Cristin", cristinIdentifier)));
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());

        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withMainTitle("Dynamic Response of Floating Wind Turbines")
                            .withContributor(brageContributor)
                            .withCristinIdentifier(cristinIdentifier)
                            .withPublicationDate(new PublicationDate("2023",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2023")
                                                                         .build()))
                            .withType(new Type(List.of(), "DegreeBachelor")).build();
        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(existingPublication.getIdentifier());

        assertThat(updatedPublication, is(not(equalTo(existingPublication))));
    }

    @Test
    void shouldMergeJournalWithAUnconfirmedJournalWhenFetchedByTitleFinder() throws IOException,
                                                                                    nva.commons.apigateway.exceptions.NotFoundException,
                                                                                    InvalidIssnException {
        var publication = randomPublication(JournalArticle.class);
        publication.getEntityDescription().getReference().setPublicationContext(new UnconfirmedJournal(randomString()
            , null, null));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());

        var title = "Dynamic Response of Floating Wind Turbines";
        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withMainTitle(title)
                            .withContributor(brageContributor)
                            .withJournalId("12345")
                            .withPublicationDate(new PublicationDate("2023",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2023")
                                                                         .build()))
                            .withType(new Type(List.of(), "JournalArticle")).build();

        mockSingleHitSearchApiResponseByTitleAndTypes(existingPublication.getIdentifier(), 200, title, "Journal",
                                                      "UnconfirmedJournal");

        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(existingPublication.getIdentifier());

        assertThat(updatedPublication, is(not(equalTo(existingPublication))));
    }

    @Test
    void shouldMergeUnconfirmedJournalWithAJournalWhenFetchedByTitleFinder() throws IOException,
                                                                                    nva.commons.apigateway.exceptions.NotFoundException {
        var publication = randomPublication(JournalArticle.class);
        publication.getEntityDescription().getReference().setPublicationContext(new Journal(randomUri()));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());

        var title = "Dynamic Response of Floating Wind Turbines";
        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withMainTitle(title)
                            .withContributor(brageContributor)
                            .withJournalId(null)
                            .withPublicationDate(new PublicationDate("2023",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2023")
                                                                         .build()))
                            .withType(new Type(List.of(), "JournalArticle")).build();

        mockSingleHitSearchApiResponseByTitleAndTypes(existingPublication.getIdentifier(), 200, title,
                                                      "UnconfirmedJournal", "Journal");

        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(existingPublication.getIdentifier());

        assertThat(updatedPublication, is(not(equalTo(existingPublication))));
    }

    @Test
    void shouldMergePublicationContextAndUseCourseFromBragePublicationWhenPublicationContextIsDegree()
        throws IOException, InvalidUnconfirmedSeriesException {
        var cristinIdentifier = randomString();
        var publication = randomPublication(DegreeBachelor.class);
        publication.setAdditionalIdentifiers(getDefaultIdentifiers(cristinIdentifier));
        publication.setAdditionalIdentifiers(Set.of(cristinAdditionalIdentifier(cristinIdentifier)));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().getReference().setPublicationContext(new Degree(null, null, null, null,
                                                                                           List.of(), null));
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription()
                               .getReference().getPublicationInstance().getInstanceType();
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());
        var subjectCode = randomString();
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MASTER)
                                 .withCristinIdentifier(cristinIdentifier)
                                 .withMainTitle("Dynamic - Response of Floating Wind Turbines! Report")
                                 .withContributor(brageContributor)
                                 .withType(new Type(List.of(), instanceType))
                                 .withPublicationDate(new PublicationDate("2023",
                                                                          new PublicationDateNva.Builder()
                                                                              .withYear("2023")
                                                                              .build()))
                                 .withSubjectCode(subjectCode)
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        var actualPublicationContext = ((Degree) actualPublication.publication().getEntityDescription()
                                                     .getReference()
                                                     .getPublicationContext());

        assertThat(((UnconfirmedCourse) actualPublicationContext.getCourse()).code(), is(equalTo(subjectCode)));
    }

    @Test
    void shouldMigrateHiddenFileFromIncomingWhenExistingPublicationDoesNotHaveHiddenFile()
        throws IOException, InvalidUnconfirmedSeriesException {
        var cristinIdentifier = randomString();
        var publication = randomPublication(DegreeBachelor.class);
        publication.setAdditionalIdentifiers(getDefaultIdentifiers(cristinIdentifier));
        publication.setAdditionalIdentifiers(Set.of(cristinAdditionalIdentifier(cristinIdentifier)));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().getReference().setPublicationContext(new Degree(null, null, null, null,
                                                                                           List.of(), null));
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription()
                               .getReference().getPublicationInstance().getInstanceType();
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());
        var subjectCode = randomString();
        var brageGenerator =
            new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MASTER)
                .withCristinIdentifier(cristinIdentifier)
                .withMainTitle("Dynamic - Response of Floating Wind Turbines! Report")
                .withContributor(brageContributor)
                .withType(new Type(List.of(), instanceType))
                .withPublicationDate(new PublicationDate("2023",
                                                         new PublicationDateNva.Builder().withYear("2023").build()))
                .withResourceContent(new ResourceContent(List.of(new ContentFile(randomString(),
                                                                                 BundleType.LICENSE,
                                                                                 randomString(),
                                                                                 randomUUID(),
                                                                                 null,
                                                                                 null))))
                .withSubjectCode(subjectCode)
                .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublicationRepresentation = handler.handleRequest(s3Event, CONTEXT);

        assertTrue(actualPublicationRepresentation.publication().getAssociatedArtifacts().stream()
                       .anyMatch(HiddenFile.class::isInstance));
    }

    private static Set<AdditionalIdentifierBase> getDefaultIdentifiers(String cristinIdentifier) {
        return Set.of(new CristinIdentifier(SourceName.nva(), cristinIdentifier));
    }

    @Test
    void shouldPersistNewFileWithUserInstanceFromIncomingPublicationWhenMergingPublications()
        throws IOException, InvalidUnconfirmedSeriesException {
        var cristinIdentifier = randomString();
        var publication = randomPublication(DegreeBachelor.class);
        publication.setAdditionalIdentifiers(getDefaultIdentifiers(cristinIdentifier));
        publication.setAdditionalIdentifiers(Set.of(cristinAdditionalIdentifier(cristinIdentifier)));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().getReference().setPublicationContext(new Degree(null, null, null, null,
                                                                                           List.of(), null));
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        publication.setAssociatedArtifacts(AssociatedArtifactList.empty());
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription()
                               .getReference().getPublicationInstance().getInstanceType();
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());
        var subjectCode = randomString();
        var newFileIdentifier = randomUUID();
        var brageGenerator =
            new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MASTER)
                .withCristinIdentifier(cristinIdentifier)
                .withMainTitle("Dynamic - Response of Floating Wind Turbines! Report")
                .withContributor(brageContributor)
                .withType(new Type(List.of(), instanceType))
                .withPublicationDate(new PublicationDate("2023",
                                                         new PublicationDateNva.Builder().withYear("2023").build()))
                .withResourceContent(new ResourceContent(List.of(new ContentFile(randomString(),
                                                                                 BundleType.LICENSE,
                                                                                 randomString(),
                                                                                 newFileIdentifier,
                                                                                 null,
                                                                                 null))))
                .withSubjectCode(subjectCode)
                .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);

        var updatedResource = Resource.fromPublication(existingPublication).fetch(resourceService).orElseThrow();

        var fileEntry = updatedResource.getFileEntry(new SortableIdentifier(newFileIdentifier.toString()));
        var user = brageGenerator.getNvaPublication().getResourceOwner().getOwner().getValue();

        assertEquals(user, fileEntry.orElseThrow().getOwner().toString());
        Assertions.assertNotEquals(existingPublication.getResourceOwner().getOwner().getValue(),
                                   fileEntry.orElseThrow().getOwner().toString());
    }

    @Test
    void shouldNotThrowExceptionWhenPublicationWithoutContributors() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_TEXTBOOK)
                                 .withNoContributors(true)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        assertThatPublicationsMatch(actualPublication.publication(), expectedPublication);
    }

    @Test
    void shouldNotAddLineBreaksToDescriptionAndAbstractWhenBrageRecordContainsSingleValue() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_TEXTBOOK)
                                 .withDescription(List.of(randomString()))
                                 .withAbstracts(List.of(randomString()))
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        assertThat(actualPublication.publication().getEntityDescription().getDescription(), not(containsString("\n")));
        assertThat(actualPublication.publication().getEntityDescription().getAbstract(), not(containsString("\n")));
    }

    @Test
    void shouldNotCreateAbstractAndDescriptionWhenSingleValueIsBlankString() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_TEXTBOOK)
                                 .withDescription(List.of("  "))
                                 .withAbstracts(List.of("  "))
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        assertThat(actualPublication.publication().getEntityDescription().getDescription(), is(nullValue()));
        assertThat(actualPublication.publication().getEntityDescription().getAbstract(), is(nullValue()));
    }

    @Test
    void shouldStoreExceptionWhenConvertingCristinRecordAndPublicationWithMatchingCristinIdDoesExist()
        throws IOException, BadRequestException {
        var existingPublication = randomPublication().copy().withAdditionalIdentifiers(Set.of()).build();
        Resource.fromPublication(existingPublication)
            .persistNew(resourceService, UserInstance.fromPublication(existingPublication));
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_CRISTIN_RECORD)
                                 .withCristinIdentifier(randomString())
                                 .build();
        var record = brageGenerator.getBrageRecord();
        var s3Event = createNewBrageRecordEvent(record);
        var publication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(publication, is(nullValue()));
        var errorReport = extractActualReportFromS3Client(s3Event,
                                                          UnmappableCristinRecordException.class.getSimpleName(),
                                                          record);
        var exception = errorReport.get("exception").asText();
        assertThat(exception, containsString(CRISTIN_RECORD_EXCEPTION));
    }

    @Test
    void shouldStoreExceptionWhenTypeIsNotSupportedInPublicationContext() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_SOFTWARE).build();
        var record = brageGenerator.getBrageRecord();
        var s3Event = createNewBrageRecordEvent(record);
        var publication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(publication, is(nullValue()));
        var errorReport = extractActualReportFromS3Client(s3Event,
                                                          PublicationContextException.class.getSimpleName(),
                                                          record);
        var exception = errorReport.get("exception").asText();
        assertThat(exception, containsString(NOT_SUPPORTED_TYPE));
        assertThat(exception, containsString(TYPE_SOFTWARE.getBrage().getFirst()));
    }

    @Test
    void shouldStoreExceptionWhenInvalidBrageRecordIsProvided() throws IOException {
        var s3Event = createNewInvalidBrageRecordEvent();
        var publication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(publication, is(nullValue()));
        var errorReport =
            extractActualReportFromS3ClientForInvalidRecord(s3Event,
                                                            UnknownCustomerException.class.getSimpleName());
        var exception = errorReport.get("exception").asText();
        assertThat(exception, containsString("Unknown customer"));
    }

    @Test
    void shouldPersistPublicationInDatabase()
        throws IOException, nva.commons.apigateway.exceptions.NotFoundException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withPublishedDate(null)
                                 .withType(TYPE_BOOK)
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(resourceService.getPublicationByIdentifier(actualPublication.publication()
                                                                                   .getIdentifier()),
                                    brageGenerator.getNvaPublication());
    }

    @Test
    void shouldTryToPersistPublicationInDatabaseSeveralTimesWhenResourceServiceIsThrowingException()
        throws IOException {
        var fakeResourceServiceThrowingException = new FakeResourceServiceThrowingException(client);
        this.handler = new BrageEntryEventConsumer(s3Client, fakeResourceServiceThrowingException, uriRetriever);
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder().withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var record = nvaBrageMigrationDataGenerator.getBrageRecord();
        var s3Event = createNewBrageRecordEvent(record);
        var publication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(publication, is(nullValue()));
        assertThat(fakeResourceServiceThrowingException.getNumberOfAttempts(), is(greaterThan(1)));
    }

    @Test
    void shouldStoreExceptionIfItCannotCopyAssociatedArtifacts() throws IOException {
        this.s3Client = new FakeS3ClientThrowingExceptionWhenCopying();
        this.s3Driver = new S3Driver(s3Client, INPUT_BUCKET_NAME);
        this.handler = new BrageEntryEventConsumer(s3Client, resourceService, uriRetriever);
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withPublishedDate(null)
                                 .withType(TYPE_BOOK)
                                 .withResourceContent(createResourceContent())
                                 .withAssociatedArtifacts(createCorrespondingAssociatedArtifactWithLegalNote(null))
                                 .build();
        var record = brageGenerator.getBrageRecord();
        var s3Event = createNewBrageRecordEvent(record);
        var publication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(publication, is(nullValue()));
        var errorReport = extractActualReportFromS3Client(s3Event,
                                                          AssociatedArtifactException.class.getSimpleName(),
                                                          record);
        var exception = errorReport.get("exception").asText();
        assertThat(exception, containsString(COULD_NOT_COPY_ASSOCIATED_ARTEFACT_EXCEPTION_MESSAGE));
    }

    @Test
    void shouldCopyAssociatedArtifactsToResourceStorage() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withPublishedDate(null)
                                 .withResourceContent(createResourceContent())
                                 .withType(TYPE_BOOK)
                                 .build();
        var s3Event = createNewBrageRecordEventWithSpecifiedObjectKey(brageGenerator.getBrageRecord());
        var expectedMultipartCopyObjectRequest =
            CompleteMultipartUploadResponse.builder()
                .bucket(new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME"))
                .key(UUID.toString())
                .build();
        handler.handleRequest(s3Event, CONTEXT);
        var fakeS3Client = (ExtendedFakeS3Client) s3Client;
        var actualCopyObjectRequests = fakeS3Client.getMultipartCopiedResults();
        assertThat(actualCopyObjectRequests, hasSize(1));
        assertThat(actualCopyObjectRequests, contains(expectedMultipartCopyObjectRequest));
    }

    @Test
    void shouldStoreErrorWhenMandatoryFieldsAreMissing() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_BOOK)
                                                 .withIsbn(randomIsbn10())
                                                 .withNullHandle()
                                                 .build();
        var record = nvaBrageMigrationDataGenerator.getBrageRecord();
        var s3Event = createNewBrageRecordEvent(record);
        var publication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(publication, is(nullValue()));
        var errorReport = extractActualReportFromS3Client(s3Event, IllegalArgumentException.class.getSimpleName(),
                                                          record);
        var exception = errorReport.get("exception").asText();
        assertThat(exception, containsString("Record must contain a handle"));
    }

    @Test
    void shouldSaveErrorReportInS3ContainingTheOriginalInputData() throws IOException {
        resourceService = new FakeResourceServiceThrowingException(client);
        this.handler = new BrageEntryEventConsumer(s3Client, resourceService, uriRetriever);
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder().withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var publication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(publication, is(nullValue()));

        var actualReport = extractActualReportFromS3Client(s3Event, RuntimeException.class.getSimpleName(),
                                                           nvaBrageMigrationDataGenerator.getBrageRecord());
        var input = actualReport.get("input").toPrettyString();
        var actualErrorReportBrageRecord = JsonUtils.dtoObjectMapper.readValue(input, Record.class);

        assertThat(actualErrorReportBrageRecord, is(equalTo(nvaBrageMigrationDataGenerator.getBrageRecord())));
    }

    @Test
    void shouldSaveHandleAndResourceIdentifierReportInS3() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder().withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        var actualStoredHandleString =
            extractHandleReportFromS3Client(s3Event,
                                            actualPublication.publication(),
                                            nvaBrageMigrationDataGenerator.getBrageRecord().getId());
        assertThat(actualStoredHandleString,
                   is(equalTo(nvaBrageMigrationDataGenerator.getBrageRecord().getId().toString())));
    }

    @Test
    void shouldSaveSuccessReportContainingIncomingHandleWhenUpdatingExistingPublication()
        throws IOException {
        var cristinIdentifier = randomString();
        var publication = randomPublication(ConferencePoster.class);
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Cristin", cristinIdentifier)));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription()
                               .getReference().getPublicationInstance().getInstanceType();
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());

        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withMainTitle("Dynamic Response of Floating Wind Turbines")
                            .withContributor(brageContributor)
                            .withCristinIdentifier(cristinIdentifier)
                            .withPublicationDate(new PublicationDate("2023",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2023")
                                                                         .build()))
                            .withType(new Type(List.of(), instanceType)).build();
        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        var updatedPublication = handler.handleRequest(s3Event, CONTEXT);

        var actualStoredHandleString =
            extractUpdatedPublicationsHandleReportFromS3Client(s3Event,
                                                               updatedPublication.publication(),
                                                               generator.getBrageRecord()
                                                                   .getId());
        assertThat(updatedPublication.publication().getIdentifier(), is(equalTo(existingPublication.getIdentifier())));
        assertThat(actualStoredHandleString,
                   is(equalTo(generator.getBrageRecord().getId().toString())));
    }

    @Test
    void shouldPersistMergeReport() throws IOException {
        var cristinIdentifier = randomString();
        var publication = randomPublication(DataSet.class);
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Cristin", cristinIdentifier),
                                                    new CristinIdentifier(SourceName.fromBrage("ntnu"),
                                                                          cristinIdentifier)));
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        publication.setHandle(null);
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription()
                               .getReference().getPublicationInstance().getInstanceType();
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());
        var nvaBrageMigrationDataGenerator =
            new NvaBrageMigrationDataGenerator.Builder()
                .withPublishedDate(null)
                .withCristinIdentifier(cristinIdentifier)
                .withType(new Type(List.of(), instanceType))
                .withContributor(brageContributor)
                .withMainTitle(existingPublication.getEntityDescription().getMainTitle())
                .withPublicationDate(new PublicationDate("2023",
                                                         new PublicationDateNva.Builder().withYear("2023").build()))
                .build();
        existingPublication.getEntityDescription()
            .setMainTitle(nvaBrageMigrationDataGenerator.getNvaPublication().getEntityDescription().getMainTitle());
        resourceService.updatePublication(existingPublication);
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);
        var storedMergeReportString =
            extractUpdateReportFromS3ByUpdateSource(s3Event,
                                                    existingPublication,
                                                    nvaBrageMigrationDataGenerator.getBrageRecord().getId(), "CRISTIN");
        var storedMergeReport = JsonUtils.dtoObjectMapper.readValue(storedMergeReportString, BrageMergingReport.class);
        var updatedPublication = Resource.fromPublication(existingPublication)
                                     .fetch(resourceService)
                                     .orElseThrow()
                                     .toPublication();
        var expectedPublication = existingPublication.copy().withModifiedDate(storedMergeReport.oldImage()
                                                                                  .getModifiedDate()).build();
        assertEquals(expectedPublication, storedMergeReport.oldImage());
        assertThat(storedMergeReport.newImage(), is(equalTo(updatedPublication)));
    }

    @Test
    void shouldMergePublicationsIfDoiDuplicateExists() throws IOException {
        final var doi = randomDoi();
        var publication = randomPublication(ConferencePoster.class);
        publication.setAdditionalIdentifiers(getDefaultIdentifiers(randomString()));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        publication.getEntityDescription().getReference().setDoi(doi);
        publication.setHandle(null);
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        final var instanceType =
            existingPublication.getEntityDescription().getReference().getPublicationInstance().getInstanceType();
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        final var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                                     "ARTIST", null, List.of());
        assertThat(existingPublication.getHandle(), is(nullValue()));

        var listOfExistingPublications =
            List.of(new ResourceWithId(UriWrapper.fromUri("https://api.test.nva.aws.unit.no/publication/"
                                                          + existingPublication.getIdentifier().toString()).getUri()));
        var searchResourceApiResponseSingleHit = new SearchResourceApiResponse(listOfExistingPublications.size(),
                                                                               listOfExistingPublications);
        var singleHitOptional = Optional.of(searchResourceApiResponseSingleHit.toString());
        when(uriRetriever.getRawContent(any(), any())).thenReturn(singleHitOptional);

        var nvaBrageMigrationDataGenerator =
            new NvaBrageMigrationDataGenerator.Builder()
                .withPublishedDate(null)
                .withType(new Type(List.of(), instanceType))
                .withDoi(doi)
                .withContributor(brageContributor)
                .withMainTitle(existingPublication.getEntityDescription().getMainTitle())
                .withPublicationDate(new PublicationDate("2023",
                                                         new PublicationDateNva.Builder().withYear("2023").build()))
                .build();

        var brageRecord = nvaBrageMigrationDataGenerator.getBrageRecord();
        var s3Event = createNewBrageRecordEvent(brageRecord);
        var updatedPublication = handler.handleRequest(s3Event, CONTEXT);
        var brageHandle = updatedPublication.publication().getAdditionalIdentifiers().stream()
                              .filter(HandleIdentifier.class::isInstance)
                              .map(HandleIdentifier.class::cast)
                              .findFirst()
                              .orElseThrow();
        assertThat(brageHandle,
                   is(equalTo(new HandleIdentifier(SourceName.fromBrage(brageRecord.getCustomer().getName()),
                                                   brageRecord.getId()))));
        assertThat(updatedPublication.publication().getIdentifier(), is(equalTo(existingPublication.getIdentifier())));

        var actualStoredHandleString = extractUpdatedPublicationsHandleReportFromS3Client(s3Event,
                                                                                          existingPublication,
                                                                                          brageRecord.getId());
        assertThat(actualStoredHandleString,
                   is(equalTo(nvaBrageMigrationDataGenerator.getBrageRecord().getId().toString())));
    }

    @Test
    void shouldNotMergePublicationsIfNoDoiDuplicateExists() throws IOException {
        var searchResourceApiResponseNoHits = new SearchResourceApiResponse(0, null);
        var noHitsOptional = Optional.of(searchResourceApiResponseNoHits.toString());
        when(uriRetriever.getRawContent(any(), any())).thenReturn(noHitsOptional);

        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .withDoi(randomDoi())
                                                 .build();

        var brageRecord = nvaBrageMigrationDataGenerator.getBrageRecord();
        var s3Event = createNewBrageRecordEvent(brageRecord);
        var publication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(publication.publication().getAdditionalIdentifiers().stream()
                       .filter(HandleIdentifier.class::isInstance)
                       .map(HandleIdentifier.class::cast)
                       .findFirst()
                       .orElseThrow(),
                   is(equalTo(new HandleIdentifier(SourceName.fromBrage(brageRecord.getCustomer().getName()),
                                                 brageRecord.getId()))));

        var actualStoredHandleString = extractHandleReportFromS3Client(s3Event,
                                                                       publication.publication(),
                                                                       brageRecord.getId());
        assertThat(actualStoredHandleString,
                   is(equalTo(nvaBrageMigrationDataGenerator.getBrageRecord().getId().toString())));
    }

    @Test
    void whenMergingWithExistingPublicationTheHandleReportShouldBeStoredUnderUpdatedPublicationsFolder()
        throws IOException {
        var publication = randomPublication(ConferencePoster.class);
        publication.setAdditionalIdentifiers(getDefaultIdentifiers(randomString()));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription()
                               .getReference().getPublicationInstance().getInstanceType();
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());
        mockSingleHitSearchApiResponse(existingPublication.getIdentifier(), 200);

        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withMainTitle("Dynamic Response of Floating Wind Turbines")
                            .withContributor(brageContributor)
                            .withPublicationDate(new PublicationDate("2023",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2023")
                                                                         .build()))
                            .withType(new Type(List.of(), instanceType)).build();

        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        var updatedPublication = handler.handleRequest(s3Event, CONTEXT);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        var updatedPublicationsFolder = UnixPath.of(UPDATED_PUBLICATIONS_REPORTS_PATH);
        var filesInUpdatedPublicationsFolder = s3Driver.getFiles(updatedPublicationsFolder);
        assertThat(filesInUpdatedPublicationsFolder, is(not(empty())));

        var storedHandleString = extractUpdatedPublicationsHandleReportFromS3Client(s3Event,
                                                                                    updatedPublication.publication(),
                                                                                    generator.getBrageRecord().getId());
        assertThat(storedHandleString, is(not(nullValue())));
    }

    @Test
    void whenCreatingNewPublicationTheHandleReportShouldBeStoredUnderHandleReportsFolder() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_REPORT_WORKING_PAPER).build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var publication = handler.handleRequest(s3Event, CONTEXT);

        var s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        var handleReportsFolder = UnixPath.of(HANDLE_REPORTS_PATH);
        var filesInHandleReportsFolder = s3Driver.getFiles(handleReportsFolder);
        assertThat(filesInHandleReportsFolder, is(not(empty())));

        var storedHandleString = extractHandleReportFromS3Client(s3Event,
                                                                 publication.publication(),
                                                                 brageGenerator.getBrageRecord().getId());
        assertThat(storedHandleString, is(not(nullValue())));
    }

    @Test
    void whenCristinPublicationIsMissingImportingMinimalRecordShouldNotCreateNewPublication() throws IOException {
        var cristinIdentifier = randomString();
        var minimalRecord = createMinimalRecord(cristinIdentifier);
        var contentFile = createContentFile();
        minimalRecord.setContentBundle(new ResourceContent(List.of(contentFile)));
        var s3Event = createNewBrageRecordEvent(minimalRecord);
        handler.handleRequest(s3Event, CONTEXT);
        var publications = resourceService.getPublicationsByCristinIdentifier(cristinIdentifier);
        assertThat(publications, hasSize(0));
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        var errorFiles = s3Driver.getFiles(UnixPath.of("ERROR"));
        assertThat(errorFiles, hasSize(1));
    }

    @Test
    void shouldStoreValidIsmnInIsmnObjectWhenConvertingMusicalPieces() throws IOException, InvalidIsmnException {
        var validIsmn1 = "9790900514608";
        var validIsmn2 = "9790900514615";
        var brageGenerator = new NvaBrageMigrationDataGenerator
                                     .Builder()
                                 .withType(TYPE_MUSIC)
                                 .withIsmn(List.of(validIsmn1, validIsmn2))

                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var publication = handler.handleRequest(s3Event, CONTEXT);
        var musicPerformance = (MusicPerformance) publication.publication().getEntityDescription()
                                                      .getReference()
                                                      .getPublicationInstance();
        var actualManifestation = musicPerformance.getManifestations();
        assertThat(actualManifestation, containsInAnyOrder(new MusicScore[]{
            new MusicScore(null,
                           null,
                           null,
                           null,
                           new Ismn(validIsmn1)),
            new MusicScore(null,
                           null,
                           null,
                           null,
                           new Ismn(validIsmn2))}));
    }

    @Test
    void shouldStoreErrorReportIfTheMusicalPerformanceHasInvalidIsmn() throws IOException {
        var invalidIsmn = "invalid ismn 1";
        var brageGenerator = new NvaBrageMigrationDataGenerator
                                     .Builder()
                                 .withType(TYPE_MUSIC)
                                 .withIsmn(List.of(invalidIsmn))
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);
        var publication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(publication, is(nullValue()));

        var actualReport = extractActualReportFromS3Client(s3Event, InvalidIsmnRuntimeException.class.getSimpleName(),
                                                           brageGenerator.getBrageRecord());
        var exception = actualReport.get("exception").asText();
        assertThat(exception, containsString(invalidIsmn));
    }

    @Test
    void shouldPersistReportContainingInformationRegardingDiscardedFilesDuringMerging()
        throws IOException {
        final var cristinIdentifier = randomString();
        var publication = randomPublication(NonFictionMonograph.class);
        publication.getEntityDescription()
            .getReference().setPublicationContext(new Book.BookBuilder().withIsbnList(List.of(randomIsbn10())).build());
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.setHandle(randomUri());
        publication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(randomOpenFile())));
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Cristin", cristinIdentifier)));
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());
        var isbn = getIsbn(existingPublication);
        mockSearchPublicationByIsbnResponse(existingPublication.getIdentifier(), isbn);
        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withCristinIdentifier(cristinIdentifier)
                            .withType(TYPE_BOOK)
                            .withContributor(brageContributor)
                            .withPublicationDate(new PublicationDate("2023",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2023")
                                                                         .build()))
                            .withMainTitle(existingPublication.getEntityDescription().getMainTitle())
                            .withResourceContent(new ResourceContent(List.of(createContentFile(),
                                                                             createContentFile())))
                            .build();

        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        var updatedPublication = handler.handleRequest(s3Event, CONTEXT);

        var discardedFilesReport = extractDiscardedFilesReportFromS3(generator.getBrageRecord(),
                                                                     s3Event,
                                                                     updatedPublication.publication());
        assertThat(discardedFilesReport.getDiscardedFromUpdatedRecord(), hasSize(0));
        assertThat(discardedFilesReport.getDiscardedFromBrageRecord(),
                   hasSize(generator.getBrageRecord().getContentBundle().getContentFiles().size()));
    }

    @Test
    void shouldMergeIncomingPublicationWithExistingOneWhenSearchByIsbnReturnsSingleHit() throws IOException {
        final var isbn = randomIsbn13();
        var publication = randomPublication(NonFictionMonograph.class);
        publication.getEntityDescription().getReference()
            .setPublicationContext(new Book.BookBuilder().withIsbnList(List.of(isbn)).build());
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());
        mockSearchPublicationByIsbnResponse(existingPublication.getIdentifier(), isbn);
        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withIsbn(isbn)
                            .withType(TYPE_BOOK)
                            .withContributor(brageContributor)
                            .withPublicationDate(new PublicationDate("2023",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2023")
                                                                         .build()))
                            .withMainTitle(existingPublication.getEntityDescription().getMainTitle())
                            .build();

        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        var publicationFromBrage = handler.handleRequest(s3Event, CONTEXT);
        var storedMergeReportString =
            extractUpdateReportFromS3ByUpdateSource(s3Event, existingPublication, generator.getBrageRecord().getId(),
                                                    "ISBN");

        assertThat(storedMergeReportString, is(notNullValue()));
        assertThat(publicationFromBrage.publication().getIdentifier().toString(),
                   is(equalTo(existingPublication.getIdentifier().toString())));
    }

    @Test
    void shouldNotMergeIncomingPublicationWithExistingOneWhenMainTitleLevenshteinDistanceIsTooLarge()
        throws IOException {
        var publication = randomPublication(ConferencePoster.class);
        publication.setAdditionalIdentifiers(getDefaultIdentifiers(randomString()));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription()
            .setMainTitle("Sensitivity analysis of the dynamic response of floating wind turbines");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription().getReference()
                               .getPublicationInstance().getInstanceType();
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());

        mockSingleHitSearchApiResponse(existingPublication.getIdentifier(), 200);

        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withMainTitle("Dynamic Response of Floating Wind Turbines")
                            .withContributor(brageContributor)
                            .withType(new Type(List.of(), instanceType)).build();

        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);

        assertThrows(NoSuchKeyException.class, () -> extractUpdateReportFromS3ByUpdateSource(
            s3Event, existingPublication, generator.getBrageRecord().getId(), "SEARCH"));
    }

    @Description("Should merge incoming publication with existing one when they match. Publications match"
                 + "when levenshtein distance between titles is less than 10 and at least one contributor name"
                 + "has levenshtein distance less than 5 with another contributor")
    @Test
    void shouldMergeIncomingPublicationWithExistingOneWhenPublicationsMatch() throws IOException {
        var publication = randomPublication(ConferencePoster.class);
        publication.setAdditionalIdentifiers(getDefaultIdentifiers(randomString()));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription().getReference().getPublicationInstance()
                               .getInstanceType();
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());
        mockSingleHitSearchApiResponse(existingPublication.getIdentifier(), 200);

        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withMainTitle("Dynamic Response of Floating Wind Turbines")
                            .withContributor(brageContributor)
                            .withPublicationDate(new PublicationDate("2023",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2023")
                                                                         .build()))
                            .withType(new Type(List.of(), instanceType)).build();

        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        var updatedPublication = handler.handleRequest(s3Event, CONTEXT);

        assertDoesNotThrow(() -> extractUpdateReportFromS3ByUpdateSource(
            s3Event, existingPublication, generator.getBrageRecord().getId(), "SEARCH"));

        assertThat(updatedPublication.publication().getIdentifier().toString(),
                   is(equalTo(updatedPublication.publication().getIdentifier().toString())));
    }

    @Test
    void shouldNotMergeIncomingPublicationWithExistingOneWhenAtLeastOneContributorDoesNotMatch() throws IOException {
        var publication = randomPublication(ConferencePoster.class);
        publication.setAdditionalIdentifiers(getDefaultIdentifiers(randomString()));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setMainTitle("Dynamic - Response of Floating Wind Turbines! Report");
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription().getReference()
                               .getPublicationInstance().getInstanceType();

        mockSingleHitSearchApiResponse(existingPublication.getIdentifier(), 200);

        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withMainTitle("Dynamic Response of Floating Wind Turbines")
                            .withType(new Type(List.of(), instanceType)).build();

        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);

        assertThrows(NoSuchKeyException.class, () -> extractUpdateReportFromS3ByUpdateSource(
            s3Event, existingPublication, generator.getBrageRecord().getId(), "SEARCH"));
    }

    @Test
    void shouldKeepBrageContributorsIfNvaPublicationIsMissingContributors() throws IOException {
        var cirstinIdentifier = "1234";
        var publication = randomPublication(ConferencePoster.class);
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier(SOURCE_CRISTIN, cirstinIdentifier)));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setContributors(List.of());
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription().getReference().getPublicationInstance()
                               .getInstanceType();
        var affiliationIdentifier = randomString();
        var contributor = new Contributor(new Identity(randomString(), null, null),
                                          "Creator",
                                          "Creator",
                                          List.of(new Affiliation(affiliationIdentifier, "ntnu",
                                                                  null)));
        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withCristinIdentifier(cirstinIdentifier)
                            .withMainTitle(publication.getEntityDescription().getMainTitle())
                            .withContributor(contributor)
                            .withPublicationDate(new PublicationDate("2022",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2022")
                                                                         .build()))
                            .withType(new Type(List.of(), instanceType))
                            .build();
        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        var updatedPublication = handler.handleRequest(s3Event, CONTEXT);
        var organizationUri = UriWrapper.fromUri("https://test.nva.aws.unit.no/cristin/organization/"
                                                 + affiliationIdentifier);
        var expectedContributor = new no.unit.nva.model.Contributor.Builder()
                                      .withIdentity(new no.unit.nva.model.Identity.Builder()
                                                        .withName(contributor.getIdentity().getName())
                                                        .build())
                                      .withRole(new RoleType(Role.parse(contributor.getRole())))
                                      .withSequence(1)
                                      .withAffiliations(List.of(new Organization.Builder()
                                                                    .withId(organizationUri.getUri())
                                                                    .build()))
                                      .build();
        assertThat(updatedPublication.publication().getIdentifier(), is(equalTo(existingPublication.getIdentifier())));
        assertThat(updatedPublication.publication().getEntityDescription().getContributors(),
                   contains(expectedContributor));
    }

    @ParameterizedTest
    @MethodSource("emptyPublicationInstanceSupplier")
    void shouldUpdateExistingPublicationByFillingUpInstanceTypeEmptyValues(PublicationInstance<?> publicationInstance,
                                                                           Type type) throws IOException {
        var generator = generateBrageRecordAndPersistDuplicateByCristinIdentifier(publicationInstance,
                                                                                  type,
                                                                                  randomAdditionalIdentifier());
        var s3Event = createNewBrageRecordEvent(generator.getGeneratorBuilder().build().getBrageRecord());
        var updatedPublicationInstance = handler.handleRequest(s3Event, CONTEXT)
                                             .publication().getEntityDescription()
                                             .getReference().getPublicationInstance();

        assertThat(updatedPublicationInstance,
                   doesNotHaveEmptyValuesIgnoringFields(Set.of(".pages.introduction", ".pages.illustrated")));
    }

    @Test
    void shouldSetResourceEventWhenImportingBrageRecord() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_REPORT_WORKING_PAPER).build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var publication = handler.handleRequest(s3Event, CONTEXT).publication();

        var resource = Resource.resourceQueryObject(publication.getIdentifier())
                           .fetch(resourceService)
                           .orElseThrow();
        var resourceEvent = (ImportedResourceEvent) resource.getResourceEvent();

        assertEquals(Source.BRAGE, resourceEvent.importSource().getSource());
    }

    @Test
    void shouldAddImportDetailWhenUpdatingExistingPublicationByBrageRecord() throws IOException {
        var generator = generateBrageRecordAndPersistDuplicate(new Lecture(), TYPE_CONFERENCE_REPORT);
        var existingPublication = generator.getExistingPublication();
        var brageRecord = generator.getGeneratorBuilder().build().getBrageRecord();

        mockSingleHitSearchApiResponse(existingPublication.getIdentifier(), 200);
        var s3Event = createNewBrageRecordEvent(brageRecord);
        var publicationRepresentation = handler.handleRequest(s3Event, CONTEXT);

        var resource = Resource.resourceQueryObject(publicationRepresentation.publication().getIdentifier())
                           .fetch(resourceService)
                           .orElseThrow();
        var resourceEvent = (MergedResourceEvent) resource.getResourceEvent();

        assertEquals(Source.BRAGE, resourceEvent.importSource().getSource());
    }

    @DisplayName("Should not keep Cristin identifier from Brage when Cristin identifier is present in existing " +
                 "publication")
    @Test
    void shouldUpdateExistingPublicationWhenBragePublicationHasCristinIdentifier()
        throws IOException, nva.commons.apigateway.exceptions.NotFoundException {

        var publication = randomPublication(ConferencePoster.class);
        publication.setAdditionalIdentifiers(Set.of(new CristinIdentifier(SourceName.fromCristin("ntnu@1234"),
                                                                          randomString())));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setContributors(List.of());
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var instanceType = existingPublication.getEntityDescription().getReference().getPublicationInstance()
                               .getInstanceType();
        var affiliationIdentifier = randomString();
        var contributor = new Contributor(new Identity(randomString(), null, null),
                                          "Creator",
                                          "Creator",
                                          List.of(new Affiliation(affiliationIdentifier, NTNU_CUSTOMER_NAME,
                                                                  null)));

        mockSingleHitSearchApiResponse(existingPublication.getIdentifier(), 200);
        var brageCristinIdentifier = randomString();
        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withCristinIdentifier(brageCristinIdentifier)
                            .withMainTitle(publication.getEntityDescription().getMainTitle())
                            .withContributor(contributor)
                            .withPublicationDate(new PublicationDate("2022",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2022")
                                                                         .build()))
                            .withType(new Type(List.of(), instanceType))
                            .build();
        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(existingPublication.getIdentifier());
        var expectedCristinIdentifierFromBrage =
            new CristinIdentifier(SourceName.fromBrage(generator.getBrageRecord().getCustomer().getName()),
                                  brageCristinIdentifier);
        assertThat(updatedPublication.getAdditionalIdentifiers(),
                   not(hasItem(expectedCristinIdentifierFromBrage)));
    }

    private BrageTestRecord generateBrageRecordAndPersistDuplicateByCristinIdentifier(
        PublicationInstance<?> publicationInstance, Type type, AdditionalIdentifierBase additionalIdentifier) {
        var cristinIdentifier = "1234";
        var publication = randomPublication(publicationInstance.getClass());

        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier(SOURCE_CRISTIN, cristinIdentifier),
                                                    additionalIdentifier));
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setContributors(List.of());
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .withMonth("03")
                                                                  .withDay("01")
                                                                  .build());
        publication.getEntityDescription().getReference().setPublicationInstance(publicationInstance);
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var affiliationIdentifier = randomString();
        var contributor = new Contributor(new Identity(randomString(), null, null),
                                          "Creator",
                                          "Creator",
                                          List.of(new Affiliation(affiliationIdentifier, NTNU_CUSTOMER_NAME,
                                                                  null)));
        var generatorBuilder = new NvaBrageMigrationDataGenerator.Builder()
                                   .withCristinIdentifier(cristinIdentifier)
                                   .withMainTitle(publication.getEntityDescription().getMainTitle())
                                   .withContributor(contributor)
                                   .withPublicationDate(new PublicationDate("2022",
                                                                            new PublicationDateNva.Builder()
                                                                                .withYear("2022")
                                                                                .withMonth("03")
                                                                                .withDay("01")
                                                                                .build()))
                                   .withType(type)
                                   .withHasPart(List.of(randomString()))
                                   .withPages(new Pages(randomString(), new Range("1", "2"), randomString()));
        return new BrageTestRecord(generatorBuilder, existingPublication);
    }

    private BrageTestRecord generateBrageRecordAndPersistDuplicate(
        PublicationInstance<?> publicationInstance, Type type) {
        var publication = randomPublication(publicationInstance.getClass());
        publication.getEntityDescription().getReference().setDoi(null);
        publication.getEntityDescription().setContributors(List.of());
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .withMonth("03")
                                                                  .withDay("01")
                                                                  .build());
        publication.getEntityDescription().getReference().setPublicationInstance(publicationInstance);
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromSource(Source.CRISTIN));
        var affiliationIdentifier = randomString();
        var contributor = new Contributor(new Identity(randomString(), null, null),
                                          "Creator",
                                          "Creator",
                                          List.of(new Affiliation(affiliationIdentifier, NTNU_CUSTOMER_NAME,
                                                                  null)));
        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withMainTitle(publication.getEntityDescription().getMainTitle())
                            .withContributor(contributor)
                            .withPublicationDate(new PublicationDate("2022",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2022")
                                                                         .withMonth("03")
                                                                         .withDay("01")
                                                                         .build()))
                            .withType(type)
                            .withHasPart(List.of(randomString()))
                            .withPages(new Pages(randomString(), new Range("1", "2"), randomString()));
        return new BrageTestRecord(generator, existingPublication);
    }

    @Test
    void shouldSkipPostWhereThereExistNvaPostWithTheSameHandle() throws IOException {
        var isbn = randomIsbn13();
        var mainTitle = randomString();
        var handle = randomUri();
        var publication = randomPublication(NonFictionMonograph.class);
        publication.getEntityDescription().getReference()
            .setPublicationContext(new Book.BookBuilder().withIsbnList(List.of(isbn)).build());
        publication.getEntityDescription().setPublicationDate(new no.unit.nva.model.PublicationDate.Builder()
                                                                  .withYear("2022")
                                                                  .build());
        publication.getEntityDescription().setMainTitle(mainTitle);
        publication
            .setAdditionalIdentifiers(Set.of(new HandleIdentifier(SourceName.fromBrage("ntnu"),
                                                                  handle)));
        var existingPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var contributor = existingPublication.getEntityDescription().getContributors().getFirst();
        var brageContributor = new Contributor(new Identity(contributor.getIdentity().getName(), null, null),
                                               "ARTIST", null, List.of());
        var generator = new NvaBrageMigrationDataGenerator.Builder()
                            .withIsbn(isbn)
                            .withType(TYPE_BOOK)
                            .withHandle(handle)
                            .withContributor(brageContributor)
                            .withPublicationDate(new PublicationDate("2023",
                                                                     new PublicationDateNva.Builder()
                                                                         .withYear("2023")
                                                                         .build()))
                            .withMainTitle(mainTitle)
                            .build();

        var s3Event = createNewBrageRecordEvent(generator.getBrageRecord());
        mockSearchPublicationByIsbnResponse(existingPublication.getIdentifier(), isbn);
        handler.handleRequest(s3Event, CONTEXT);
        var errorReport = extractActualReportFromS3Client(s3Event, HandleDuplicateException.class.getSimpleName(),
                                                          generator.getBrageRecord());
        var exception = errorReport.get("exception").asText();
        assertThat(exception, containsString("Publication with handle"));
    }

    private static String getIsbn(Publication existingPublication) {
        return ((Book) existingPublication.getEntityDescription()
                           .getReference()
                           .getPublicationContext()).getIsbnList().getFirst();
    }

    @Test
    void shouldMergeConferenceReportWithEvent() throws IOException {
        var generator = generateBrageRecordAndPersistDuplicate(new Lecture(), TYPE_CONFERENCE_REPORT);
        var existingPublication = generator.getExistingPublication();

        mockSingleHitSearchApiResponse(existingPublication.getIdentifier(), 200);
        var s3Event = createNewBrageRecordEvent(generator.getGeneratorBuilder().build().getBrageRecord());
        var publicationRepresentation = handler.handleRequest(s3Event, CONTEXT);

        assertThat(publicationRepresentation.publication().getIdentifier(),
                   is(equalTo(existingPublication.getIdentifier())));
    }

    @Test
    void shouldPersistNewMergeReportWhenUpdatingPublicationForTheFirstTime()
        throws IOException {
        var generator = generateBrageRecordAndPersistDuplicate(new Lecture(), TYPE_CONFERENCE_REPORT);
        var existingPublication = generator.getExistingPublication();

        mockSingleHitSearchApiResponse(existingPublication.getIdentifier(), 200);
        var brageRecord = generator.getGeneratorBuilder().build().getBrageRecord();
        var s3Event = createNewBrageRecordEvent(brageRecord);
        var publicationRepresentation = handler.handleRequest(s3Event, CONTEXT);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        var uri = UriWrapper.fromUri("PUBLICATION_UPDATE")
                      .addChild(publicationRepresentation.publication().getIdentifier().toString())
                      .toS3bucketPath();
        var content = s3Driver.getFile(uri);
        var mergeReport = JsonUtils.dtoObjectMapper.readValue(content, PublicationMergeReport.class);
        var result = mergeReport.mergeReport().get(brageRecord.getCustomer().getName());

        assertThat(result.newImage(), is(equalTo(publicationRepresentation.publication())));
        assertThat(result.oldImage(), is(equalTo(existingPublication)));
        assertThat(result.institutionImage(), is(notNullValue()));
    }

    @Test
    void shouldUpdateExistingMergeReportWhenUpdatingPublication() throws IOException {
        var generator = generateBrageRecordAndPersistDuplicate(new Lecture(), TYPE_CONFERENCE_REPORT);
        var existingPublication = generator.getExistingPublication();

        mockSingleHitSearchApiResponse(existingPublication.getIdentifier(), 200);
        var brageRecord = generator.getGeneratorBuilder().build().getBrageRecord();
        var s3Event = createNewBrageRecordEvent(brageRecord);
        handler.handleRequest(s3Event, CONTEXT);
        var newBrageRecord = brageRecord;
        newBrageRecord.setCustomer(new Customer(NTNU_CUSTOMER_NAME, null));
        newBrageRecord.setId(randomUri());
        var news3Event = createNewBrageRecordEvent(brageRecord);
        var publicationRepresentation = handler.handleRequest(news3Event, CONTEXT);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        var uri = UriWrapper.fromUri("PUBLICATION_UPDATE")
                      .addChild(publicationRepresentation.publication().getIdentifier().toString())
                      .toS3bucketPath();
        var content = s3Driver.getFile(uri);
        var mergeReport = JsonUtils.dtoObjectMapper.readValue(content, PublicationMergeReport.class);

        var firstMergeResult = mergeReport.mergeReport().get(brageRecord.getCustomer().getName());
        var secondMergeResult = mergeReport.mergeReport().get(newBrageRecord.getCustomer().getName());

        assertThat(firstMergeResult, is(notNullValue()));
        assertThat(secondMergeResult, is(notNullValue()));
    }

    @Test
    void whenSearchApiReturnsMultiplePublicationsButDynamoDbCannotFindTheFirstOneMergingShouldHappenWithNextInLine()
        throws IOException {
        // create a brage record and matching publication

        var generator = generateBrageRecordAndPersistDuplicate(new Lecture(), TYPE_CONFERENCE_REPORT);
        var existingPublicationIdentifier = generator.getExistingPublication().getIdentifier();

        //mock search response with first result being something that does not exist in database:
        mockMultipleHitSearchApiResponse(List.of(SortableIdentifier.next(), existingPublicationIdentifier));

        var s3Event = createNewBrageRecordEvent(generator.getGeneratorBuilder().build().getBrageRecord());
        var updatedPublication = handler.handleRequest(s3Event, CONTEXT);

        //assert that we have not created a new publication, but instead updated the existing one:
        assertThat(updatedPublication.publication().getIdentifier(), is(equalTo(existingPublicationIdentifier)));
    }

    @Test
    void shouldImportPublicationMissingMandatoryValuesAndPersistReport() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_REPORT_WORKING_PAPER).build();
        brageGenerator.getBrageRecord().getEntityDescription().setMainTitle(null);
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);

        var s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        var uri = UriWrapper.fromUri("ERROR_REPORT")
                      .addChild(brageGenerator.getBrageRecord().getCustomer().getName())
                      .addChild(MissingFieldsError.name())
                      .addChild(brageGenerator.getBrageRecord().getId().getPath())
                      .toS3bucketPath();
        var content = s3Driver.getFile(uri);

        assertThat(content, containsString("Empty fields found: entityDescription.mainTitle"));
    }

    @Test
    void importedFileShouldHaveFileImportedEvent() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_TEXTBOOK)
                                 .withResourceContent(createResourceContent())
                                 .withAssociatedArtifacts(createCorrespondingAssociatedArtifactWithLegalNote(null))
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var publicationRepresentation = handler.handleRequest(s3Event, CONTEXT);

        var resource = Resource.fromPublication(publicationRepresentation.publication())
                           .fetch(resourceService)
                           .orElseThrow();

        var fileEvent = resource.getFileEntries().getFirst().getFileEvent();

        assertInstanceOf(FileImportedEvent.class, fileEvent);
    }

    @Test
    void importedFileOnPublicationUpdateShouldHaveFileImportedEvent() throws IOException {
        var generator = generateBrageRecordAndPersistDuplicate(new Lecture(), TYPE_CONFERENCE_REPORT);
        var existingPublication = generator.getExistingPublication();
        ;
        var contentFile = new ContentFile("dublin_core.xml", BundleType.IGNORED, null,
                                          randomUUID(), new License(randomString(), new NvaLicense(randomUri())), null);
        var brageRecord = generator.getGeneratorBuilder()
                              .withResourceContent(new ResourceContent(List.of(contentFile)))
                              .build().getBrageRecord();

        mockSingleHitSearchApiResponse(existingPublication.getIdentifier(), 200);
        var s3Event = createNewBrageRecordEvent(brageRecord);
        var publicationRepresentation = handler.handleRequest(s3Event, CONTEXT);

        var resource = Resource.resourceQueryObject(publicationRepresentation.publication().getIdentifier())
                           .fetch(resourceService)
                           .orElseThrow();
        var fileEntry =
            resource.getFileEntry(new SortableIdentifier(contentFile.getIdentifier().toString())).orElseThrow();

        assertInstanceOf(FileImportedEvent.class, fileEntry.getFileEvent());
    }

    private static AdditionalIdentifier cristinAdditionalIdentifier(String cristinIdentifier) {
        return new AdditionalIdentifier("Cristin", cristinIdentifier);
    }

    private static Publication copyPublication(NvaBrageMigrationDataGenerator brageGenerator)
        throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(
            JsonUtils.dtoObjectMapper.writeValueAsString(brageGenerator.getNvaPublication()), Publication.class);
    }

    private static void putAssociatedArtifactsToResourceStorage(NvaBrageMigrationDataGenerator dataGenerator,
                                                                S3Driver s3Driver, java.io.File file) {
        dataGenerator.getNvaPublication()
            .getAssociatedArtifacts()
            .stream()
            .filter(artifact -> artifact instanceof File)
            .map(artifact -> (File) artifact)
            .map(File::getIdentifier)
            .forEach(id -> {
                try {
                    s3Driver.insertFile(UnixPath.of(id.toString()), new FileInputStream(file));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    private String getPartOfReport(S3Event s3Event, NvaBrageMigrationDataGenerator brageGenerator,
                                   Publication actualPublication) {
        var timestamp = s3Event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        var uri = UriWrapper.fromUri("PART_OF")
                      .addChild(NTNU_CUSTOMER_NAME)
                      .addChild(timestamp)
                      .addChild(brageGenerator.getBrageRecord().getId().getPath())
                      .addChild(String.valueOf(actualPublication.getIdentifier()));
        S3Driver s3Driver = new S3Driver(s3Client,
                                         new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        return s3Driver.getFile(uri.toS3bucketPath());
    }

    private void mockSingleHitSearchApiResponse(SortableIdentifier identifier, int statusCode) {
        var publicationId = UriWrapper.fromHost(API_HOST)
                                .addChild("publication")
                                .addChild(identifier.toString())
                                .getUri();
        var searchResourceApiResponse = new SearchResourceApiResponse(1, List.of(new ResourceWithId(publicationId)));
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(searchResourceApiResponse.toString());
        when(this.uriRetriever.fetchResponse(any(), any())).thenReturn(Optional.of(response));
    }

    private void mockSingleHitSearchApiResponseByTitleAndTypes(SortableIdentifier identifier, int statusCode,
                                                               String title, String... types) {
        var publicationId = UriWrapper.fromHost(API_HOST)
                                .addChild("publication")
                                .addChild(identifier.toString())
                                .getUri();
        var searchResourceApiResponse = new SearchResourceApiResponse(1, List.of(new ResourceWithId(publicationId)));
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(searchResourceApiResponse.toString());
        var uriWrapper = UriWrapper.fromHost(API_HOST)
                      .addChild("search")
                      .addChild("resources")
                      .addQueryParameter("titleShould", title);
        for (String type : types) {
            uriWrapper.addQueryParameter("contextType", type);
        }

        when(this.uriRetriever.fetchResponse(uriWrapper.getUri(), "application/json")).thenReturn(Optional.of(response));
    }

    private void mockMultipleHitSearchApiResponse(List<SortableIdentifier> identifiers) {
        var resourceWithIds = identifiers.stream()
                                  .map(identifier -> UriWrapper.fromHost(API_HOST)
                                                         .addChild("publication")
                                                         .addChild(identifier.toString())
                                                         .getUri())
                                  .map(ResourceWithId::new)
                                  .toList();
        var searchResourceApiResponse = new SearchResourceApiResponse(resourceWithIds.size(), resourceWithIds);
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.body()).thenReturn(searchResourceApiResponse.toString());
        when(this.uriRetriever.fetchResponse(any(), any())).thenReturn(Optional.of(response));
    }

    private void mockSearchPublicationByIsbnResponse(SortableIdentifier identifier, String isbn) {
        var publicationId = UriWrapper.fromHost(API_HOST)
                                .addChild("publication")
                                .addChild(identifier.toString())
                                .getUri();
        var searchUri = UriWrapper.fromHost(API_HOST)
                            .addChild("search")
                            .addChild("resources")
                            .addQueryParameter("isbn", isbn)
                            .addQueryParameter("aggregation", "none")
                            .getUri();
        var searchResourceApiResponse = new SearchResourceApiResponse(1, List.of(new ResourceWithId(publicationId)));
        when(uriRetriever.getRawContent(eq(searchUri), eq("application/json")))
            .thenReturn(Optional.ofNullable(searchResourceApiResponse.toString()));
    }

    private DiscardedFilesReport extractDiscardedFilesReportFromS3(Record brageRecord, S3Event s3Event,
                                                                   Publication cristinPublication)
        throws JsonProcessingException {
        var errorFileUri = constructDiscardedFileUri(s3Event, brageRecord, cristinPublication);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        var content = s3Driver.getFile(errorFileUri.toS3bucketPath());
        return JsonUtils.dtoObjectMapper.readValue(content, DiscardedFilesReport.class);
    }

    private UriWrapper constructDiscardedFileUri(S3Event s3Event, Record brageRecord, Publication cristinPublication) {

        var timestamp = s3Event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        return UriWrapper.fromUri("DISCARDED_CONTENT_FILES")
                   .addChild(NTNU_CUSTOMER_NAME)
                   .addChild(timestamp)
                   .addChild(brageRecord.getId().getPath())
                   .addChild(String.valueOf(cristinPublication.getIdentifier()));
    }

    private AssociatedArtifact randomOpenFile() {

        return new OpenFile(randomUUID(), randomString(), "application/pdf", 10L, null,
                            PublisherVersion.PUBLISHED_VERSION, null, NullRightsRetentionStrategy.create(
            RightsRetentionStrategyConfiguration.UNKNOWN), null, Instant.now(),
                            new ImportUploadDetails(null, null, null));
    }

    private Record createMinimalRecord(String cristinIdentifier) {
        var minimalRecord = new Record();
        var fakeDummyHandle = UriWrapper.fromUri(DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS
                                                 + "/1").getUri();
        minimalRecord.setId(fakeDummyHandle);
        minimalRecord.setPublisherAuthority(new PublisherAuthority(List.of(),
                                                                   PublisherVersion.PUBLISHED_VERSION));
        minimalRecord.setCustomer(new Customer("unis", null));
        minimalRecord.setCristinId(cristinIdentifier);
        minimalRecord.setEntityDescription(new EntityDescription());
        minimalRecord.setType(new Type(List.of(), CRISTIN_RECORD.getValue()));
        return minimalRecord;
    }

    private ContentFile createContentFile() {
        var contentFile = new ContentFile();
        contentFile.setFilename("MyAwsomeUnisFile.pdf");
        contentFile.setBundleType(BundleType.ORIGINAL);
        contentFile.setIdentifier(randomUUID());
        contentFile.setLicense(new License("", new NvaLicense(randomUri())));
        return contentFile;
    }

    private String extractUpdateReportFromS3ByUpdateSource(S3Event s3Event,
                                                           Publication publication,
                                                           URI brageHandle, String updateSource) {
        var timestamp = s3Event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        var uri = UriWrapper.fromUri(UPDATE_REPORTS_PATH)
                      .addChild(updateSource)
                      .addChild(NTNU_CUSTOMER_NAME)
                      .addChild(timestamp)
                      .addChild(brageHandle.getPath())
                      .addChild(String.valueOf(publication.getIdentifier()));
        S3Driver s3Driver = new S3Driver(s3Client,
                                         new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        return s3Driver.getFile(uri.toS3bucketPath());
    }

    private JsonNode extractActualReportFromS3ClientForInvalidRecord(S3Event s3Event, String simpleName)
        throws JsonProcessingException {
        var errorFileUri = constructErrorFileUriForInvalidBrageRecord(s3Event, simpleName);
        var s3Driver = new S3Driver(s3Client,
                                    new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        var content = s3Driver.getFile(errorFileUri.toS3bucketPath());
        return JsonUtils.dtoObjectMapper.readTree(content);
    }

    private void assertThatPublicationsMatch(Publication actualPublication, Publication expectedPublication) {
        var ignoredFields = new String[]{"createdDate", "identifier", "modifiedDate", "publishedDate", "subjects",
            "associatedArtifacts", "fundings", "status", "importDetails"};
        assertThat(actualPublication, is(samePropertyValuesAs(expectedPublication, ignoredFields)));
        assertThat(actualPublication.getAssociatedArtifacts(),
                   hasSize(expectedPublication.getAssociatedArtifacts().size()));
        actualPublication.getAssociatedArtifacts().stream().filter(File.class::isInstance)
            .forEach(associatedArtifact -> {
                var file = (File) associatedArtifact;
                assertThat(((ImportUploadDetails) file.getUploadDetails()).archive(), is(equalTo("NTNU")));
            });
    }

    private List<SortableIdentifier> persistMultiplePublicationWithSameCristinId(Record record) {
        return IntStream.range(0, 5)
                   .boxed()
                   .map(i -> publicationThatMatchesRecord(record))
                   .map(publication ->
                            resourceService.createPublicationFromImportedEntry(publication,
                                                                               ImportSource.fromSource(Source.CRISTIN)))
                   .map(Publication::getIdentifier).toList();
    }

    private Publication publicationThatMatchesRecord(Record record) {
        var publication = randomPublication(NonFictionMonograph.class);
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Cristin", record.getCristinId())));
        publication.getEntityDescription().setPublicationDate(
            new no.unit.nva.model.PublicationDate.Builder()
                .withYear(record.getEntityDescription().getPublicationDate().getNva().getYear())
                .build());
        publication.getEntityDescription().setMainTitle(record.getEntityDescription().getMainTitle());
        var brageContributor = record.getEntityDescription().getContributors().getFirst();
        var nvaContributor = new no.unit.nva.model.Contributor(
            new no.unit.nva.model.Identity.Builder().withName(brageContributor.getIdentity().getName()).build(),
            List.of(), null, null, false);
        publication.getEntityDescription().setContributors(List.of(nvaContributor));
        return publication;
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForJournalArticleWithoutId() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_JOURNAL_ARTICLE)
                   .withSpatialCoverage(List.of("Norway"))
                   .withJournalTitle("Some Very Popular Journal")
                   .withIssn(List.of(randomIssn(), randomIssn()))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForJournalArticleWithUnconfirmedJournal() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_JOURNAL_ARTICLE)
                   .withSpatialCoverage(List.of("Norway"))
                   .withJournalTitle("Some Very Popular Journal")
                   .withIssn(List.of(randomIssn()))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForJournalArticle() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_JOURNAL_ARTICLE)
                   .withJournalId("someId")
                   .withSpatialCoverage(List.of("Norway"))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForDataset() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_DATASET)
                   .withPublisherId("someId")
                   .withPublicationDate(new PublicationDate("03-08", new PublicationDateNva.Builder().withYear(null)
                                                                         .withDay("03")
                                                                         .withMonth("08")
                                                                         .build()))
                   .withPublicationDateForPublication(new no.unit.nva.model.PublicationDate.Builder().withYear(null)
                                                          .withDay("03")
                                                          .withMonth("08")
                                                          .build())
                   .withSpatialCoverage(List.of("Norway"))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForMaster() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MASTER)
                   .withPublicationDate(PUBLICATION_DATE)
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForBachelor() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_BACHELOR)
                   .withPublicationDate(PUBLICATION_DATE)
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorObjectWithCristinId() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_BACHELOR)
                   .withPublicationDate(PUBLICATION_DATE)
                   .withCristinIdentifier(HARD_CODED_CRISTIN_IDENTIFIER)
                   .withType(TYPE_BOOK)
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForResearchReport() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_RESEARCH_REPORT).build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForReport() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_REPORT)
                   .withSeries("someSeries;42")
                   .withIssn(List.of(randomIssn()))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForReportWithUnconfirmedSeries() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_REPORT)
                   .withSeriesNumberRecord(new PartOfSeries("seriesTitle", "42"))
                   .withSeriesNumberPublication("42")
                   .withSeriesTitle("seriesTitle")
                   .withIssn(List.of(randomIssn(), randomIssn()))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForMap() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MAP).withPublisherId("someId").build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForBookWithoutValidSeriesNumber() {
        var seriesTitle = randomString();
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_BOOK)
                   .withSeriesTitle(seriesTitle)
                   .withSeriesNumberRecord(new PartOfSeries(seriesTitle, null))
                   .withSeriesNumberPublication(null)
                   .withPublicationDate(PUBLICATION_DATE)
                   .withIsbn(randomIsbn10())
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForBook() {
        var seriesNumber = randomString();
        var seriesTitle = randomString();
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_BOOK)
                   .withSeriesTitle(seriesTitle)
                   .withSeriesNumberRecord(new PartOfSeries(seriesTitle, seriesNumber))
                   .withSeriesNumberPublication(seriesNumber)
                   .withPublicationDate(PUBLICATION_DATE)
                   .withIsbn(randomIsbn10())
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForRecord() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_BOOK)
                   .withIsbn(randomIsbn10())
                   .withResourceContent(createResourceContent())
                   .withAssociatedArtifacts(createCorrespondingAssociatedArtifactWithLegalNote(null))
                   .withOrganization(TEST_ORGANIZATION)
                   .build();
    }

    private String extractUpdatedPublicationsHandleReportFromS3Client(S3Event s3Event,
                                                                      Publication actualPublication,
                                                                      URI brageHandle) {
        return extractHandleReportFromS3Client(s3Event, actualPublication, UPDATED_PUBLICATIONS_REPORTS_PATH,
                                               brageHandle);
    }

    private String extractHandleReportFromS3Client(S3Event s3Event, Publication actualPublication, URI brageHandle) {
        return extractHandleReportFromS3Client(s3Event, actualPublication, HANDLE_REPORTS_PATH, brageHandle);
    }

    private String extractHandleReportFromS3Client(S3Event s3Event, Publication actualPublication,
                                                   String destinationFolder,
                                                   URI brageHandle) {
        UriWrapper handleReport = constructHandleReportFileUri(s3Event, actualPublication, destinationFolder,
                                                               brageHandle);
        S3Driver s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        return s3Driver.getFile(handleReport.toS3bucketPath());
    }

    private UriWrapper constructHandleReportFileUri(S3Event s3Event, Publication actualPublication,
                                                    String destinationFolder,
                                                    URI brageHandle) {
        var timestamp = s3Event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        return UriWrapper.fromUri(destinationFolder)
                   .addChild(NTNU_CUSTOMER_NAME)
                   .addChild(timestamp)
                   .addChild(brageHandle.getPath())
                   .addChild(actualPublication.getIdentifier().toString());
    }

    private JsonNode extractActualReportFromS3Client(S3Event s3Event, String exceptionSimpleName, Record brageRecord)
        throws JsonProcessingException {
        var errorFileUri = constructErrorFileUri(s3Event, exceptionSimpleName, brageRecord);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        var content = s3Driver.getFile(errorFileUri.toS3bucketPath());
        return JsonUtils.dtoObjectMapper.readTree(content);
    }

    private String extractWarnReportFromS3Client(Record record) {
        var warningFileUri = constructWarnFileUri(record);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        return s3Driver.getFile(warningFileUri.toS3bucketPath());
    }

    private UriWrapper constructWarnFileUri(Record record) {
        return UriWrapper.fromUri("DUPLICATES_DETECTED")
                   .addChild(record.getCustomer().getName())
                   .addChild(DuplicateDetectionCause.CRISTIN_DUPLICATES.getValue())
                   .addChild(UriWrapper.fromUri(record.getId()).getLastPathElement());
    }

    private UriWrapper constructErrorFileUri(S3Event event, String exceptionSimpleName, Record brageRecord) {
        var fileUri = UriWrapper.fromUri(extractFilename(event));
        var timestamp = event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        return UriWrapper.fromUri(ERROR_BUCKET_PATH)
                   .addChild(brageRecord.getCustomer().getName())
                   .addChild(timestamp)
                   .addChild(exceptionSimpleName)
                   .addChild(fileUri.getLastPathElement());
    }

    private UriWrapper constructErrorFileUriForInvalidBrageRecord(S3Event event, String exceptionSimpleName) {
        var fileUri = UriWrapper.fromUri(extractFilename(event));
        var timestamp = event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        return UriWrapper.fromUri(ERROR_BUCKET_PATH)
                   .addChild(NTNU_CUSTOMER_NAME)
                   .addChild(timestamp)
                   .addChild(exceptionSimpleName)
                   .addChild(fileUri.getLastPathElement());
    }

    private String extractFilename(S3Event event) {
        return event.getRecords().getFirst().getS3().getObject().getKey();
    }

    private ResourceContent createResourceContent() {
        var file = new ContentFile(FILENAME,
                                   BundleType.ORIGINAL,
                                   "description",
                                   UUID,
                                   new License("someLicense", new NvaLicense(
                                       URI.create("https://creativecommons.org/licenses/by-nc/4.0"))),
                                   EMBARGO_DATE);

        return new ResourceContent(Collections.singletonList(file));
    }

    private List<AssociatedArtifact> createCorrespondingAssociatedArtifactWithLegalNote(String legalNote) {
        return List.of(File.builder()
                           .withIdentifier(UUID)
                           .withLicense(LICENSE_URI)
                           .withName(FILENAME)
                           .withPublisherVersion(null)
                           .withSize(ExtendedFakeS3Client.SOME_CONTENT_LENGTH)
                           .withMimeType(ExtendedFakeS3Client.APPLICATION_PDF_MIMETYPE)
                           .withEmbargoDate(EMBARGO_DATE)
                           .withLegalNote(legalNote)
                           .withUploadDetails(new ImportUploadDetails(ImportUploadDetails.Source.BRAGE, "ntnu", null))
                           .buildOpenFile());
    }

    private S3Event createNewInvalidBrageRecordEvent() throws IOException {
        var record = new Record();
        record.setCustomer(new Customer(randomString(), null));
        var uri = s3Driver.insertFile(UnixPath.of(NTNU_CUSTOMER_NAME + "/" + randomString()), record.toString());
        return createS3Event(uri);
    }

    private S3Event createNewBrageRecordEvent(Record brageRecord) throws IOException {
        var recordAsJson = JsonUtils.dtoObjectMapper.writeValueAsString(brageRecord);
        var uri = s3Driver.insertFile(UnixPath.of(NTNU_CUSTOMER_NAME + "/" + randomString()), recordAsJson);
        return createS3Event(uri);
    }

    private S3Event createNewBrageRecordEventForCustomer(Record brageRecord, String customer) throws IOException {
        var recordAsJson = JsonUtils.dtoObjectMapper.writeValueAsString(brageRecord);
        var uri = s3Driver.insertFile(UnixPath.of(customer, randomString()), recordAsJson);
        return createS3Event(uri);
    }

    private S3Event createNewBrageRecordEventWithSpecifiedObjectKey(Record brageRecord) throws IOException {
        var recordAsJson = JsonUtils.dtoObjectMapper.writeValueAsString(brageRecord);
        var uri = s3Driver.insertFile(UnixPath.of("my/path/some.json"), recordAsJson);
        return createS3Event(uri);
    }

    private S3Event createS3Event(URI uri) {
        return createS3Event(UriWrapper.fromUri(uri).toS3bucketPath().toString());
    }

    private S3Event createS3Event(String expectedObjectKey) {
        var eventNotification = new S3EventNotificationRecord(randomString(), randomString(), randomString(),
                                                              randomDate(), randomString(), EMPTY_REQUEST_PARAMETERS,
                                                              EMPTY_RESPONSE_ELEMENTS,
                                                              createS3Entity(expectedObjectKey), EMPTY_USER_IDENTITY);
        return new S3Event(List.of(eventNotification));
    }

    private String randomDate() {
        return Instant.now().toString();
    }

    private S3Entity createS3Entity(String expectedObjectKey) {
        var bucket = new S3BucketEntity(INPUT_BUCKET_NAME, EMPTY_USER_IDENTITY, randomString());
        var object = new S3ObjectEntity(expectedObjectKey, SOME_FILE_SIZE, randomString(), randomString(),
                                        randomString());
        var schemaVersion = randomString();
        return new S3Entity(randomString(), bucket, object, schemaVersion);
    }
}


