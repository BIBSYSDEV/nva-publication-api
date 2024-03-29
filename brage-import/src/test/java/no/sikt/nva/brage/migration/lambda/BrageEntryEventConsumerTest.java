package no.sikt.nva.brage.migration.lambda;

import static no.sikt.nva.brage.migration.NvaType.ANTHOLOGY;
import static no.sikt.nva.brage.migration.NvaType.CRISTIN_RECORD;
import static no.sikt.nva.brage.migration.NvaType.EDITORIAL;
import static no.sikt.nva.brage.migration.NvaType.EXHIBITION_CATALOGUE;
import static no.sikt.nva.brage.migration.NvaType.FILM;
import static no.sikt.nva.brage.migration.NvaType.LITERARY_ARTS;
import static no.sikt.nva.brage.migration.NvaType.PERFORMING_ARTS;
import static no.sikt.nva.brage.migration.NvaType.POPULAR_SCIENCE_MONOGRAPH;
import static no.sikt.nva.brage.migration.NvaType.PROFESSIONAL_ARTICLE;
import static no.sikt.nva.brage.migration.NvaType.READER_OPINION;
import static no.sikt.nva.brage.migration.NvaType.TEXTBOOK;
import static no.sikt.nva.brage.migration.NvaType.VISUAL_ARTS;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.CRISTIN_RECORD_EXCEPTION;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.DUPLICATE_PUBLICATIONS_MESSAGE;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.ERROR_BUCKET_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.HANDLE_REPORTS_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.UPDATED_PUBLICATIONS_REPORTS_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.UPDATE_REPORTS_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.YYYY_MM_DD_HH_FORMAT;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.NOT_SUPPORTED_TYPE;
import static no.sikt.nva.brage.migration.merger.AssociatedArtifactMover.COULD_NOT_COPY_ASSOCIATED_ARTEFACT_EXCEPTION_MESSAGE;
import static no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger.DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.TEST_DESCRIPTION;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn10;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.any;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.mapper.InvalidIsmnRuntimeException;
import no.sikt.nva.brage.migration.merger.AssociatedArtifactException;
import no.sikt.nva.brage.migration.merger.BrageMergingReport;
import no.sikt.nva.brage.migration.merger.DiscardedFilesReport;
import no.sikt.nva.brage.migration.merger.DuplicatePublicationException;
import no.sikt.nva.brage.migration.merger.UnmappableCristinRecordException;
import no.sikt.nva.brage.migration.record.EntityDescription;
import no.sikt.nva.brage.migration.record.PublicationDate;
import no.sikt.nva.brage.migration.record.PublicationDateNva;
import no.sikt.nva.brage.migration.record.PublisherAuthority;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.ResourceOwner;
import no.sikt.nva.brage.migration.record.Type;
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
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.UnconfirmedCourse;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.artistic.music.InvalidIsmnException;
import no.unit.nva.model.instancetypes.artistic.music.Ismn;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.music.MusicScore;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;

public class BrageEntryEventConsumerTest extends ResourcesLocalTest {

    public static final String PART_OF_SERIES_VALUE_V1 = "SOMESERIES;42";
    public static final String PART_OF_SERIES_VALUE_V2 = "SOMESERIES;42:2022";
    public static final String PART_OF_SERIES_VALUE_V3 = "SOMESERIES;2022:42";
    public static final String PART_OF_SERIES_VALUE_V4 = "SOMESERIES;2022/42";
    public static final String PART_OF_SERIES_VALUE_V5 = "SOMESERIES;42/2022";
    public static final String PART_OF_SERIES_VALUE_V6 = "NVE Rapport;";
    public static final String EXPECTED_SERIES_NUMBER = "42";
    public static final UUID UUID = java.util.UUID.randomUUID();
    public static final Context CONTEXT = null;
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
    public static final Type TYPE_MUSIC = new Type(List.of(NvaType.RECORDING_MUSICAL.getValue()),
                                                   NvaType.RECORDING_MUSICAL.getValue());
    public static final Type TYPE_DESIGN_PRODUCT = new Type(List.of(NvaType.DESIGN_PRODUCT.getValue()),
                                                            NvaType.DESIGN_PRODUCT.getValue());
    public static final Type TYPE_PLAN_OR_BLUEPRINT = new Type(List.of(NvaType.PLAN_OR_BLUEPRINT.getValue()),
                                                               NvaType.PLAN_OR_BLUEPRINT.getValue());
    public static final Type TYPE_MAP = new Type(List.of(NvaType.MAP.getValue()), NvaType.MAP.getValue());
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
    private final String persistedStorageBucket = new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");
    private BrageEntryEventConsumer handler;
    private S3Driver s3Driver;
    private FakeS3Client s3Client;
    private ResourceService resourceService;
    private UriRetriever uriRetriever;

