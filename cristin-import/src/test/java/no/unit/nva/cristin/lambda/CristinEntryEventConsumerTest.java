package no.unit.nva.cristin.lambda;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.CristinDataGenerator.randomString;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERRORS_FOLDER;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERROR_SAVING_CRISTIN_RESULT;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.FILE_ENDING;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.UNKNOWN_CRISTIN_ID_ERROR_REPORT_PREFIX;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_NVA_CUSTOMER;
import static nva.commons.core.JsonUtils.objectMapperNoEmpty;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinMapper;
import no.unit.nva.cristin.mapper.CristinObject;
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
import no.unit.nva.testutils.IoUtils;
import nva.commons.core.SingletonCollector;
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
    public static final Path LIST_ALL_FILES = Path.of("");
    public static final String ID_FIELD_NAME = "id";
    private CristinDataGenerator cristinDataGenerator;

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
        cristinDataGenerator = new CristinDataGenerator();
        super.testingData = cristinDataGenerator.singleRandomObjectAsString();
    }

    @Test
    public void handlerLogsErrorWhenFailingToStorePublicationToDynamo() {

        final TestAppender appender = LogUtils.getTestingAppender(CristinEntryEventConsumer.class);
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        CristinObject cristinObject = cristinDataGenerator.randomObject();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event = cristinDataGenerator.toAwsEvent(cristinObject);
        InputStream input = stringToStream(event.toJsonString());

        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        runWithoutThrowingException(action);

        Integer cristinIdentifier = cristinObject.getId();
        assertThat(appender.getMessages(), containsString(ERROR_SAVING_CRISTIN_RESULT + cristinIdentifier));
        assertThat(appender.getMessages(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    public void handlerReturnsAnNvaPublicationEntryWhenInputIsEventWithCristinResult() throws JsonProcessingException {
        CristinObject cristinObject = cristinDataGenerator.randomObject();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = cristinDataGenerator.toAwsEvent(cristinObject);
        InputStream input = stringToStream(awsEvent.toJsonString());
        handler.handleRequest(input, outputStream, CONTEXT);
        String json = outputStream.toString();
        Publication actualPublication = objectMapperNoEmpty.readValue(json, Publication.class);

        Publication expectedPublication = generatePublicationFromResource(awsEvent.toJsonString()).toPublication();
        injectValuesThatAreCreatedWhenSavingInDynamo(awsEvent, actualPublication, expectedPublication);

        assertThat(actualPublication, is(equalTo(expectedPublication)));
        assertThat(actualPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    public void handlerSavesPublicationToDynamoDbWhenInputIsEventWithCristinResult() {
        CristinObject cristinObject = cristinDataGenerator.randomObject();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = cristinDataGenerator.toAwsEvent(cristinObject);
        InputStream input = stringToStream(awsEvent.toJsonString());

        handler.handleRequest(input, outputStream, CONTEXT);

        UserInstance userInstance = createExpectedPublicationOwner(awsEvent);
        Publication actualPublication = fetchPublicationDirectlyFromDatabase(userInstance);
        Publication expectedPublication = cristinObject.toPublication();
        injectValuesThatAreCreatedWhenSavingInDynamo(awsEvent, actualPublication, expectedPublication);

        Diff diff = JAVERS.compare(expectedPublication, actualPublication);
        assertThat(diff.prettyPrint(), actualPublication, is(equalTo(expectedPublication)));
    }

    private void injectValuesThatAreCreatedWhenSavingInDynamo(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent,
                                                              Publication actualPublication,
                                                              Publication expectedPublication) {
        //publications owner is for now set in import time and is available as extra information in the event.
        expectedPublication.setOwner(awsEvent.getDetail().getPublicationsOwner());
        //NVA identifier is not known until the entry has been saved in the NVA database.
        expectedPublication.setIdentifier(actualPublication.getIdentifier());
        expectedPublication.setStatus(PublicationStatus.PUBLISHED);
        expectedPublication.setCreatedDate(actualPublication.getCreatedDate());
        expectedPublication.setModifiedDate(actualPublication.getModifiedDate());
        expectedPublication.setPublishedDate(actualPublication.getPublishedDate());
    }

    @Test
    public void handlerSavesPublicationWithOwnerBeingTheSuppliedByTheEventOwner() {
        CristinObject cristinObject = cristinDataGenerator.randomObject();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = cristinDataGenerator.toAwsEvent(cristinObject);
        InputStream input = stringToStream(awsEvent.toJsonString());

        handler.handleRequest(input, outputStream, CONTEXT);

        UserInstance userInstance = createExpectedPublicationOwner(awsEvent);
        Publication actualPublication = fetchPublicationDirectlyFromDatabase(userInstance);
        String actualPublicationOwner = actualPublication.getOwner();

        assertThat(actualPublicationOwner, is(equalTo(awsEvent.getDetail().getPublicationsOwner())));
    }

    @Test
    public void handlerThrowsExceptionWhenInputDetailTypeIsNotTheExpected() throws JsonProcessingException {
        String unexpectedDetailType = "unexpectedDetailType";
        String input = eventWithInvalidDetailType(unexpectedDetailType);

        Executable action = () -> handler.handleRequest(stringToStream(input), outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(CristinEntryEventConsumer.EVENT_DETAIL_TYPE));
        assertThat(exception.getMessage(), containsString(unexpectedDetailType));
    }

    @Test
    public void handlerThrowsExceptionWhenFailingToStorePublicationToDynamo() {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client);

        CristinObject cristinObject = cristinDataGenerator.randomObject();
        InputStream event = stringToStream(cristinDataGenerator.toAwsEvent(cristinObject).toJsonString());
        Integer cristinIdentifier = Optional.of(cristinObject)
                                        .map(CristinObject::getId)
                                        .orElseThrow();
        Executable action = () -> handler.handleRequest(event, outputStream, CONTEXT);

        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(ERROR_SAVING_CRISTIN_RESULT + cristinIdentifier));
        assertThat(exception.getCause().getMessage(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    public void handlerSavesErrorReportInS3InTheLocationIndicatedAsTheInputsFileLocationAndWithFilenameTheObjectId()
        throws JsonProcessingException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        CristinObject cristinObject = cristinDataGenerator.randomObject();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event = cristinDataGenerator.toAwsEvent(cristinObject);
        InputStream inputStream = stringToStream(event.toJsonString());
        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        assertThrows(RuntimeException.class, action);

        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> actualReport =
            extractActualReportFromS3Client(event);
        assertThat(actualReport, is(not(nullValue())));
    }

    @Test
    public void handlerThrowsExceptionWhenMainCategoryTypeIsNotKnown() throws JsonProcessingException {

        JsonNode inputData = cristinDataGenerator.objectWithCustomMainCategory(randomString());
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = cristinDataGenerator.toAwsEvent(inputData);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        RuntimeException exception = assertThrows(RuntimeException.class, action);

        Throwable cause = exception.getCause();
        assertThat(cause, is(instanceOf(UnsupportedOperationException.class)));
        assertThat(cause.getMessage(), is(equalTo(CristinMapper.ERROR_PARSING_MAIN_CATEGORY)));
    }

    @Test
    public void handlerSavesErrorReportFileInS3ContainingInputDataWhenFailingToStorePublicationToDynamo()
        throws JsonProcessingException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();

        CristinObject cristinObject = cristinDataGenerator.randomObject();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event = cristinDataGenerator.toAwsEvent(cristinObject);
        InputStream input = stringToStream(event.toJsonString());

        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);

        RuntimeException thrownException = assertThrows(RuntimeException.class, action);
        Exception cause = (Exception) thrownException.getCause();
        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> expectedReport =
            constructExpectedErrorReport(cause, event);

        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> actualReport =
            extractActualReportFromS3Client(event);

        JsonNode expectedReportJson = objectMapperNoEmpty.convertValue(expectedReport, JsonNode.class);
        JsonNode actualReportJson = objectMapperNoEmpty.convertValue(actualReport, JsonNode.class);
        assertThat(actualReportJson, is(equalTo(expectedReportJson)));
    }

    @Test
    public void handlerSavesReportInS3ContainingTheOriginalInputData() throws JsonProcessingException {

        JsonNode inputData = cristinDataGenerator.objectWithCustomMainCategory(randomString());
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = cristinDataGenerator.toAwsEvent(inputData);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        runWithoutThrowingException(action);

        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> actualReport =
            extractActualReportFromS3Client(awsEvent);

        assertThat(actualReport.getInput().getDetail().getContents(), is(equalTo(inputData)));
    }

    @Test
    public void handlerThrowsExceptionWhenSecondaryCategoryTypeIsNotKnown() throws JsonProcessingException {

        JsonNode inputData = cristinDataGenerator.objectWithCustomSecondaryCategory(randomString());
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = cristinDataGenerator.toAwsEvent(inputData);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        RuntimeException exception = assertThrows(RuntimeException.class, action);

        Throwable cause = exception.getCause();
        assertThat(cause, is(instanceOf(UnsupportedOperationException.class)));
        assertThat(cause.getMessage(), is(containsString(CristinMapper.ERROR_PARSING_SECONDARY_CATEGORY)));
    }

    @Test
    public void handlerCreatesFileWithCustomNameWhenCristinIdIsNotFound() throws JsonProcessingException {
        JsonNode cristinObjectWithoutId = cristinDataGenerator.objectWithoutId();

        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent =
            cristinDataGenerator.toAwsEvent(cristinObjectWithoutId);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        runWithoutThrowingException(action);

        S3Driver s3Driver = new S3Driver(s3Client, IGNORED_VALUE);
        String errorReportFile = s3Driver.listFiles(LIST_ALL_FILES)
                                     .stream()
                                     .collect(SingletonCollector.collect());
        String errorReport = s3Driver.getFile(errorReportFile);

        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> actualReport =
            objectMapperNoEmpty.readValue(errorReport, IMPORT_RESULT_JAVA_TYPE);

        assertThat(errorReportFile, containsString(UNKNOWN_CRISTIN_ID_ERROR_REPORT_PREFIX));
        assertThat(actualReport.getInput().getDetail().getContents(), is(equalTo(cristinObjectWithoutId)));
    }

    private static JavaType constructImportResultJavaType() {

        JavaType fileContentsType = objectMapperNoEmpty.getTypeFactory()
                                        .constructParametricType(FileContentsEvent.class, JsonNode.class);
        JavaType eventType = objectMapperNoEmpty.getTypeFactory()
                                 .constructParametricType(AwsEventBridgeEvent.class, fileContentsType);
        return objectMapperNoEmpty.getTypeFactory()
                   .constructParametricType(ImportResult.class, eventType);
    }

    private static AwsEventBridgeEvent<FileContentsEvent<JsonNode>> parseEvent(String input) {
        JavaType detailType = objectMapperNoEmpty.getTypeFactory().constructParametricType(FileContentsEvent.class,
                                                                                           JsonNode.class);

        JavaType eventType = objectMapperNoEmpty.getTypeFactory()
                                 .constructParametricType(AwsEventBridgeEvent.class, detailType);
        return attempt(() -> objectMapperNoEmpty
                                 .<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>>
                                      readValue(input, eventType)).orElseThrow();
    }

    private Publication fetchPublicationDirectlyFromDatabase(UserInstance userInstance) {
        return resourceService.getPublicationsByOwner(userInstance)
                   .stream()
                   .collect(SingletonCollector.collect());
    }

    private UriWrapper constructErrorFileUri(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent) {
        UriWrapper inputFileUri = new UriWrapper(awsEvent.getDetail().getFileUri());
        UriWrapper errorsFolder = inputFileUri.getParent().orElseThrow().addChild(Path.of(ERRORS_FOLDER));
        String cristinObjectId = awsEvent.getDetail().getContents().get(ID_FIELD_NAME).asText();
        String filename = cristinObjectId + FILE_ENDING;
        return errorsFolder.addChild(Path.of(filename));
    }

    private void runWithoutThrowingException(Executable action) {
        assertThrows(RuntimeException.class, action);
    }

    private ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> extractActualReportFromS3Client(
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) throws JsonProcessingException {
        UriWrapper errorFileUri = constructErrorFileUri(event);
        S3Driver s3Driver = new S3Driver(s3Client, errorFileUri.getUri().getHost());
        String content = s3Driver.getFile(errorFileUri.toS3bucketPath().toString());
        return objectMapperNoEmpty.readValue(content, IMPORT_RESULT_JAVA_TYPE);
    }

    private ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> constructExpectedErrorReport(
        Throwable thrownException,
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        return ImportResult.reportFailure(event, (Exception) thrownException);
    }

    private ResourceService resourceServiceThrowingExceptionWhenSavingResource() {
        return new ResourceService(client, Clock.systemDefaultZone()) {
            @Override
            public Publication createPublicationWithPredefinedCreationDate(Publication publication) {
                throw new RuntimeException(RESOURCE_EXCEPTION_MESSAGE);
            }
        };
    }

    private String eventWithInvalidDetailType(String invalidDetailType) throws JsonProcessingException {
        String input = validEventToJsonString();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event = parseEvent(input);

        event.setDetailType(invalidDetailType);
        input = objectMapperNoEmpty.writeValueAsString(event);
        return input;
    }

    private String validEventToJsonString() {
        CristinObject cristinObject = cristinDataGenerator.randomObject();
        return cristinDataGenerator.toAwsEvent(cristinObject).toJsonString();
    }

    private UserInstance createExpectedPublicationOwner(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        return new UserInstance(event.getDetail().getPublicationsOwner(), HARDCODED_NVA_CUSTOMER);
    }

    private CristinObject generatePublicationFromResource(String input) throws JsonProcessingException {
        JsonNode jsonNode = objectMapperNoEmpty.readTree(input);
        String detail = jsonNode.get(DETAIL_FIELD).toString();
        FileContentsEvent<CristinObject> eventDetails = FileContentsEvent.fromJson(detail, CristinObject.class);
        CristinObject cristinObject = eventDetails.getContents();
        cristinObject.setPublicationOwner(eventDetails.getPublicationsOwner());
        assert nonNull(cristinObject.getId()); //java assertion produces Error not exception
        return cristinObject;
    }
}