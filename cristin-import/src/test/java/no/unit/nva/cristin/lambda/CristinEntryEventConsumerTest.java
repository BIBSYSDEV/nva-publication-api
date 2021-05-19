package no.unit.nva.cristin.lambda;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.CristinDataGenerator.randomString;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERROR_SAVING_CRISTIN_RESULT;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.FILE_ENDING;
import static nva.commons.core.JsonUtils.objectMapperNoEmpty;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
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
import no.unit.nva.publication.s3imports.FileContentsEvent;
import no.unit.nva.publication.s3imports.ImportResult;
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

    public static final Path VALID_CRISTIN_ENTRY_EVENT = Path.of("valid_cristin_entry_event.json");
    public static final AwsEventBridgeEvent<FileContentsEvent<JsonNode>> VALID_CRISTIN_ENTRY_EVENT_OBJECT =
        parseEvent(IoUtils.stringFromResources(VALID_CRISTIN_ENTRY_EVENT));

    public static final Context CONTEXT = mock(Context.class);
    public static final String DETAIL_FIELD = "detail";
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final String RESOURCE_EXCEPTION_MESSAGE = "resourceExceptionMessage";
    public static final String OWNER_VALUE_IN_RESOURCE_FILE = "someRandomUser";
    public static final JavaType IMPORT_RESULT_JAVA_TYPE = constructImportResultJavaType();
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
        CristinObject cristinObject = CristinEntryEventConsumer.parseCristinObject(VALID_CRISTIN_ENTRY_EVENT_OBJECT);
        Integer cristinIdentifier = cristinObject.getId();
        Executable action = () -> handler.handleRequest(stringToStream(validEventToJsonString()), outputStream,
                                                        CONTEXT);

        runWithoutThrowingException(action);

        assertThat(appender.getMessages(), containsString(ERROR_SAVING_CRISTIN_RESULT + cristinIdentifier));
        assertThat(appender.getMessages(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    public void handlerReturnsAnNvaPublicationEntryWhenInputIsEventWithCristinResult() throws JsonProcessingException {
        String input = validEventToJsonString();
        handler.handleRequest(stringToStream(input), outputStream, CONTEXT);
        String json = outputStream.toString();
        Publication actualPublication = objectMapperNoEmpty.readValue(json, Publication.class);

        Publication expectedPublication = generatePublicationFromResource(input).toPublication();
        expectedPublication.setIdentifier(actualPublication.getIdentifier());
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    public void handlerSavesPublicationToDynamoDbWhenInputIsEventWithCristinResult() throws JsonProcessingException {
        String input = validEventToJsonString();
        handler.handleRequest(stringToStream(input), outputStream, CONTEXT);
        CristinObject cristinObject = generatePublicationFromResource(input);
        Publication expectedPublication = cristinObject.toPublication();
        UserInstance userInstance = extractUserInstance(expectedPublication);

        Publication actualPublication = resourceService.getPublicationsByOwner(userInstance)
                                            .stream()
                                            .collect(SingletonCollector.collect());

        expectedPublication.setIdentifier(actualPublication.getIdentifier());
        Diff diff = JAVERS.compare(expectedPublication, actualPublication);
        assertThat(diff.prettyPrint(), actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    public void handlerSavesPublicationWithOwnerBeingTheSuppliedByTheEventOwner() throws JsonProcessingException {
        String input = validEventToJsonString();
        handler.handleRequest(stringToStream(input), outputStream, CONTEXT);
        CristinObject cristinObject = generatePublicationFromResource(input);
        Publication expectedPublication = cristinObject.toPublication();
        UserInstance userInstance = extractUserInstance(expectedPublication);

        Publication actualPublication = resourceService.getPublicationsByOwner(userInstance)
                                            .stream()
                                            .collect(SingletonCollector.collect());
        String actualPublicationOwner = actualPublication.getOwner();

        assertThat(actualPublicationOwner, is(equalTo(OWNER_VALUE_IN_RESOURCE_FILE)));
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

        Integer cristinIdentifier = Optional.of(VALID_CRISTIN_ENTRY_EVENT_OBJECT)
                                        .map(CristinEntryEventConsumer::parseCristinObject)
                                        .map(CristinObject::getId)
                                        .orElseThrow();
        Executable action = () -> handler.handleRequest(stringToStream(validEventToJsonString()), outputStream,
                                                        CONTEXT);

        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(ERROR_SAVING_CRISTIN_RESULT + cristinIdentifier));
        assertThat(exception.getCause().getMessage(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    public void handlerSavesErrorReportInS3InTheLocationIndicatedAsTheInputsFileLocationAndWithFilenameTheObjectId() {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        String event = validEventToJsonString();

        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        Executable action = () -> handler.handleRequest(stringToStream(event), outputStream, CONTEXT);
        assertThrows(RuntimeException.class, action);

        URI errorFileUri = CristinEntryEventConsumer.constructErrorFileUri(VALID_CRISTIN_ENTRY_EVENT_OBJECT);
        S3Driver s3Driver = new S3Driver(s3Client, errorFileUri.getHost());

        CristinObject cristinObject = CristinEntryEventConsumer.parseCristinObject(VALID_CRISTIN_ENTRY_EVENT_OBJECT);
        String expectedFilename = cristinObject.getId() + FILE_ENDING;
        Path expectedFolderPath = Optional.of(VALID_CRISTIN_ENTRY_EVENT_OBJECT)
                                      .map(AwsEventBridgeEvent::getDetail)
                                      .map(FileContentsEvent::getFileUri)
                                      .map(URI::getPath)
                                      .map(Path::of)
                                      .map(Path::getParent)
                                      .orElseThrow();
        Path expectedPath = Path.of(expectedFolderPath.toString(), expectedFilename);
        String actualReport = s3Driver.getFile(expectedPath.toString());
        assertThat(actualReport, is(not(emptyString())));
    }

    @Test
    public void handlerThrowsExceptionWhenMainCategoryTypeIsNotKnown() throws JsonProcessingException {

        JsonNode inputData = cristinDataGenerator.customMainCategory(randomString());
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
        String event = validEventToJsonString();

        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        Executable action = () -> handler.handleRequest(stringToStream(event), outputStream, CONTEXT);
        RuntimeException thrownException = assertThrows(RuntimeException.class, action);
        JsonNode expectedReport = constructExpectedErrorReport((Exception) thrownException.getCause());

        JsonNode actualReport = extractActualReportFromS3Client();
        assertThat(actualReport, is(equalTo(expectedReport)));
    }

    @Test
    public void handlerSavesReportInS3ContainingTheOriginalInputData() throws JsonProcessingException {

        JsonNode inputData = cristinDataGenerator.customMainCategory(randomString());
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = cristinDataGenerator.toAwsEvent(inputData);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        runWithoutThrowingException(action);

        URI errorFileUri = CristinEntryEventConsumer.constructErrorFileUri(awsEvent);
        S3Driver s3Driver = new S3Driver(s3Client, errorFileUri.getHost());
        String actualReport = s3Driver.getFile(errorFileUri.getPath());

        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> report =
            objectMapperNoEmpty.readValue(actualReport, IMPORT_RESULT_JAVA_TYPE);
        assertThat(report.getInput().getDetail().getContents(), is(equalTo(inputData)));
    }

    @Test
    public void handlerThrowsExceptionWhenSecondaryCategoryTypeIsNotKnown() throws JsonProcessingException {

        JsonNode inputData = cristinDataGenerator.customSecondaryCategory(randomString());
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> awsEvent = cristinDataGenerator.toAwsEvent(inputData);
        InputStream inputStream = IoUtils.stringToStream(awsEvent.toJsonString());

        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        RuntimeException exception = assertThrows(RuntimeException.class, action);

        Throwable cause = exception.getCause();
        assertThat(cause, is(instanceOf(UnsupportedOperationException.class)));
        assertThat(cause.getMessage(), is(equalTo(CristinMapper.ERROR_PARSING_SECONDARY_CATEGORY)));
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

    private void runWithoutThrowingException(Executable action) {
        assertThrows(RuntimeException.class, action);
    }

    private JsonNode extractActualReportFromS3Client() throws JsonProcessingException {
        URI errorFileUri = CristinEntryEventConsumer
                               .constructErrorFileUri(VALID_CRISTIN_ENTRY_EVENT_OBJECT);
        S3Driver s3Driver = new S3Driver(s3Client, errorFileUri.getHost());
        String content = s3Driver.getFile(errorFileUri.getPath());
        return objectMapperNoEmpty.readTree(content);
    }

    private JsonNode constructExpectedErrorReport(Exception thrownException) {
        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> expectedImportResult =
            ImportResult.reportFailure(VALID_CRISTIN_ENTRY_EVENT_OBJECT, thrownException);
        return objectMapperNoEmpty.convertValue(expectedImportResult, JsonNode.class);
    }

    private ResourceService resourceServiceThrowingExceptionWhenSavingResource() {
        return new ResourceService(client, Clock.systemDefaultZone()) {
            @Override
            public Publication createPublicationWithPredefinedCreationDate(Publication publication) {
                throw new RuntimeException(RESOURCE_EXCEPTION_MESSAGE);
            }
        };
    }

    private String validEventToJsonString() {
        return IoUtils.stringFromResources(VALID_CRISTIN_ENTRY_EVENT);
    }

    private String eventWithInvalidDetailType(String invalidDetailType) throws JsonProcessingException {
        String input = validEventToJsonString();
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event = parseEvent(input);

        event.setDetailType(invalidDetailType);
        input = objectMapperNoEmpty.writeValueAsString(event);
        return input;
    }

    private UserInstance extractUserInstance(Publication publication) {
        return new UserInstance(publication.getOwner(), publication.getPublisher().getId());
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