    @BeforeEach
    public void init() {
        super.init();
        this.resourceService = getResourceServiceBuilder(client).build();
        this.s3Client = new ExtendedFakeS3Client();
        this.s3Driver = new S3Driver(s3Client, INPUT_BUCKET_NAME);
        this.uriRetriever = mock(UriRetriever.class);
        this.handler = new BrageEntryEventConsumer(s3Client, resourceService, uriRetriever);
    }

    @Test
    void shouldConvertBrageRecordToNvaPublicationWithCorrectCustomer() throws IOException {
        var brageGenerator = buildGeneratorForRecord();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, brageGenerator.getNvaPublication());
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
        resourceService.createPublicationFromImportedEntry(cristinPublication);
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication.getEntityDescription().getDescription(),
                   is(equalTo(brageGenerator.getNvaPublication().getEntityDescription().getDescription())));
        assertThat(actualPublication.getEntityDescription().getAbstract(),
                   is(equalTo(brageGenerator.getNvaPublication().getEntityDescription().getAbstract())));
        assertThat(actualPublication.getHandle(), is(equalTo(brageGenerator.getNvaPublication().getHandle())));
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
        resourceService.createPublicationFromImportedEntry(cristinPublication);
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication.getEntityDescription().getDescription(), is(equalTo(cristinDescription)));
        assertThat(actualPublication.getEntityDescription().getAbstract(), is(equalTo(cristinAbstract)));
    }

    @Test
    void shouldCreateNewPublicationWhenPublicationHasCristinIdWhichIsNotPresentInNva()
        throws IOException, NotFoundException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_REPORT_WORKING_PAPER)
                                 .withCristinIdentifier("123456")
                                 .withResourceContent(createResourceContent())
                                 .withAssociatedArtifacts(createCorrespondingAssociatedArtifactWithLegalNote(null))
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldCreateNewPublicationWhenPublicationHasNoCristinId() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_REPORT_WORKING_PAPER).build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, brageGenerator.getNvaPublication());
    }

    @Test
    void shouldPersistExceptionWhenThereIsMultipleSearchResultsOnCristinId() throws IOException {
        var record = buildGeneratorObjectWithCristinId().getBrageRecord();
        var s3Event = createNewBrageRecordEvent(record);
        persistMultiplePublicationWithSameCristinId();
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(nullValue()));
        var actualErrorReport =
            extractActualReportFromS3Client(s3Event,
                                            DuplicatePublicationException.class.getSimpleName(),
                                            record);
        var exception = actualErrorReport.get("exception").asText();
        assertThat(exception, containsString(DUPLICATE_PUBLICATIONS_MESSAGE));
    }

    @ParameterizedTest(name = "shouldConvertBookToNvaPublication")
    @ValueSource(strings = {PART_OF_SERIES_VALUE_V1, PART_OF_SERIES_VALUE_V2, PART_OF_SERIES_VALUE_V3,
        PART_OF_SERIES_VALUE_V4, PART_OF_SERIES_VALUE_V5})
    void shouldConvertBookToNvaPublication(String seriesNumber) throws IOException {
        var brageGenerator = buildGeneratorForBook(seriesNumber);
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldNotConvertSeriesNumberWithoutNumber() throws IOException {
        var brageGenerator = buildGeneratorForBookWithoutValidSeriesNumber();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertMapToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForMap();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertReportToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForReport();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertReportWithUnconfirmedSeriesToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForReportWithUnconfirmedSeries();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertResearchReportToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForResearchReport();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertReportWorkingPaperToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_REPORT_WORKING_PAPER).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertBachelorToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForBachelor();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertMasterToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForMaster();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertPhdToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_PHD)
                                 .withPublicationDate(PUBLICATION_DATE)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertPhdWithPartsToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_PHD)
                                 .withPublicationDate(PUBLICATION_DATE)
                                 .withHasPart(Set.of(randomString(), randomString()))
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertDatasetToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForDataset();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertJournalArticleToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForJournalArticle();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertJournalArticleWithoutJournalIdToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForJournalArticleWithoutId();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertJournalArticleWithUnconfirmedJournalToNvaPublication() throws IOException {
        var brageGenerator = buildGeneratorForJournalArticleWithUnconfirmedJournal();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
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
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertMediaFeatureArticleToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MEDIA_FEATURE_ARTICLE)
                                 .withJournalId("journal")
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertLectureToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_LECTURE).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertChapterToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_CHAPTER).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertScientificChapterToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_SCIENTIFIC_CHAPTER).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertStudentPaperToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_STUDENT_PAPER_OTHERS).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertOtherStudentWorkToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_OTHER_STUDENT_WORK).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertConferencePosterToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_CONFERENCE_POSTER).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertDesignProductToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_DESIGN_PRODUCT).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertPlanOrBluePrintToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_PLAN_OR_BLUEPRINT).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertMusicToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MUSIC).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertScientificMonographToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_SCIENTIFIC_MONOGRAPH).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertInterviewToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_INTERVIEW).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertPresentationOtherToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_PRESENTATION_OTHER).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
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
        assertThatPublicationsMatch(actualPublication, expectedPublication);
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
        assertThatPublicationsMatch(actualPublication, expectedPublication);
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
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertPerformingArtsToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_PERFORMING_ARTS).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertVisualArtsToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_VISUAL_ARTS).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertReaderOpinionToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_READER_OPINION).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertAnthologyToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_ANTHOLOGY).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertTextbookToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_TEXTBOOK).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertFilmToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_FILM).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertLiteraryArtsToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_LITERARY_ARTS).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertExhibitionCatalogueToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_EXHIBITION_CATALOGUE).build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertPopularScienceMonographToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_POPULAR_SCIENCE_MONOGRAPH)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
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
        assertThatPublicationsMatch(actualPublication, expectedPublication);
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
        assertThatPublicationsMatch(actualPublication, expectedPublication);
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

        var file = (File) actualPublication.getAssociatedArtifacts().getFirst();

        var embargoToBeAfterThisDate = Instant.now().plus(Duration.ofDays(ALMOST_HUNDRED_YEARS));

        assertThat(file.getLegalNote(), is(equalTo(accessCode)));
        Assertions.assertTrue(file.getEmbargoDate().orElseThrow().isAfter(embargoToBeAfterThisDate));
    }

    @Test
    void shouldConvertCristinRecordToPublicationAndMergeWithExistingPublicationWithTheSameCristinId()
        throws IOException, BadRequestException {
        var cristinIdentifier = randomString();
        var existingPublication = randomPublication().copy()
                                      .withAdditionalIdentifiers(
                                          Set.of(cristinAdditionalIdentifier(cristinIdentifier)))
                                      .build();
        Resource.fromPublication(existingPublication)
            .persistNew(resourceService, UserInstance.fromPublication(existingPublication));
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_CRISTIN_RECORD)
                                 .withCristinIdentifier(cristinIdentifier)
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication.getEntityDescription().getReference().getPublicationContext(),
                   is(not(nullValue())));
    }

    @Test
    void shouldMergePublicationContextAndUseCourseFromBragePublicationWhenPublicationContextIsDegree()
        throws IOException, BadRequestException, InvalidUnconfirmedSeriesException {
        var cristinIdentifier = randomString();
        var publication = randomPublication();
        var existingPublication = publication.copy()
                                      .withAdditionalIdentifiers(Set.of(cristinAdditionalIdentifier(cristinIdentifier)))
                                      .withEntityDescription(entityDescriptionWithPublicationContextDegree(publication))
                                      .build();
        Resource.fromPublication(existingPublication)
            .persistNew(resourceService, UserInstance.fromPublication(existingPublication));
        var subjectCode = randomString();
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MASTER)
                                 .withCristinIdentifier(cristinIdentifier)
                                 .withSubjectCode(subjectCode)
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        var actualPublicationContext = ((Degree) actualPublication.getEntityDescription()
                                      .getReference()
                                      .getPublicationContext());

        assertThat(((UnconfirmedCourse) actualPublicationContext.getCourse()).code(), is(equalTo(subjectCode)));
    }

    private static no.unit.nva.model.EntityDescription entityDescriptionWithPublicationContextDegree(Publication publication)
        throws InvalidUnconfirmedSeriesException {
        var reference = publication.getEntityDescription().getReference();
        var degree = new Degree(null, null, null, null, List.of(), null);
        reference.setPublicationContext(degree);
        publication.getEntityDescription().setReference(reference);
        return publication.getEntityDescription();
    }

    @NotNull
    private static AdditionalIdentifier cristinAdditionalIdentifier(String cristinIdentifier) {
        return new AdditionalIdentifier("Cristin", cristinIdentifier);
    }

    @Test
    void shouldNotThrowExceptionWhenPublicationWithoutContributors() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_TEXTBOOK)
                                 .withNoContributors(true)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldNotAddLineBreaksToDescriptionAndAbstractWhenBrageRecordContainsSingleValue() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_TEXTBOOK)
                                 .withDescription(List.of(randomString()))
                                 .withAbstracts(List.of(randomString()))
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        assertThat(actualPublication.getEntityDescription().getDescription(), not(containsString("\n")));
        assertThat(actualPublication.getEntityDescription().getAbstract(), not(containsString("\n")));
    }

    @Test
    void shouldNotCreateAbstractAndDescriptionWhenSingleValueIsBlankString() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_TEXTBOOK)
                                 .withDescription(List.of("  "))
                                 .withAbstracts(List.of("  "))
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        assertThat(actualPublication.getEntityDescription().getDescription(), is(nullValue()));
        assertThat(actualPublication.getEntityDescription().getAbstract(), is(nullValue()));
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
        var errorReport = extractActualReportFromS3ClientForInvalidRecord(s3Event,
                                                                          NullPointerException.class.getSimpleName());
        var exception = errorReport.get("exception").asText();
        assertThat(exception, containsString("NullPointerException"));
    }

    @Test
    void shouldPersistPublicationInDatabase()
        throws IOException, nva.commons.apigateway.exceptions.NotFoundException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder().withPublishedDate(null)
                                 .withType(TYPE_BOOK)
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(resourceService.getPublicationByIdentifier(actualPublication.getIdentifier()),
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
        var errorReport = extractActualReportFromS3Client(s3Event, MissingFieldsException.class.getSimpleName(),
                                                          record);
        var exception = errorReport.get("exception").asText();
        assertThat(exception, containsString(TEST_DESCRIPTION));
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
                                            actualPublication,
                                            nvaBrageMigrationDataGenerator.getBrageRecord().getId());
        assertThat(actualStoredHandleString,
                   is(equalTo(nvaBrageMigrationDataGenerator.getNvaPublication().getHandle().toString())));
    }

    @Test
    void shouldSaveSuccessReportContainingIncomingHandleWhenUpdatingExistingPublication()
        throws BadRequestException, IOException {
        var cristinIdentifier = randomString();
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder().withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .withCristinIdentifier(cristinIdentifier)
                                                 .build();
        var existingPublication =
            persistPublicationWithCristinIdAndHandle(cristinIdentifier,
                                                     nvaBrageMigrationDataGenerator.getNvaPublication().getHandle());
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        var actualStoredHandleString =
            extractUpdatedPublicationsHandleReportFromS3Client(s3Event,
                                                               actualPublication,
                                                               nvaBrageMigrationDataGenerator.getBrageRecord()
                                                                   .getId());
        assertThat(actualPublication.getIdentifier(), is(equalTo(existingPublication.getIdentifier())));
        assertThat(actualStoredHandleString,
                   is(equalTo(nvaBrageMigrationDataGenerator.getBrageRecord().getId().toString())));
    }

    //TODO: flaky test
    @Test
    void shouldPersistMergeReport()
        throws IOException, BadRequestException, nva.commons.apigateway.exceptions.NotFoundException {
        var cristinIdentifier = randomString();
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder().withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .withCristinIdentifier(cristinIdentifier)
                                                 .build();
        var existingPublication =
            persistPublicationWithCristinIdAndHandle(cristinIdentifier,
                                                     nvaBrageMigrationDataGenerator.getNvaPublication().getHandle());
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);
        var storedMergeReportString =
            extractUpdateReportFromS3(s3Event,
                                      existingPublication,
                                      nvaBrageMigrationDataGenerator.getBrageRecord().getId());
        var storedMergeReport = JsonUtils.dtoObjectMapper.readValue(storedMergeReportString, BrageMergingReport.class);
        var updatedPublication = resourceService.getPublication(existingPublication);
        assertThat(storedMergeReport.oldImage(), is(equalTo(existingPublication)));
        assertThat(storedMergeReport.newImage(), is(equalTo(updatedPublication)));
    }

    @Test
    void shouldMergePublicationsIfDoiDuplicateExists() throws BadRequestException, IOException {
        var doi = randomDoi();
        var existingPublication = persistPublicationWithDoi(doi);
        assertThat(existingPublication.getHandle(), is(nullValue()));

        var listOfExistingPublications =
            List.of(new ResourceWithId(UriWrapper.fromUri("https://api.test.nva.aws.unit.no/publication/"
                                                          + existingPublication.getIdentifier().toString()).getUri()));
        var searchResourceApiResponseSingleHit = new SearchResourceApiResponse(listOfExistingPublications.size(),
                                                                               listOfExistingPublications);
        var singleHitOptional = Optional.of(searchResourceApiResponseSingleHit.toString());
        when(uriRetriever.getRawContent(any(), any())).thenReturn(singleHitOptional);

        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .withDoi(doi)
                                                 .build();

        var brageRecord = nvaBrageMigrationDataGenerator.getBrageRecord();
        var s3Event = createNewBrageRecordEvent(brageRecord);
        var publication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(publication.getHandle(), is(equalTo(brageRecord.getId())));
        assertThat(publication.getIdentifier(), is(equalTo(existingPublication.getIdentifier())));

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
        assertThat(publication.getHandle(), is(equalTo(brageRecord.getId())));

        var actualStoredHandleString = extractHandleReportFromS3Client(s3Event, publication, brageRecord.getId());
        assertThat(actualStoredHandleString,
                   is(equalTo(nvaBrageMigrationDataGenerator.getBrageRecord().getId().toString())));
    }

    @Test
    void whenMergingWithExistingPublicationTheHandleReportShouldBeStoredUnderUpdatedPublicationsFolder()
        throws IOException, BadRequestException {

        var cristinIdentifier = randomString();
        var existingPublication = randomPublication().copy()
                                      .withAdditionalIdentifiers(
                                          Set.of(cristinAdditionalIdentifier(cristinIdentifier)))
                                      .build();
        Resource.fromPublication(existingPublication)
            .persistNew(resourceService, UserInstance.fromPublication(existingPublication));

        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_CRISTIN_RECORD)
                                 .withCristinIdentifier(cristinIdentifier)
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var publication = handler.handleRequest(s3Event, CONTEXT);

        var s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        var updatedPublicationsFolder = UnixPath.of(UPDATED_PUBLICATIONS_REPORTS_PATH);
        var filesInUpdatedPublicationsFolder = s3Driver.getFiles(updatedPublicationsFolder);
        assertThat(filesInUpdatedPublicationsFolder, is(not(empty())));

        var storedHandleString = extractUpdatedPublicationsHandleReportFromS3Client(s3Event, publication,
                                                                                    brageGenerator.getBrageRecord()
                                                                                        .getId());
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
                                                                 publication,
                                                                 brageGenerator.getBrageRecord().getId());
        assertThat(storedHandleString, is(not(nullValue())));
    }

    @Test
    void shouldMergeCristinPublicationEvenWhenMostDataIsMissing()
        throws BadRequestException, IOException, nva.commons.apigateway.exceptions.NotFoundException {
        var cristinIdentifier = randomString();
        var cristinPublication = createCristinPublication(cristinIdentifier);
        var minimalRecord = createMinimalRecord(cristinIdentifier);
        var contentFile = createContentFile();
        minimalRecord.setContentBundle(new ResourceContent(List.of(contentFile)));
        var s3Event = createNewBrageRecordEvent(minimalRecord);
        handler.handleRequest(s3Event, CONTEXT);
        var updatedPublication = resourceService.getPublication(cristinPublication);

        //assert that dummy handles has not been stored in the updated publication
        assertThat(updatedPublication.getHandle(), not(equalTo(minimalRecord.getId())));
        assertThat(updatedPublication.getAdditionalIdentifiers(), not(hasItem(
            new AdditionalIdentifier("handle",
                                     minimalRecord.getId().toString()))));
        var associatedArtifacts = updatedPublication.getAssociatedArtifacts();

        // assert that  contentFile was copied
        assertThat(associatedArtifacts, hasSize(1));
        var publishedFile = (PublishedFile) associatedArtifacts.getFirst();
        assertThat(publishedFile.getName(), is(equalTo(contentFile.getFilename())));
        assertThat(publishedFile.getIdentifier(), is(equalTo(contentFile.getIdentifier())));
        assertThat(publishedFile.getLicense(), is(equalTo(contentFile.getLicense().getNvaLicense().getLicense())));
        assertThat(publishedFile.getPublisherVersion(), is(equalTo(minimalRecord.getPublisherAuthority().getNva())));

        //assert that we are storing reports based on the dummy handles:
        var updateHandleReporstFolder = UnixPath.of(UPDATE_REPORTS_PATH);
        var filesInUpdatedHandleReportsFolder = s3Driver.getFiles(updateHandleReporstFolder);
        assertThat(filesInUpdatedHandleReportsFolder, is(not(empty())));
        var storedHandleString = extractUpdateReportFromS3(s3Event,
                                                           cristinPublication,
                                                           minimalRecord.getId());
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
        var musicPerformance = (MusicPerformance) publication.getEntityDescription()
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
        throws BadRequestException, IOException {
        var cristinIdentifier = randomString();
        var cristinPublication =
            createCristinPublicationWithFilesAndRandomHandle(cristinIdentifier);
        var brageMigrationDataGenerator = new NvaBrageMigrationDataGenerator
                                                  .Builder()
                                              .withType(TYPE_MUSIC)
                                              .withResourceContent(new ResourceContent( List.of(createContentFile(),
                                                                                                createContentFile())))
                                              .withCristinIdentifier(cristinIdentifier)
                                              .build();
        var s3Event = createNewBrageRecordEvent(brageMigrationDataGenerator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);

        var discardedFilesReport = extractDiscardedFilesReportFromS3(brageMigrationDataGenerator.getBrageRecord(), s3Event, cristinPublication);
        assertThat(discardedFilesReport.getDiscardedFromUpdatedRecord(), hasSize(0));
        assertThat(discardedFilesReport.getDiscardedFromBrageRecord(),
                   hasSize(brageMigrationDataGenerator.getBrageRecord().getContentBundle().getContentFiles().size()));
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
                   .addChild("institution")
                   .addChild(timestamp)
                   .addChild(brageRecord.getId().getPath())
                   .addChild(String.valueOf(cristinPublication.getIdentifier()));
    }

    private Publication createCristinPublicationWithFilesAndRandomHandle(String cristinIdentifier)
        throws BadRequestException {
        var publication = createCristinPublication(cristinIdentifier);
        publication.setHandle(randomUri());
        publication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(randomPublishedFile())));
        return resourceService.updatePublication(publication);
    }

    private AssociatedArtifact randomPublishedFile() {

        return new PublishedFile(java.util.UUID.randomUUID(), randomString(), "application/pdf", 10L, null, false,
                                 PublisherVersion.PUBLISHED_VERSION, null, NullRightsRetentionStrategy.create(
            RightsRetentionStrategyConfiguration.UNKNOWN), null, Instant.now());
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

    private Publication createCristinPublication(String cristinIdentifier) throws BadRequestException {
        var publication = randomPublication().copy()
                              .withAdditionalIdentifiers(
                                  Set.of(cristinAdditionalIdentifier(cristinIdentifier)))
                              .withAssociatedArtifacts(List.of())
                              .build();
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private Record createMinimalRecord(String cristinIdentifier) {
        var minimalRecord = new Record();
        var fakeDummyHandle = UriWrapper.fromUri(DUMMY_HANDLE_THAT_EXIST_FOR_PROCESSING_UNIS
                                                 + "/1").getUri();
        minimalRecord.setId(fakeDummyHandle);
        minimalRecord.setPublisherAuthority(new PublisherAuthority(List.of(),
                                                                   PublisherVersion.PUBLISHED_VERSION));
        minimalRecord.setResourceOwner(new ResourceOwner("unis@186.0.0.0", randomUri()));
        minimalRecord.setCristinId(cristinIdentifier);
        minimalRecord.setEntityDescription(new EntityDescription());
        minimalRecord.setType(new Type(List.of(), CRISTIN_RECORD.getValue()));
        return minimalRecord;
    }

    private ContentFile createContentFile() {
        var contentFile = new ContentFile();
        contentFile.setFilename("MyAwsomeUnisFile.pdf");
        contentFile.setBundleType(BundleType.ORIGINAL);
        contentFile.setIdentifier(java.util.UUID.randomUUID());
        contentFile.setLicense(new License("", new NvaLicense(randomUri())));
        return contentFile;
    }

    private Publication persistPublicationWithCristinIdAndHandle(String cristinIdentifier, URI handle)
        throws BadRequestException {
        var publication = randomPublication().copy()
                              .withAdditionalIdentifiers(
                                  Set.of(cristinAdditionalIdentifier(cristinIdentifier)))
                              .withHandle(handle)
                              .build();
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private Publication persistPublicationWithDoi(URI doi)
        throws BadRequestException {
        var publication = randomPublication().copy()
                              .withDoi(doi)
                              .withHandle(null)
                              .build();

        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private String extractUpdateReportFromS3(S3Event s3Event,
                                             Publication publication,
                                             URI brageHandle) {
        var timestamp = s3Event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        var uri = UriWrapper.fromUri(UPDATE_REPORTS_PATH)
                      .addChild("institution")
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
        assertThat(actualPublication.getSubjects(), containsInAnyOrder(expectedPublication.getSubjects().toArray()));
        var ignoredFields = new String[]{"createdDate", "identifier", "modifiedDate", "publishedDate", "subjects",
            "associatedArtifacts"};
        assertThat(actualPublication, is(samePropertyValuesAs(expectedPublication, ignoredFields)));
        assertThat(actualPublication.getAssociatedArtifacts(),
                   hasSize(expectedPublication.getAssociatedArtifacts().size()));
        if (!actualPublication.getAssociatedArtifacts().isEmpty()) {
            assertThat(actualPublication.getAssociatedArtifacts(),
                       samePropertyValuesAs(expectedPublication.getAssociatedArtifacts(), "publishedDate"));
        }
    }

    private void persistMultiplePublicationWithSameCristinId() {
        IntStream.range(0, 5)
            .boxed()
            .map(i -> buildGeneratorObjectWithCristinId())
            .map(NvaBrageMigrationDataGenerator::getNvaPublication)
            .forEach(publication -> resourceService.createPublicationFromImportedEntry(publication));
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
                   .withSeriesNumberRecord("series;42")
                   .withSeriesNumberPublication("42")
                   .withSeriesTitle("series")
                   .withIssn(List.of(randomIssn(), randomIssn()))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForMap() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_MAP).withPublisherId("someId").build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForBookWithoutValidSeriesNumber() {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_BOOK)
                   .withSeriesNumberRecord(PART_OF_SERIES_VALUE_V6)
                   .withSeriesNumberPublication(null)
                   .withPublicationDate(PUBLICATION_DATE)
                   .withIsbn(randomIsbn10())
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForBook(String seriesNumber) {
        return new NvaBrageMigrationDataGenerator.Builder().withType(TYPE_BOOK)
                   .withSeriesNumberRecord(seriesNumber)
                   .withSeriesNumberPublication(EXPECTED_SERIES_NUMBER)
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
                   .addChild("institution")
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

    private UriWrapper constructErrorFileUri(S3Event event, String exceptionSimpleName, Record brageRecord) {
        var fileUri = UriWrapper.fromUri(extractFilename(event));
        var timestamp = event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        return UriWrapper.fromUri(ERROR_BUCKET_PATH)
                   .addChild(brageRecord.getResourceOwner().getOwner().split("@")[0])
                   .addChild(timestamp)
                   .addChild(exceptionSimpleName)
                   .addChild(fileUri.getLastPathElement());
    }

    private UriWrapper constructErrorFileUriForInvalidBrageRecord(S3Event event, String exceptionSimpleName) {
        var fileUri = UriWrapper.fromUri(extractFilename(event));
        var timestamp = event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        return UriWrapper.fromUri(ERROR_BUCKET_PATH)
                   .addChild(fileUri.getLastPathElement())
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
                           .buildPublishedFile());
    }

    private S3Event createNewInvalidBrageRecordEvent() throws IOException {
        var invalidBrageRecord = randomJson();
        var uri = s3Driver.insertFile(randomS3Path(), invalidBrageRecord);
        return createS3Event(uri);
    }

    private S3Event createNewBrageRecordEvent(Record brageRecord) throws IOException {
        var recordAsJson = JsonUtils.dtoObjectMapper.writeValueAsString(brageRecord);
        var uri = s3Driver.insertFile(UnixPath.of("institution/" + randomString()), recordAsJson);
        return createS3Event(uri);
    }

    private S3Event createNewBrageRecordEventWithSpecifiedObjectKey(Record brageRecord) throws IOException {
        var recordAsJson = JsonUtils.dtoObjectMapper.writeValueAsString(brageRecord);
        var uri = s3Driver.insertFile(UnixPath.of("my/path/some.json"), recordAsJson);
        return createS3Event(uri);
    }

    private UnixPath randomS3Path() {
        return UnixPath.of(randomString());
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


