package no.sikt.nva.brage.migration.lambda;

import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import no.sikt.nva.brage.migration.AssociatedArtifactException;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.record.Pages;
import no.sikt.nva.brage.migration.record.PublicationDate;
import no.sikt.nva.brage.migration.record.PublicationDateNva.Builder;
import no.sikt.nva.brage.migration.record.Range;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.Type;
import no.sikt.nva.brage.migration.testutils.FakeS3ClientThrowingExceptionWhenCopying;
import no.sikt.nva.brage.migration.testutils.FakeS3cClientWithCopyObjectSupport;
import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Organization;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

public class BrageEntryEventConsumerTest {

    public static final String PART_OF_SERIES_VALUE_V1 = "SOMESERIES;42";
    public static final String PART_OF_SERIES_VALUE_V2 = "SOMESERIES;42:2022";
    public static final String PART_OF_SERIES_VALUE_V3 = "SOMESERIES;2022:42";
    public static final String PART_OF_SERIES_VALUE_V4 = "SOMESERIES;2022/42";
    public static final String PART_OF_SERIES_VALUE_V5 = "SOMESERIES;42/2022";
    public static final String EXPECTED_SERIES_NUMBER = "42";
    public static final Context CONTEXT = mock(Context.class);
    public static final long SOME_FILE_SIZE = 100L;
    public static final Type TYPE_BOOK = new Type(List.of(NvaType.BOOK.getValue()), NvaType.BOOK.getValue());
    public static final Type TYPE_MAP = new Type(List.of(NvaType.MAP.getValue()), NvaType.MAP.getValue());
    public static final Type TYPE_DATASET = new Type(List.of(NvaType.DATASET.getValue()), NvaType.DATASET.getValue());
    public static final PublicationDate PUBLICATION_DATE = new PublicationDate("2020",
                                                                               new Builder().withYear("2020").build());
    public static final Organization TEST_ORGANIZATION = new Organization.Builder().withId(URI.create(
        "https://api.nva.unit.no/customer/test")).build();
    public static final String TEST_CUSTOMER = "TEST";
    private static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    private static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    private static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    private BrageEntryEventConsumer handler;
    private S3Driver s3Driver;
    private FakeS3Client s3Client;

    @BeforeEach
    void init() {
        s3Client = new FakeS3cClientWithCopyObjectSupport();
        this.handler = new BrageEntryEventConsumer(s3Client);
        s3Driver = new S3Driver(s3Client, "ignored");
    }

    @Test
    void shouldConvertBrageRecordToNvaPublicationWithCorrectCustomer() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_BOOK)
                                                 .withCustomer(TEST_CUSTOMER)
                                                 .withDescription(null)
                                                 .withAbstracts(null)
                                                 .withOrganization(TEST_ORGANIZATION)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @ParameterizedTest
    @ValueSource(strings = {PART_OF_SERIES_VALUE_V1, PART_OF_SERIES_VALUE_V2,
        PART_OF_SERIES_VALUE_V3, PART_OF_SERIES_VALUE_V4, PART_OF_SERIES_VALUE_V5})
    void shouldConvertBookToNvaPublication(String seriesNumber) throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_BOOK)
                                                 .withDescription(List.of("Description"))
                                                 .withAbstracts(List.of("Abstract"))
                                                 .withSeriesNumberRecord(seriesNumber)
                                                 .withSeriesNumberPublication(EXPECTED_SERIES_NUMBER)
                                                 .withPublicationDate(PUBLICATION_DATE)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertMapToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_MAP)
                                                 .withDescription(Collections.emptyList())
                                                 .withAbstracts(Collections.emptyList())
                                                 .withPages(new Pages("46 s.", new Range("5", "10"), "5"))
                                                 .withMonographPages(
                                                     new MonographPages.Builder().withPages("5").build())
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldConvertDatasetToNvaPublication() throws IOException {
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withType(TYPE_DATASET)
                                                 .withSpatialCoverage(List.of("Norway"))
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
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var expectedPublication = nvaBrageMigrationDataGenerator.getCorrespondingNvaPublication();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        var actualPublication = handler.handleRequest(s3Event, CONTEXT);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldThrowExceptionWhenInvalidBrageRecordIsProvided() throws IOException {
        var s3Event = createNewInvalidBrageRecordEvent();
        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldThrowExceptionIfItCannotCopyAssociatedArtifacts() throws IOException {
        s3Client = new FakeS3ClientThrowingExceptionWhenCopying();
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var s3Event = createNewBrageRecordEvent(nvaBrageMigrationDataGenerator.getBrageRecord());
        assertThrows(AssociatedArtifactException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldCopyAssociatedArtifactsToResourceStorage() throws IOException {
        s3Client = new FakeS3ClientThrowingExceptionWhenCopying();
        var nvaBrageMigrationDataGenerator = new NvaBrageMigrationDataGenerator.Builder()
                                                 .withPublishedDate(null)
                                                 .withType(TYPE_BOOK)
                                                 .build();
        var s3Event = createNewBrageRecordEventWithSpecifiedObjectKey(nvaBrageMigrationDataGenerator.getBrageRecord()
            , "my/path/some.json");
        var objectKey = "";
        var expectedDopyObjRequest = CopyObjectRequest.builder()
                                 .sourceBucket("ignored")
                                 .destinationBucket(new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME"))
                                 .sourceKey("my/path/" + objectKey)
                                 .destinationKey(objectKey)
                                 .build();
        handler.handleRequest(s3Event, CONTEXT);
        var fakeS3cClientWithCopyObjectSupport = (FakeS3cClientWithCopyObjectSupport) s3Client;
        var actualCopyObjectRequests = fakeS3cClientWithCopyObjectSupport.getCopyObjectRequestList();
        assertThat(actualCopyObjectRequests, hasSize(1));
        assertThat(actualCopyObjectRequests, contains(expectedDopyObjRequest));
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
        var bucket = new S3BucketEntity(randomString(), EMPTY_USER_IDENTITY, randomString());
        var object = new S3ObjectEntity(expectedObjectKey,
                                        SOME_FILE_SIZE,
                                        randomString(),
                                        randomString(),
                                        randomString());
        var schemaVersion = randomString();
        return new S3Entity(randomString(), bucket, object, schemaVersion);
    }
}
