package no.sikt.nva.brage.migration.lambda;

import static no.sikt.nva.brage.migration.NvaType.ANTHOLOGY;
import static no.sikt.nva.brage.migration.NvaType.CRISTIN_RECORD;
import static no.sikt.nva.brage.migration.NvaType.FILM;
import static no.sikt.nva.brage.migration.NvaType.PERFORMING_ARTS;
import static no.sikt.nva.brage.migration.NvaType.PROFESSIONAL_ARTICLE;
import static no.sikt.nva.brage.migration.NvaType.READER_OPINION;
import static no.sikt.nva.brage.migration.NvaType.TEXTBOOK;
import static no.sikt.nva.brage.migration.NvaType.VISUAL_ARTS;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.ERROR_BUCKET_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.HANDLE_REPORTS_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.PATH_SEPERATOR;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.UPDATE_REPORTS_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.YYYY_MM_DD_HH_FORMAT;
import static no.sikt.nva.brage.migration.merger.AssociatedArtifactMover.COULD_NOT_COPY_ASSOCIATED_ARTEFACT_EXCEPTION_MESSAGE;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn10;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.merger.UnmappableCristinRecordException;
import no.sikt.nva.brage.migration.record.PublicationDate;
import no.sikt.nva.brage.migration.record.PublicationDateNva;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.Type;
import no.sikt.nva.brage.migration.record.content.ContentFile;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import no.sikt.nva.brage.migration.record.content.ResourceContent.BundleType;
import no.sikt.nva.brage.migration.record.license.License;
import no.sikt.nva.brage.migration.record.license.NvaLicense;
import no.sikt.nva.brage.migration.testutils.FakeS3ClientThrowingExceptionWhenCopying;
import no.sikt.nva.brage.migration.testutils.FakeS3cClientWithCopyObjectSupport;
import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

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
    public static final Type TYPE_VISUAL_ARTS = new Type(List.of(VISUAL_ARTS.getValue()),
                                                             VISUAL_ARTS.getValue());
    public static final Type TYPE_READER_OPINION = new Type(List.of(READER_OPINION.getValue()),
                                                         READER_OPINION.getValue());
    public static final Type TYPE_ANTHOLOGY = new Type(List.of(ANTHOLOGY.getValue()),
                                                       ANTHOLOGY.getValue());
    public static final Type TYPE_CRISTIN_RECORD = new Type(List.of(CRISTIN_RECORD.getValue()),
                                                       CRISTIN_RECORD.getValue());
    public static final Type TYPE_TEXTBOOK = new Type(List.of(TEXTBOOK.getValue()), TEXTBOOK.getValue());
    public static final Type TYPE_FILM = new Type(List.of(FILM.getValue()), FILM.getValue());
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
    public static final String EMBARGO_DATE = "2019-05-16T11:56:24Z";
    public static final PublicationDate PUBLICATION_DATE =
        new PublicationDate("2020",
                            new PublicationDateNva.Builder().withYear(
                                "2020").build());
    public static final Organization TEST_ORGANIZATION =
        new Organization.Builder().withId(URI.create(
            "https://api.nva.unit.no/customer/test")).build();
    public static final String FILENAME = "filename";
    public static final String HARD_CODED_CRISTIN_IDENTIFIER = "12345";
    public static final String RESOURCE_EXCEPTION_MESSAGE = "resourceExceptionMessage";
    public static final URI LICENSE_URI = URI.create("http://creativecommons.org/licenses/by-nc/4.0/");
    private static final Type TYPE_REPORT_WORKING_PAPER = new Type(List.of(NvaType.WORKING_PAPER.getValue()),
                                                                   NvaType.WORKING_PAPER.getValue());
    private static final Type TYPE_LECTURE = new Type(List.of(NvaType.LECTURE.getValue()),
                                                      NvaType.LECTURE.getValue());
    private static final Type TYPE_CHAPTER = new Type(List.of(NvaType.CHAPTER.getValue()),
                                                      NvaType.CHAPTER.getValue());
    private static final Type TYPE_SCIENTIFIC_CHAPTER = new Type(List.of(NvaType.SCIENTIFIC_CHAPTER.getValue()),
                                                                 NvaType.SCIENTIFIC_CHAPTER.getValue());
    private static final Type TYPE_FEATURE_ARTICLE = new Type(List.of(NvaType.CHRONICLE.getValue()),
                                                              NvaType.CHRONICLE.getValue());
    private static final Type TYPE_SOFTWARE = new Type(List.of(NvaType.SOFTWARE.getValue()),
                                                       NvaType.SOFTWARE.getValue());
    private static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    private static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    private static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    private static final String INPUT_BUCKET_NAME = "some-input-bucket-name";
    private final String persistedStorageBucket = new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");
    private BrageEntryEventConsumer handler;
    private S3Driver s3Driver;
    private FakeS3Client s3Client;
    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.s3Client = new FakeS3cClientWithCopyObjectSupport();
        this.handler = new BrageEntryEventConsumer(s3Client, resourceService);
        this.s3Driver = new S3Driver(s3Client, INPUT_BUCKET_NAME);
    }

    @Test
    void shouldConvertBrageRecordToNvaPublicationWithCorrectCustomer() throws IOException {
        var brageGenerator = buildGeneratorForRecord();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, brageGenerator.getNvaPublication());
    }

    //Temporary compares associatedArtifacts based on contentLength. Has to be changed for proper comparison of
    // artifacts. See AssociatedArtifactComparator.
    @Test
    void shouldNotAttachAssociatedArtifactsToExistingPublicationWhenAssociatedArtifactAlreadyExistsAndCristinIdsMatch()
        throws IOException, NotFoundException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_REPORT_WORKING_PAPER)
                                 .withCristinIdentifier("123456")
                                 .withResourceContent(createResourceContent())
                                 .withAssociatedArtifacts(createCorrespondingAssociatedArtifacts())
                                 .withLink(randomUri())
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, brageGenerator.getNvaPublication());
    }

    @Test
    void shouldAttachAssociatedArtifactsToExistingPublicationWhenNewAssociatedArtifactAndWhenCristinIdsMatch()
        throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_REPORT_WORKING_PAPER)
                                 .withCristinIdentifier("123456")
                                 .withResourceContent(createResourceContent())
                                 .build();
        var s3Driver = new S3Driver(s3Client, persistedStorageBucket);
        var file = new java.io.File("src/test/resources/testFile.txt");
        putAssociatedArtifactsToResourceStorage(brageGenerator, s3Driver, file);
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var expectedPublication = createPublicationWithAssociatedArtifacts(brageGenerator.getNvaPublication(),
                                                                           createCorrespondingAssociatedArtifacts());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldAttachCertainMetadataFieldsToExistingPublicationWhenExistingPublicationDoesNotHaveThoseFields()
        throws IOException {
        // The metadata fields are currently Description, Abstract and handle
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_REPORT_WORKING_PAPER)
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
        assertThat(actualPublication.getEntityDescription().getDescription(),
                   is(equalTo(cristinDescription)));
        assertThat(actualPublication.getEntityDescription().getAbstract(),
                   is(equalTo(cristinAbstract)));
    }

    @Test
    void shouldCreateNewPublicationWhenPublicationHasCristinIdWhichIsNotPresentInNva()
        throws IOException, NotFoundException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_REPORT_WORKING_PAPER)
                                 .withCristinIdentifier("123456")
                                 .withResourceContent(createResourceContent())
                                 .withAssociatedArtifacts(createCorrespondingAssociatedArtifacts())
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldCreateNewPublicationWhenPublicationHasNoCristinId() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_REPORT_WORKING_PAPER)
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, brageGenerator.getNvaPublication());
    }

    @Test
    void shouldThrowExceptionWhenThereIsMultipleSearchResultsOnCristinId() throws IOException {
        var s3Event = createNewBrageRecordEvent(buildGeneratorObjectWithCristinId().getBrageRecord());
        persistMultiplePublicationWithSameCristinId();
        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @ParameterizedTest(name = "shouldConvertBookToNvaPublication")
    @ValueSource(strings = {PART_OF_SERIES_VALUE_V1, PART_OF_SERIES_VALUE_V2,
        PART_OF_SERIES_VALUE_V3, PART_OF_SERIES_VALUE_V4, PART_OF_SERIES_VALUE_V5})
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
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_REPORT_WORKING_PAPER)
                                 .build();
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
        var brageGenerator = buildGeneratorForPhd();
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
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_SCIENTIFIC_ARTICLE)
                                 .withJournalTitle("Journal")
                                 .withJournalId("id")
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertFeatureArticleToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_FEATURE_ARTICLE)
                                 .withJournalId("journal")
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertLectureToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_LECTURE)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertChapterToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_CHAPTER)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertScientificChapterToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_SCIENTIFIC_CHAPTER)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertStudentPaperToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_STUDENT_PAPER_OTHERS)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertOtherStudentWorkToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_OTHER_STUDENT_WORK)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertConferencePosterToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_CONFERENCE_POSTER)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertDesignProductToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_DESIGN_PRODUCT)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertPlanOrBluePrintToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_PLAN_OR_BLUEPRINT)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertMusicToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_MUSIC)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertScientificMonographToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_SCIENTIFIC_MONOGRAPH)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertInterviewToNvaPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_INTERVIEW)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertPresentationOtherToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_PRESENTATION_OTHER)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertWhenPublicationContextIsNull() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withPublishedDate(null)
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
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withPublishedDate(null)
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
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withJournalTitle("Journal")
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
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_PERFORMING_ARTS)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertVisualArtsToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_VISUAL_ARTS)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertReaderOpinionToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_READER_OPINION)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertAnthologyToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_ANTHOLOGY)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertTextbookToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_TEXTBOOK)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertFilmToPublication() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_FILM)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertToPublicationWithUnconfirmedJournalWhenJournalIdIsNotPresent() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_FEATURE_ARTICLE)
                                 .withJournalTitle("Some Very Popular Journal")
                                 .withIssn(List.of(randomIssn()))
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldConvertCristinRecordToPublicationAndMergeWithExistingPublicationWithTheSameCristinId()
        throws IOException, BadRequestException {
        var cristinIdentifier = randomString();
        var existingPublication =
            randomPublication().copy().withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(
                "Cristin", cristinIdentifier))).build();
        Resource.fromPublication(existingPublication).persistNew(resourceService,
                                                                 UserInstance.fromPublication(existingPublication));
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_CRISTIN_RECORD)
                                 .withCristinIdentifier(cristinIdentifier)
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication.getEntityDescription().getReference().getPublicationContext(),
                   is(not(nullValue())));
    }

    @Test
    void shouldNotThrowExceptionWhenPublicationWithoutContributors() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_TEXTBOOK)
                                 .withNoContributors(true)
                                 .build();
        var expectedPublication = brageGenerator.getNvaPublication();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);

        assertThatPublicationsMatch(actualPublication, expectedPublication);
    }

    @Test
    void shouldThrowExceptionWhenConvertingCristinRecordAndPublicationWithMatchingCristinIdDoesExist()
        throws IOException, BadRequestException {
        var existingPublication = randomPublication().copy().withAdditionalIdentifiers(Set.of()).build();
        Resource.fromPublication(existingPublication).persistNew(resourceService,
                                                                 UserInstance.fromPublication(existingPublication));
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_CRISTIN_RECORD)
                                 .withCristinIdentifier(randomString())
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());

        assertThrows(UnmappableCristinRecordException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNotSupportedInPublicationContext() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_SOFTWARE)
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNotSupportedInPublicationInstance() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withType(TYPE_SOFTWARE)
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldThrowExceptionWhenInvalidBrageRecordIsProvided() throws IOException {
        var s3Event = createNewInvalidBrageRecordEvent();
        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldPersistPublicationInDatabase() throws IOException, nva.commons.apigateway.exceptions.NotFoundException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withPublishedDate(null)
                                 .withType(TYPE_BOOK)
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThatPublicationsMatch(
            resourceService.getPublicationByIdentifier(actualPublication.getIdentifier()),
            brageGenerator.getNvaPublication());
    }

    @Test
    void shouldTryToPersistPublicationInDatabaseSeveralTimesWhenResourceServiceIsThrowingException()
        throws IOException {
        var fakeResourceServiceThrowingException = resourceServiceThrowingExceptionWhenSavingResource();
        this.handler = new BrageEntryEventConsumer(s3Client, fakeResourceServiceThrowingException);
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldThrowExceptionIfItCannotCopyAssociatedArtifacts() throws IOException {
        this.s3Client = new FakeS3ClientThrowingExceptionWhenCopying();
        this.s3Driver = new S3Driver(s3Client, INPUT_BUCKET_NAME);
        this.handler = new BrageEntryEventConsumer(s3Client, resourceService);
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withPublishedDate(null)
                                 .withType(TYPE_BOOK)
                                 .withResourceContent(createResourceContent())
                                 .withAssociatedArtifacts(createCorrespondingAssociatedArtifacts())
                                 .build();
        var s3Event = createNewBrageRecordEvent(brageGenerator.getBrageRecord());

        Executable action = () -> handler.handleRequest(s3Event, CONTEXT);
        var exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(COULD_NOT_COPY_ASSOCIATED_ARTEFACT_EXCEPTION_MESSAGE));
    }

    @Test
    void shouldCopyAssociatedArtifactsToResourceStorage() throws IOException {
        var brageGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                 .withPublishedDate(null)
                                 .withResourceContent(createResourceContent())
                                 .withType(TYPE_BOOK)
                                 .build();
        var s3Event = createNewBrageRecordEventWithSpecifiedObjectKey(brageGenerator.getBrageRecord()
        );
        var objectKey = UUID;
        var expectedDopyObjRequest = CopyObjectRequest.builder()
                                         .sourceBucket(INPUT_BUCKET_NAME)
                                         .destinationBucket(
                                             new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME"))
                                         .sourceKey("my/path/" + objectKey)
                                         .destinationKey(objectKey.toString())
                                         .build();
        handler.handleRequest(s3Event, CONTEXT);
        var fakeS3cClientWithCopyObjectSupport = (FakeS3cClientWithCopyObjectSupport) s3Client;
        var actualCopyObjectRequests = fakeS3cClientWithCopyObjectSupport.getCopyObjectRequestList();
        assertThat(actualCopyObjectRequests, hasSize(1));
        assertThat(actualCopyObjectRequests, contains(expectedDopyObjRequest));
    }

    @Test
    void throwErrorWhenMandatoryFieldsAreMissing() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_BOOK)
                                                 .withIsbn(randomIsbn10())
                                                 .withNullHandle()
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        assertThrows(MissingFieldsException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldSaveErrorReportInS3ContainingTheOriginalInputData() throws IOException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        this.handler = new BrageEntryEventConsumer(s3Client, resourceService);
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());

        Executable action = () -> handler.handleRequest(s3Event, CONTEXT);
        var exception = assertThrows(RuntimeException.class, action);
        var actualReport = extractActualReportFromS3Client(s3Event, exception);
        var input = actualReport.get("input").asText();
        var actualErrorReportBrageRecord = JsonUtils.dtoObjectMapper.readValue(input, Record.class);
        assertThat(actualErrorReportBrageRecord,
                   is(equalTo(nvaBrageMigrationDataGenerator.getBrageRecord())));
    }

    @Test
    void shouldSaveHandleAndResourceIdentifierReportInS3() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        var actualStoredHandleString = extractActualHandleReportFromS3Client(s3Event, actualPublication);
        assertThat(actualStoredHandleString,
                   is(equalTo(nvaBrageMigrationDataGenerator.getNvaPublication().getHandle().toString())));
    }

    @Test
    void shouldSavePublicationBeforeUpdateInS3() throws IOException, BadRequestException {
        var cristinIdentifier = randomString();
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .withCristinIdentifier(cristinIdentifier)
                                                 .build();
        var existingPublication = persistPublicationWithCristinIdAndHandle(
            cristinIdentifier, nvaBrageMigrationDataGenerator.getNvaPublication().getHandle());
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        handler.handleRequest(s3Event, CONTEXT);
        var storedPublication = extractUpdateReportFromS3(s3Event, existingPublication.getIdentifier());
        assertThat(storedPublication, is(equalTo(existingPublication.toString())));
    }

    private static Publication copyPublication(NvaBrageMigrationDataGenerator brageGenerator)
        throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(
            JsonUtils.dtoObjectMapper.writeValueAsString(brageGenerator.getNvaPublication()), Publication.class);
    }

    private static void putAssociatedArtifactsToResourceStorage(NvaBrageMigrationDataGenerator dataGenerator,
                                                                S3Driver s3Driver, java.io.File file) {
        dataGenerator.getNvaPublication().getAssociatedArtifacts().stream()
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

    private Publication persistPublicationWithCristinIdAndHandle(String cristinIdentifier, URI handle)
        throws BadRequestException {
        var publication =
            randomPublication().copy()
                .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Cristin", cristinIdentifier)))
                .withHandle(handle)
                .build();
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }

    private String extractUpdateReportFromS3(S3Event s3Event, SortableIdentifier identifier) {
        var timestamp = s3Event.getRecords().get(0).getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        var uri = UriWrapper.fromUri(UPDATE_REPORTS_PATH).addChild(timestamp).addChild(String.valueOf(identifier));
        S3Driver s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        return s3Driver.getFile(uri.toS3bucketPath());
    }

    private ResourceService resourceServiceThrowingExceptionWhenSavingResource() {
        return new ResourceService(client, Clock.systemDefaultZone()) {
            @Override
            public Publication createPublicationFromImportedEntry(Publication publication) {
                throw new RuntimeException(RESOURCE_EXCEPTION_MESSAGE);
            }
        };
    }

    private void assertThatPublicationsMatch(Publication actualPublication,
                                             Publication expectedPublication) {
        assertThat(actualPublication.getEntityDescription(), is(equalTo(expectedPublication.getEntityDescription())));
        assertThat(actualPublication.getAssociatedArtifacts(),
                   is(equalTo(expectedPublication.getAssociatedArtifacts())));
        assertThat(actualPublication.getDoi(), is(equalTo(expectedPublication.getDoi())));
        assertThat(actualPublication.getResourceOwner(), is(equalTo(expectedPublication.getResourceOwner())));
        assertThat(actualPublication.getAdditionalIdentifiers(),
                   is(equalTo(expectedPublication.getAdditionalIdentifiers())));
        assertThat(actualPublication.getFundings(), is(equalTo(expectedPublication.getFundings())));
        assertThat(actualPublication.getHandle(), is(equalTo(expectedPublication.getHandle())));
        assertThat(actualPublication.getHandle(), is(equalTo(expectedPublication.getHandle())));
        assertThat(actualPublication.getLink(), is(equalTo(expectedPublication.getLink())));
        assertThat(actualPublication.getSubjects(), containsInAnyOrder(expectedPublication.getSubjects().toArray()));
    }

    private Publication createPublicationWithAssociatedArtifacts(Publication publication,
                                                                 List<AssociatedArtifact> associatedArtifacts) {
        return publication.copy().withAssociatedArtifacts(new AssociatedArtifactList(associatedArtifacts)).build();
    }

    private void persistMultiplePublicationWithSameCristinId() {
        IntStream.range(0, 5).boxed()
            .map(i -> buildGeneratorObjectWithCristinId())
            .map(NvaBrageMigrationDataGenerator::getNvaPublication)
            .forEach(publication -> resourceService.createPublicationFromImportedEntry(publication));
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForJournalArticleWithoutId() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_JOURNAL_ARTICLE)
                   .withSpatialCoverage(List.of("Norway"))
                   .withJournalTitle("Some Very Popular Journal")
                   .withIssn(List.of(randomIssn(), randomIssn()))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForJournalArticleWithUnconfirmedJournal() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_JOURNAL_ARTICLE)
                   .withSpatialCoverage(List.of("Norway"))
                   .withJournalTitle("Some Very Popular Journal")
                   .withIssn(List.of(randomIssn()))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForJournalArticle() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_JOURNAL_ARTICLE)
                   .withJournalId("someId")
                   .withSpatialCoverage(List.of("Norway"))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForDataset() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_DATASET)
                   .withPublisherId("someId")
                   .withPublicationDate(new PublicationDate("03-08",
                                                            new PublicationDateNva.Builder().withYear(null).withDay(
                                                                "03").withMonth("08").build()))
                   .withPublicationDateForPublication(
                       new no.unit.nva.model.PublicationDate.Builder().withYear(null).withDay(
                           "03").withMonth("08").build())
                   .withSpatialCoverage(List.of("Norway"))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForPhd() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_PHD)
                   .withPublicationDate(PUBLICATION_DATE)
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForMaster() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_MASTER)
                   .withPublicationDate(PUBLICATION_DATE)
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForBachelor() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_BACHELOR)
                   .withPublicationDate(PUBLICATION_DATE)
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorObjectWithCristinId() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_BACHELOR)
                   .withPublicationDate(PUBLICATION_DATE)
                   .withCristinIdentifier(HARD_CODED_CRISTIN_IDENTIFIER)
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForResearchReport() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_RESEARCH_REPORT)
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForReport() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_REPORT)
                   .withSeries("someSeries;42")
                   .withIssn(List.of(randomIssn()))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForReportWithUnconfirmedSeries() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_REPORT)
                   .withSeriesNumberRecord("series;42")
                   .withSeriesNumberPublication("42")
                   .withSeriesTitle("series")
                   .withIssn(List.of(randomIssn(), randomIssn()))
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForMap() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_MAP)
                   .withPublisherId("someId")
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForBookWithoutValidSeriesNumber() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_BOOK)
                   .withSeriesNumberRecord(PART_OF_SERIES_VALUE_V6)
                   .withSeriesNumberPublication(null)
                   .withPublicationDate(PUBLICATION_DATE)
                   .withIsbn(randomIsbn10())
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForBook(String seriesNumber) {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_BOOK)
                   .withSeriesNumberRecord(seriesNumber)
                   .withSeriesNumberPublication(EXPECTED_SERIES_NUMBER)
                   .withPublicationDate(PUBLICATION_DATE)
                   .withIsbn(randomIsbn10())
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForRecord() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_BOOK)
                   .withIsbn(randomIsbn10())
                   .withResourceContent(createResourceContent())
                   .withAssociatedArtifacts(createCorrespondingAssociatedArtifacts())
                   .withOrganization(TEST_ORGANIZATION)
                   .build();
    }

    private String extractActualHandleReportFromS3Client(S3Event s3Event, Publication actualPublication) {
        UriWrapper handleReport = constructHandleReportFileUri(s3Event, actualPublication);
        S3Driver s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        return s3Driver.getFile(handleReport.toS3bucketPath());
    }

    private UriWrapper constructHandleReportFileUri(S3Event s3Event, Publication actualPublication) {
        var timestamp = s3Event.getRecords().get(0).getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        return UriWrapper
                   .fromUri(HANDLE_REPORTS_PATH)
                   .addChild(actualPublication.getResourceOwner().getOwner().getValue().split("@")[0])
                   .addChild(timestamp)
                   .addChild(actualPublication.getIdentifier().toString());
    }

    private JsonNode extractActualReportFromS3Client(
        S3Event s3Event,
        Exception exception) throws JsonProcessingException {
        UriWrapper errorFileUri = constructErrorFileUri(s3Event, exception);
        S3Driver s3Driver = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"));
        String content = s3Driver.getFile(errorFileUri.toS3bucketPath());
        return JsonUtils.dtoObjectMapper.readTree(content);
    }

    private UriWrapper constructErrorFileUri(S3Event event,
                                             Exception exception) {
        var fileUri = UriWrapper.fromUri(extractFilename(event));
        var timestamp = event.getRecords().get(0).getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
        return UriWrapper.fromUri(
            ERROR_BUCKET_PATH + PATH_SEPERATOR + timestamp + PATH_SEPERATOR + exception.getClass().getSimpleName()
            + PATH_SEPERATOR + fileUri.getLastPathElement());
    }

    private String extractFilename(S3Event event) {
        return event.getRecords().get(0).getS3().getObject().getKey();
    }

    private ResourceContent createResourceContent() {
        var file = new ContentFile(FILENAME,
                                   BundleType.ORIGINAL,
                                   "description",
                                   UUID,
                                   new License("someLicense", new NvaLicense(URI.create("https://creativecommons"
                                                                                        + ".org/licenses/by-nc/4.0"))),
                                   EMBARGO_DATE);

        return new ResourceContent(Collections.singletonList(file));
    }

    private List<AssociatedArtifact> createCorrespondingAssociatedArtifacts() {
        return List.of(File.builder()
                           .withIdentifier(UUID)
                           .withLicense(LICENSE_URI)
                           .withName(FILENAME)
                           .withSize(FakeS3cClientWithCopyObjectSupport.SOME_CONTENT_LENGTH)
                           .withMimeType(FakeS3cClientWithCopyObjectSupport.APPLICATION_PDF_MIMETYPE)
                           .withEmbargoDate(Instant.parse(EMBARGO_DATE))
                           .buildPublishedFile());
    }

    private S3Event createNewInvalidBrageRecordEvent() throws IOException {
        var invalidBrageRecord = randomJson();
        var uri = s3Driver.insertFile(randomS3Path(), invalidBrageRecord);
        return createS3Event(uri);
    }

    private S3Event createNewBrageRecordEvent(Record brageRecord) throws IOException {
        var recordAsJson = JsonUtils.dtoObjectMapper.writeValueAsString(brageRecord);
        var uri = s3Driver.insertFile(randomS3Path(), recordAsJson);
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
        var eventNotification = new S3EventNotificationRecord(randomString(),
                                                              randomString(),
                                                              randomString(),
                                                              randomDate(),
                                                              randomString(),
                                                              EMPTY_REQUEST_PARAMETERS,
                                                              EMPTY_RESPONSE_ELEMENTS,
                                                              createS3Entity(expectedObjectKey),
                                                              EMPTY_USER_IDENTITY);
        return new S3Event(List.of(eventNotification));
    }

    private String randomDate() {
        return Instant.now().toString();
    }

    private S3Entity createS3Entity(String expectedObjectKey) {
        var bucket = new S3BucketEntity(INPUT_BUCKET_NAME, EMPTY_USER_IDENTITY, randomString());
        var object = new S3ObjectEntity(expectedObjectKey,
                                        SOME_FILE_SIZE,
                                        randomString(),
                                        randomString(),
                                        randomString());
        var schemaVersion = randomString();
        return new S3Entity(randomString(), bucket, object, schemaVersion);
    }
}


