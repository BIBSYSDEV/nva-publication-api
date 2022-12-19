package no.sikt.nva.brage.migration.lambda;

import static no.sikt.nva.brage.migration.AssociatedArtifactMover.COULD_NOT_COPY_ASSOCIATED_ARTEFACT_EXCEPTION_MESSAGE;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.ERROR_BUCKET_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.HANDLE_REPORTS_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.PATH_SEPERATOR;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.YYYY_MM_DD_HH_FORMAT;
import static no.sikt.nva.brage.migration.mapper.BrageNvaMapper.NORWEGIAN_BOKMAAL;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn10;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.record.PublicationDate;
import no.sikt.nva.brage.migration.record.PublicationDateNva;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.Type;
import no.sikt.nva.brage.migration.record.content.ContentFile;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import no.sikt.nva.brage.migration.record.content.ResourceContent.BundleType;
import no.sikt.nva.brage.migration.record.license.License;
import no.sikt.nva.brage.migration.record.license.NvaLicense;
import no.sikt.nva.brage.migration.record.license.NvaLicenseIdentifier;
import no.sikt.nva.brage.migration.testutils.FakeResourceService;
import no.sikt.nva.brage.migration.testutils.FakeResourceServiceThrowingException;
import no.sikt.nva.brage.migration.testutils.FakeS3ClientThrowingExceptionWhenCopying;
import no.sikt.nva.brage.migration.testutils.FakeS3cClientWithCopyObjectSupport;
import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

public class BrageEntryEventConsumerTest {

    public static final String PART_OF_SERIES_VALUE_V1 = "SOMESERIES;42";
    public static final String PART_OF_SERIES_VALUE_V2 = "SOMESERIES;42:2022";
    public static final String PART_OF_SERIES_VALUE_V3 = "SOMESERIES;2022:42";
    public static final String PART_OF_SERIES_VALUE_V4 = "SOMESERIES;2022/42";
    public static final String PART_OF_SERIES_VALUE_V5 = "SOMESERIES;42/2022";
    public static final String PART_OF_SERIES_VALUE_V6 = "NVE Rapport;";
    public static final String EXPECTED_SERIES_NUMBER = "42";
    public static final UUID UUID = java.util.UUID.randomUUID();
    public static final Context CONTEXT = mock(Context.class);
    public static final long SOME_FILE_SIZE = 100L;
    public static final Type TYPE_BOOK = new Type(List.of(NvaType.BOOK.getValue()), NvaType.BOOK.getValue());
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
    public static final Type TYPE_SCIENTIFIC_MONOGRAPH = new Type(List.of(NvaType.SCIENTIFIC_MONOGRAPH.getValue()),
                                                                  NvaType.SCIENTIFIC_MONOGRAPH.getValue());

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
    public static final NvaLicenseIdentifier LICENSE_IDENTIFIER = NvaLicenseIdentifier.CC_BY_NC;
    public static final String FILENAME = "filename";
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

    private BrageEntryEventConsumer handler;
    private S3Driver s3Driver;
    private FakeS3Client s3Client;
    private FakeResourceService resourceService;

    @BeforeEach
    void init() {
        this.resourceService = new FakeResourceService();
        this.s3Client = new FakeS3cClientWithCopyObjectSupport();
        this.handler = new BrageEntryEventConsumer(s3Client, resourceService);
        this.s3Driver = new S3Driver(s3Client, INPUT_BUCKET_NAME);
    }

