package no.unit.nva.cristin.lambda;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.CristinDataGenerator.randomString;
import static no.unit.nva.cristin.CristinImportConfig.eventHandlerObjectMapper;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERRORS_FOLDER;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERROR_SAVING_CRISTIN_RESULT;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.JSON;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.UNKNOWN_CRISTIN_ID_ERROR_REPORT_PREFIX;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_PUBLICATIONS_OWNER;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.UNIT_CUSTOMER_ID;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.PATH_CUSTOMER;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.Identifiable;
import no.unit.nva.cristin.mapper.nva.exceptions.AffiliationWithoutRoleException;
import no.unit.nva.cristin.mapper.nva.exceptions.ContributorWithoutAffiliationException;
import no.unit.nva.cristin.mapper.nva.exceptions.InvalidIsbnRuntimeException;
import no.unit.nva.cristin.mapper.nva.exceptions.InvalidIssnRuntimeException;
import no.unit.nva.cristin.mapper.nva.exceptions.MissingContributorsException;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedMainCategoryException;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedSecondaryCategoryException;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.s3imports.UriWrapper;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.SingletonCollector;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class CristinEntryEventConsumerTest extends AbstractCristinImportTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final String DETAIL_FIELD = "detail";
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final String RESOURCE_EXCEPTION_MESSAGE = "resourceExceptionMessage";
    public static final JavaType IMPORT_RESULT_JAVA_TYPE = constructImportResultJavaType();
    public static final String IGNORED_VALUE = "someBucket";
    public static final UnixPath LIST_ALL_FILES = UnixPath.of("");
    public static final String ID_FIELD_NAME = "id";
    public static final String NOT_IMPORTANT = "someBucketName";
    public static final String UNKNOWN_PROPERTY_NAME_IN_RESOURCE_FILE_WITH_UNKNOWN_PROPERTY = "unknownProperty";

    private CristinEntryEventConsumer handler;
    private ByteArrayOutputStream outputStream;
    private ResourceService resourceService;
    private FakeS3Client s3Client;

    @BeforeEach
    public void init() {
        super.init();
        resourceService = new ResourceService(super.client, Clock.systemDefaultZone());
        s3Client = new FakeS3Client(new ConcurrentHashMap<>());
        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        outputStream = new ByteArrayOutputStream();
        super.testingData = CristinDataGenerator.singleRandomObjectAsString();
    }

    @Test
    public void handlerLogsErrorWhenFailingToStorePublicationToDynamo() {

        final TestAppender appender = LogUtils.getTestingAppender(CristinEntryEventConsumer.class);
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        CristinObject cristinObject = CristinDataGenerator.randomObject();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event = CristinDataGenerator.toAwsEvent(cristinObject);
        InputStream input = stringToStream(event.toJsonString());

        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        runWithoutThrowingException(action);

        Integer cristinIdentifier = cristinObject.getId();
        assertThat(appender.getMessages(), containsString(ERROR_SAVING_CRISTIN_RESULT + cristinIdentifier));
        assertThat(appender.getMessages(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    public void handlerReturnsAnNvaPublicationEntryWhenInputIsEventWithCristinResult() throws JsonProcessingException {
        CristinObject cristinObject = CristinDataGenerator.randomObject();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = CristinDataGenerator.toAwsEvent(cristinObject);
        InputStream input = stringToStream(awsEvent.toJsonString());
        handler.handleRequest(input, outputStream, CONTEXT);
        String json = outputStream.toString();
        Publication actualPublication = eventHandlerObjectMapper.readValue(json, Publication.class);

        Publication expectedPublication = generatePublicationFromResource(awsEvent.toJsonString()).toPublication();
        injectValuesThatAreCreatedWhenSavingInDynamo(actualPublication, expectedPublication);

        assertThat(actualPublication, is(equalTo(expectedPublication)));
        assertThat(actualPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    public void handlerSavesPublicationToDynamoDbWhenInputIsEventWithCristinResult() {
        CristinObject cristinObject = CristinDataGenerator.randomObject();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = CristinDataGenerator.toAwsEvent(cristinObject);
        InputStream input = stringToStream(awsEvent.toJsonString());

        handler.handleRequest(input, outputStream, CONTEXT);

        UserInstance userInstance = createExpectedPublicationOwner();
        Publication actualPublication = fetchPublicationDirectlyFromDatabase(userInstance);
        Publication expectedPublication = cristinObject.toPublication();
        injectValuesThatAreCreatedWhenSavingInDynamo(actualPublication, expectedPublication);

        Diff diff = JAVERS.compare(expectedPublication, actualPublication);
        assertThat(diff.prettyPrint(), actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    public void handlerThrowsExceptionWhenSubtopicIsNotAsExpected() throws JsonProcessingException {
        String unexpectedSubtopic = randomString();
        String input = eventWithInvalidSubtopic(unexpectedSubtopic);

        Executable action = () -> handler.handleRequest(stringToStream(input), outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(CristinEntryEventConsumer.EVENT_SUBTOPIC));
        assertThat(exception.getMessage(), containsString(unexpectedSubtopic));
    }

    @Test
    public void handlerThrowsExceptionWhenFailingToStorePublicationToDynamo() {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client);

        CristinObject cristinObject = CristinDataGenerator.randomObject();
        InputStream event = stringToStream(CristinDataGenerator.toAwsEvent(cristinObject).toJsonString());
        Executable action = () -> handler.handleRequest(event, outputStream, CONTEXT);

        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    public void handlerSavesErrorReportInS3OutsideTheInputFolderAndWithFilenameTheObjectId()
        throws JsonProcessingException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        CristinObject cristinObject = CristinDataGenerator.randomObject();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event = CristinDataGenerator.toAwsEvent(cristinObject);
        InputStream inputStream = stringToStream(event.toJsonString());
        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        Exception throwException = assertThrows(RuntimeException.class, action);

        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> actualReport =
            extractActualReportFromS3Client(event, throwException);
        assertThat(actualReport, is(not(nullValue())));
    }

    @Test
    public void handlerThrowsExceptionWhenMainCategoryTypeIsNotKnown() throws JsonProcessingException {
        JsonNode inputData = CristinDataGenerator.objectWithCustomMainCategory(randomString());
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = CristinDataGenerator.toAwsEvent(inputData);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        UnsupportedMainCategoryException exception = assertThrows(UnsupportedMainCategoryException.class, action);
        assertThat(exception.getMessage(), is(equalTo(UnsupportedMainCategoryException.ERROR_PARSING_MAIN_CATEGORY)));
    }

    @Test
    public void handlerSavesErrorReportFileInS3ContainingInputDataWhenFailingToStorePublicationToDynamo()
        throws JsonProcessingException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();

        CristinObject cristinObject = CristinDataGenerator.randomObject();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event = CristinDataGenerator.toAwsEvent(cristinObject);
        InputStream input = stringToStream(event.toJsonString());

        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);

        RuntimeException thrownException = assertThrows(RuntimeException.class, action);
        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> expectedReport =
            constructExpectedErrorReport(thrownException, event);

        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> actualReport =
            extractActualReportFromS3Client(event, thrownException);

        JsonNode expectedReportJson = eventHandlerObjectMapper.convertValue(expectedReport, JsonNode.class);
        JsonNode actualReportJson = eventHandlerObjectMapper.convertValue(actualReport, JsonNode.class);
        assertThat(actualReportJson, is(equalTo(expectedReportJson)));
    }

    @Test
    public void handlerSavesReportInS3ContainingTheOriginalInputData() throws JsonProcessingException {

        JsonNode inputData = CristinDataGenerator.objectWithCustomMainCategory(randomString());
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = CristinDataGenerator.toAwsEvent(inputData);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        UnsupportedMainCategoryException thrownException = assertThrows(UnsupportedMainCategoryException.class, action);

        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> actualReport =
            extractActualReportFromS3Client(awsEvent, thrownException);

        assertThat(actualReport.getInput().getDetail().getContents(), is(equalTo(inputData)));
    }

    @Test
    public void handlerThrowsExceptionWhenSecondaryCategoryTypeIsNotKnown() throws JsonProcessingException {

        JsonNode inputData = CristinDataGenerator.objectWithCustomSecondaryCategory(randomString());
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = CristinDataGenerator.toAwsEvent(inputData);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        UnsupportedSecondaryCategoryException exception =
            assertThrows(UnsupportedSecondaryCategoryException.class, action);

        assertThat(exception.getMessage(),
                   containsString(UnsupportedSecondaryCategoryException.ERROR_PARSING_SECONDARY_CATEGORY));
    }

    @Test
    public void handlerThrowsInvalidIsbnRuntimeExceptionWhenTheIsbnIsInvalid() throws JsonProcessingException {
        JsonNode cristinObjectWithInvalidIsbn = CristinDataGenerator.objectWithInvalidIsbn();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent =
            CristinDataGenerator.toAwsEvent(cristinObjectWithInvalidIsbn);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);

        assertThrows(InvalidIsbnRuntimeException.class, action);
    }

    @Test
    public void handlerThrowsInvalidIssnRuntimeExceptionWhenTheBookIssnIsInvalid() throws JsonProcessingException {
        JsonNode cristinObjectWithInvalidIssn = CristinDataGenerator.bookObjectWithInvalidIssn();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent =
            CristinDataGenerator.toAwsEvent(cristinObjectWithInvalidIssn);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);

        assertThrows(InvalidIssnRuntimeException.class, action);
    }

    @Test
    public void handlerThrowsInvalidIssnRuntimeExceptionWhenTheJournalIssnIsInvalid() throws JsonProcessingException {
        JsonNode cristinObjectWithInvalidIssn = CristinDataGenerator.journalObjectWithInvalidIssn();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent =
            CristinDataGenerator.toAwsEvent(cristinObjectWithInvalidIssn);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());
        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);

        assertThrows(InvalidIssnRuntimeException.class, action);
    }

    @Test
    public void handlerThrowsMissingContributorsRuntimeExceptionWhenTheCristinObjectHasNoContributors()
        throws JsonProcessingException {
        JsonNode cristinObjectWithoutContributors = CristinDataGenerator.objectWithoutContributors();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent =
            CristinDataGenerator.toAwsEvent(cristinObjectWithoutContributors);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);

        assertThrows(MissingContributorsException.class, action);
    }

    @Test
    public void handlerThrowContributorWithoutAffiliationExceptionWhenTheCristinObjectHasContributorWithoutAffiliation()
        throws JsonProcessingException {
        JsonNode cristinObjectWithoutAffiliations = CristinDataGenerator.objectWithContributorsWithoutAffiliation();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent =
            CristinDataGenerator.toAwsEvent(cristinObjectWithoutAffiliations);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);

        assertThrows(ContributorWithoutAffiliationException.class, action);
    }

    @Test
    public void handlerThrowsAffiliationWithoutARoleExceptionWhenTheCristinObjectHasAffiliationsWithoutRoles()
        throws JsonProcessingException {
        JsonNode cristinObjectWithAffiliationWithoutRoles = CristinDataGenerator.objectWithAffiliationWithoutRole();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent =
            CristinDataGenerator.toAwsEvent(cristinObjectWithAffiliationWithoutRoles);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);

        assertThrows(AffiliationWithoutRoleException.class, action);
    }

    @Test
    public void handlerCreatesFileWithCustomNameWhenCristinIdIsNotFound() throws JsonProcessingException {
        JsonNode cristinObjectWithoutId = CristinDataGenerator.objectWithoutId();

        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent =
            CristinDataGenerator.toAwsEvent(cristinObjectWithoutId);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        runWithoutThrowingException(action);

        S3Driver s3Driver = new S3Driver(s3Client, IGNORED_VALUE);
        UnixPath errorReportFile = s3Driver.listAllFiles(LIST_ALL_FILES)
            .stream()
            .collect(SingletonCollector.collect());
        String errorReport = s3Driver.getFile(errorReportFile);

        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> actualReport =
            eventHandlerObjectMapper.readValue(errorReport, IMPORT_RESULT_JAVA_TYPE);

        assertThat(errorReportFile.toString(), containsString(UNKNOWN_CRISTIN_ID_ERROR_REPORT_PREFIX));
        assertThat(actualReport.getInput().getDetail().getContents(), is(equalTo(cristinObjectWithoutId)));
    }

    @Test
    public void savesFileInInputFolderErrorTimestampExceptionNameInputFileLocationInputFileWhenFailingToSaveInDynamo()
        throws Throwable {
        CristinObject cristinObject = CristinDataGenerator.randomObject();
        JsonNode cristinObjectWithCustomSecondaryCategory =
            CristinDataGenerator.injectCustomSecondaryCategoryIntoCristinObject(
                cristinObject, randomString());
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent =
            CristinDataGenerator.toAwsEvent(cristinObjectWithCustomSecondaryCategory);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());
        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);

        UnsupportedSecondaryCategoryException exception =
            assertThrows(UnsupportedSecondaryCategoryException.class, action);

        S3Driver s3Driver = new S3Driver(s3Client, "bucket");
        String expectedFilePath = awsEvent.getDetail().getFileUri().getPath();
        Instant expectedTimestamp = awsEvent.getDetail().getTimestamp();
        String exceptionName = exception.getClass().getSimpleName();
        String fileIdWithEnding = cristinObject.getId().toString() + JSON;
        UnixPath expectedErrorFileLocation = UnixPath.of(ERRORS_FOLDER,
                                                         timestampToString(expectedTimestamp),
                                                         exceptionName,
                                                         expectedFilePath,
                                                         fileIdWithEnding);

        String actualErrorFile = s3Driver.getFile(expectedErrorFileLocation);
        assertThat(actualErrorFile, is(not(nullValue())));
    }

    @Test
    public void shouldThrowExceptionWhenInputContainsUnknownProperty() throws JsonProcessingException {
        String input = IoUtils.stringFromResources(Path.of("cristin_entry_with_unknown_property.json"));

        AwsEventBridgeEvent<FileContentsEvent<Identifiable>> event = parseEventAsIdentifieableObject(input);
        Executable action = () -> handler.handleRequest(stringToStream(input), outputStream, CONTEXT);
        RuntimeException exception = assertThrows(RuntimeException.class, action);

        UnixPath expectedFilePath = constructExpectedFilePathForEntryWithUnkownFields(event, exception);

        S3Driver s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        String file = s3Driver.getFile(expectedFilePath);

        assertThat(file, is(not(emptyString())));
        assertThat(file, containsString(UNKNOWN_PROPERTY_NAME_IN_RESOURCE_FILE_WITH_UNKNOWN_PROPERTY));
    }

    @Test
    public void handleRequestDoesNotThrowExceptionWhenInputDoesNotHaveUnknownProperties() {
        String input = IoUtils.stringFromResources(Path.of("cristin_entry_of_known_type_with_all_fields.json"));
        Executable action = () -> handler.handleRequest(stringToStream(input), outputStream, CONTEXT);
        assertDoesNotThrow(action);
    }


    private static JavaType constructImportResultJavaType() {

        JavaType fileContentsType = eventHandlerObjectMapper.getTypeFactory()
            .constructParametricType(FileContentsEvent.class, JsonNode.class);
        JavaType eventType = eventHandlerObjectMapper.getTypeFactory()
            .constructParametricType(AwsEventBridgeEvent.class, fileContentsType);
        return eventHandlerObjectMapper.getTypeFactory()
            .constructParametricType(ImportResult.class, eventType);
    }


    private UnixPath constructExpectedFilePathForEntryWithUnkownFields(
        AwsEventBridgeEvent<FileContentsEvent<Identifiable>> event,
        RuntimeException exception) {
        return UnixPath.of(ERRORS_FOLDER)
            .addChild(timestampToString(event.getDetail().getTimestamp()))
            .addChild(exception.getClass().getSimpleName())
            .addChild(event.getDetail().getFileUri().getPath())
            .addChild(event.getDetail().getContents().getId() + JSON);
    }

    private AwsEventBridgeEvent<FileContentsEvent<Identifiable>> parseEventAsIdentifieableObject(String input)
        throws JsonProcessingException {
        JavaType fileContentsType = eventHandlerObjectMapper.getTypeFactory()
            .constructParametricType(FileContentsEvent.class, Identifiable.class);
        JavaType eventType = eventHandlerObjectMapper.getTypeFactory()
            .constructParametricType(AwsEventBridgeEvent.class, fileContentsType);
        return eventHandlerObjectMapper.readValue(input, eventType);
    }

    private void injectValuesThatAreCreatedWhenSavingInDynamo(Publication actualPublication,
                                                              Publication expectedPublication) {

        //NVA identifier is not known until the entry has been saved in the NVA database.
        expectedPublication.setIdentifier(actualPublication.getIdentifier());
        expectedPublication.setStatus(PublicationStatus.PUBLISHED);
        expectedPublication.setCreatedDate(actualPublication.getCreatedDate());
        expectedPublication.setModifiedDate(actualPublication.getModifiedDate());
        expectedPublication.setPublishedDate(actualPublication.getPublishedDate());
    }

    private Publication fetchPublicationDirectlyFromDatabase(UserInstance userInstance) {
        return resourceService.getPublicationsByOwner(userInstance)
            .stream()
            .collect(SingletonCollector.collect());
    }

    private UriWrapper constructErrorFileUri(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent,
                                             Exception exception) {

        String cristinObjectId = awsEvent.getDetail().getContents().get(ID_FIELD_NAME).asText();
        String errorReportFilename = cristinObjectId + JSON;
        UriWrapper inputFile = new UriWrapper(awsEvent.getDetail().getFileUri());
        Instant timestamp = awsEvent.getDetail().getTimestamp();
        UriWrapper bucket = inputFile.getHost();
        return bucket.addChild(ERRORS_FOLDER)
            .addChild(timestampToString(timestamp))
            .addChild(exception.getClass().getSimpleName())
            .addChild(inputFile.getPath())
            .addChild(errorReportFilename);
    }

    private void runWithoutThrowingException(Executable action) {
        assertThrows(RuntimeException.class, action);
    }

    private ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> extractActualReportFromS3Client(
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event, Exception exception) throws JsonProcessingException {
        UriWrapper errorFileUri = constructErrorFileUri(event, exception);
        S3Driver s3Driver = new S3Driver(s3Client, errorFileUri.getUri().getHost());
        String content = s3Driver.getFile(errorFileUri.toS3bucketPath());
        return eventHandlerObjectMapper.readValue(content, IMPORT_RESULT_JAVA_TYPE);
    }

    private ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> constructExpectedErrorReport(
        Throwable thrownException,
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        return ImportResult.reportFailure(event, (Exception) thrownException);
    }

    private ResourceService resourceServiceThrowingExceptionWhenSavingResource() {
        return new ResourceService(client,  Clock.systemDefaultZone()) {
            @Override
            public Publication createPublicationFromImportedEntry(Publication publication) {
                throw new RuntimeException(RESOURCE_EXCEPTION_MESSAGE);
            }
        };
    }

    private String eventWithInvalidSubtopic(String invalidSubtopic) throws JsonProcessingException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventWIthInvalidSubtopic =
            CristinDataGenerator.toAwsEvent(cristinObject, invalidSubtopic);
        return eventHandlerObjectMapper.writeValueAsString(eventWIthInvalidSubtopic);
    }

    private UserInstance createExpectedPublicationOwner() {
        UriWrapper customerId = new UriWrapper(NVA_API_DOMAIN).addChild(PATH_CUSTOMER, UNIT_CUSTOMER_ID);
        return UserInstance.create(HARDCODED_PUBLICATIONS_OWNER, customerId.getUri());
    }

    private CristinObject generatePublicationFromResource(String input) throws JsonProcessingException {
        JsonNode jsonNode = eventHandlerObjectMapper.readTree(input);
        String detail = jsonNode.get(DETAIL_FIELD).toString();
        FileContentsEvent<CristinObject> eventDetails = FileContentsEvent.fromJson(detail, CristinObject.class);
        CristinObject cristinObject = eventDetails.getContents();
        assert nonNull(cristinObject.getId()); //java assertion produces Error not exception
        return cristinObject;
    }
}