    @Test
    void shouldConvertBrageRecordToNvaPublicationWithCorrectCustomer() throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForRecord();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @ParameterizedTest(name = "shouldConvertBookToNvaPublication")
    @ValueSource(strings = {PART_OF_SERIES_VALUE_V1, PART_OF_SERIES_VALUE_V2,
        PART_OF_SERIES_VALUE_V3, PART_OF_SERIES_VALUE_V4, PART_OF_SERIES_VALUE_V5})
    void shouldConvertBookToNvaPublication(String seriesNumber) throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForBook(seriesNumber);
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldNotConvertSeriesNumberWithoutNumber() throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForBookWithoutValidSeriesNumber(PART_OF_SERIES_VALUE_V6);
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertMapToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForMap();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertReportToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForReport();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertResearchReportToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForResearchReport();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertReportWorkingPaperToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_REPORT_WORKING_PAPER)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertBachelorToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForBachelor();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertMasterToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForMaster();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertPhdToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForPhd();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertDatasetToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForDataset();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertJournalArticleToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForJournalArticle();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertJournalArticleWithoutJournalIdToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = buildGeneratorForJournalArticleWithoutId();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertScientificArticleToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_SCIENTIFIC_ARTICLE)
                                                 .withJournalTitle("Journal")
                                                 .withJournalId("id")
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertFeatureArticleToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_FEATURE_ARTICLE)
                                                 .withJournalId("journal")
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertLectureToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_LECTURE)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertChapterToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_CHAPTER)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertScientificChapterToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_SCIENTIFIC_CHAPTER)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertStudentPaperToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_STUDENT_PAPER_OTHERS)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertOtherStudentWorkToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_OTHER_STUDENT_WORK)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertDesignProductToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_DESIGN_PRODUCT)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertPlanOrBluePrintToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_PLAN_OR_BLUEPRINT)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertMusicToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_MUSIC)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertScientificMonographToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_SCIENTIFIC_MONOGRAPH)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertWhenPublicationContextIsNull() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withIsbn(randomIsbn10())
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNotSupportedInPublicationContext() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_SOFTWARE)
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNotSupportedInPublicationInstance() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_SOFTWARE)
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldThrowExceptionWhenInvalidBrageRecordIsProvided() throws IOException {
        var s3Event = createNewInvalidBrageRecordEvent();
        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldPersistPublicationInDatabase() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(resourceService.getPublicationsThatHasBeenCreatedByImportedEntry(), hasSize(1));
        assertThat(resourceService.getPublicationsThatHasBeenCreatedByImportedEntry(), contains(actualPublication));
    }

    @Test
    void shouldTryToPersistPublicationInDatabaseSeveralTimesWhenResourceServiceIsThrowingException()
        throws IOException {
        var fakeResourceServiceThrowingException = new FakeResourceServiceThrowingException();
        this.handler = new BrageEntryEventConsumer(s3Client, fakeResourceServiceThrowingException);
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, CONTEXT));
        assertThat(fakeResourceServiceThrowingException.getAttemtsToSavePublication(),
                   is(equalTo(BrageEntryEventConsumer.MAX_EFFORTS + 1)));
    }

    @Test
    void shouldThrowExceptionIfItCannotCopyAssociatedArtifacts() throws IOException {
        this.s3Client = new FakeS3ClientThrowingExceptionWhenCopying();
        this.s3Driver = new S3Driver(s3Client, INPUT_BUCKET_NAME);
        this.handler = new BrageEntryEventConsumer(s3Client, resourceService);
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .withResourceContent(createResourceContent())
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());

        Executable action = () -> handler.handleRequest(s3Event, CONTEXT);
        var exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(COULD_NOT_COPY_ASSOCIATED_ARTEFACT_EXCEPTION_MESSAGE));
    }

    @Test
    void shouldCopyAssociatedArtifactsToResourceStorage() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withResourceContent(createResourceContent())
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var s3Event = createNewBrageRecordEventWithSpecifiedObjectKey(nvaBrageMigrationDataGenerator.getBrageRecord()
            , "my/path/some.json");
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
    void shouldSaveErrorReportInS3ContainingTheOriginalInputData() throws IOException {
        this.handler = new BrageEntryEventConsumer(s3Client, new FakeResourceServiceThrowingException());
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
    void shouldSaveHandleAndResourceIdentifierReportInS3() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        var actualStoredHandleString = extractActualHandleReportFromS3Client(s3Event, actualPublication);
        assertThat(actualStoredHandleString,
                   is(equalTo(nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication().getHandle().toString())));
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForJournalArticleWithoutId() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_JOURNAL_ARTICLE)
                   .withSpatialCoverage(List.of("Norway"))
                   .withJournalTitle("Some Very Popular Journal")
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

    private NvaBrageMigrationDataGenerator buildGeneratorForResearchReport() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_RESEARCH_REPORT)
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForReport() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_REPORT)
                   .withSeries("someSeries")
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForMap() {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_MAP)
                   .withPublisherId("someId")
                   .build();
    }

    private NvaBrageMigrationDataGenerator buildGeneratorForBookWithoutValidSeriesNumber(String seriesNumber) {
        return new NvaBrageMigrationDataGenerator.Builder()
                   .withType(TYPE_BOOK)
                   .withSeriesNumberRecord(seriesNumber)
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
            ERROR_BUCKET_PATH + PATH_SEPERATOR + timestamp + PATH_SEPERATOR + exception.getClass().getSimpleName() +
            PATH_SEPERATOR + fileUri.getLastPathElement());
    }

    private String extractFilename(S3Event event) {
        return event.getRecords().get(0).getS3().getObject().getKey();
    }

    private ResourceContent createResourceContent() {
        var file = new ContentFile(FILENAME,
                                   BundleType.ORIGINAL,
                                   "description",
                                   UUID,
                                   new License("someLicense",
                                               new NvaLicense(LICENSE_IDENTIFIER, Map.of(NORWEGIAN_BOKMAAL,
                                                                                         LICENSE_IDENTIFIER.getValue()))),
                                   EMBARGO_DATE);

        return new ResourceContent(Collections.singletonList(file));
    }

    private List<AssociatedArtifact> createCorrespondingAssociatedArtifacts() {
        return List.of(File.builder()
                           .withIdentifier(UUID)
                           .withLicense(new no.unit.nva.model.associatedartifacts.file.License.Builder()
                                            .withIdentifier(String.valueOf(LICENSE_IDENTIFIER.getValue()))
                                            .withLabels(Map.of(NORWEGIAN_BOKMAAL,
                                                               LICENSE_IDENTIFIER.getValue()))
                                            .build())
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

    private S3Event createNewBrageRecordEvent(Record record) throws IOException {
        var recordAsJson = JsonUtils.dtoObjectMapper.writeValueAsString(record);
        var uri = s3Driver.insertFile(randomS3Path(), recordAsJson);
        return createS3Event(uri);
    }

    private S3Event createNewBrageRecordEventWithSpecifiedObjectKey(Record record, String path) throws IOException {
        var recordAsJson = JsonUtils.dtoObjectMapper.writeValueAsString(record);
        var uri = s3Driver.insertFile(UnixPath.of(path), recordAsJson);
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


