package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.CristinImportConfig.eventHandlerObjectMapper;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERRORS_FOLDER;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERROR_SAVING_CRISTIN_RESULT;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.EVENT_SUBTOPIC;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.JSON;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.UNKNOWN_CRISTIN_ID_ERROR_REPORT_PREFIX;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_PUBLICATIONS_OWNER;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.UNIT_CUSTOMER_ID;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.PATH_CUSTOMER;
import static no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedMainCategoryException.ERROR_PARSING_MAIN_CATEGORY;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.nva.exceptions.AffiliationWithoutRoleException;
import no.unit.nva.cristin.mapper.nva.exceptions.ContributorWithoutAffiliationException;
import no.unit.nva.cristin.mapper.nva.exceptions.InvalidIsbnRuntimeException;
import no.unit.nva.cristin.mapper.nva.exceptions.InvalidIssnRuntimeException;
import no.unit.nva.cristin.mapper.nva.exceptions.MissingContributorsException;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedMainCategoryException;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedSecondaryCategoryException;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class CristinEntryEventConsumerTest extends AbstractCristinImportTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final String RESOURCE_EXCEPTION_MESSAGE = "resourceExceptionMessage";
    public static final JavaType IMPORT_RESULT_JAVA_TYPE = constructImportResultJavaType();
    public static final String IGNORED_VALUE = "someBucket";
    public static final String NOT_IMPORTANT = "someBucketName";

    private CristinEntryEventConsumer handler;
    private ByteArrayOutputStream outputStream;
    private ResourceService resourceService;
    private FakeS3Client s3Client;
    private S3Driver s3Driver;

    @BeforeEach
    public void init() {
        super.init();
        resourceService = new ResourceService(super.client, Clock.systemDefaultZone());
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignored");
        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldSaveErrorReportInS3OutsideTheInputFolderAndWithFilenameTheObjectId()
        throws IOException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);

        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);
        Exception throwException = assertThrows(RuntimeException.class, action);

        var actualReport =
            extractActualReportFromS3Client(eventBody, throwException);
        assertThat(actualReport, is(not(nullValue())));
    }

    @Test
    void shouldLogErrorWhenFailingToStorePublicationToDynamo() throws IOException {

        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client);

        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var event = createEventReference(eventBody);
        Executable action = () -> handler.handleRequest(event, outputStream, CONTEXT);
        runWithoutThrowingException(action);

        Integer cristinIdentifier = cristinObject.getId();
        assertThat(appender.getMessages(), containsString(ERROR_SAVING_CRISTIN_RESULT + cristinIdentifier));
        assertThat(appender.getMessages(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    void shouldReturnAnNvaPublicationEntryWhenInputIsEventWithCristinResult() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);
        handler.handleRequest(eventReference, outputStream, CONTEXT);
        var json = outputStream.toString();
        var actualPublication = eventHandlerObjectMapper.readValue(json, Publication.class);

        var expectedPublication = cristinObject.toPublication();
        injectValuesThatAreCreatedWhenSavingInDynamo(actualPublication, expectedPublication);

        assertThat(actualPublication, is(equalTo(expectedPublication)));
        assertThat(actualPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldSavePublicationToDynamoDbWhenInputIsEventWithCristinResult() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, outputStream, CONTEXT);

        var userInstance = createExpectedPublicationOwner();
        var actualPublication = fetchPublicationDirectlyFromDatabase(userInstance);
        var expectedPublication = cristinObject.toPublication();
        injectValuesThatAreCreatedWhenSavingInDynamo(actualPublication, expectedPublication);

        Diff diff = JAVERS.compare(expectedPublication, actualPublication);
        assertThat(diff.prettyPrint(), actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldThrowExceptionWhenSubtopicIsNotAsExpected() {
        var unexpectedSubtopic = randomString();
        var eventReference = new EventReference(randomString(), unexpectedSubtopic, randomUri(), Instant.now());
        var event = EventBridgeEventBuilder.sampleEvent(eventReference);

        Executable action = () -> handler.handleRequest(event, outputStream, CONTEXT);
        var exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(CristinEntryEventConsumer.EVENT_SUBTOPIC));
        assertThat(exception.getMessage(), containsString(unexpectedSubtopic));
    }

    @Test
    void shouldThrowExceptionWhenFailingToStorePublicationToDynamo() throws IOException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client);

        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);

        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);
        var exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    void shouldThrowExceptionWhenCristinMainCategoryTypeIsNotKnown() throws IOException {
        var inputData = CristinDataGenerator.objectWithCustomMainCategory(randomString());
        var eventBody = createEventBody(inputData);
        var eventReference = createEventReference(eventBody);

        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);
        UnsupportedMainCategoryException exception = assertThrows(UnsupportedMainCategoryException.class, action);
        assertThat(exception.getMessage(), is(equalTo(ERROR_PARSING_MAIN_CATEGORY)));
    }

    @Test
    void shouldSaveErrorReportFileInS3ContainingInputDataWhenFailingToStorePublicationToDynamo()
        throws IOException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);

        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);

        RuntimeException thrownException = assertThrows(RuntimeException.class, action);
        var expectedReport =
            constructExpectedErrorReport(thrownException, eventBody);

        var actualReport = extractActualReportFromS3Client(eventBody, thrownException);
        var expectedReportJson = eventHandlerObjectMapper.convertValue(expectedReport, JsonNode.class);
        var actualReportJson = eventHandlerObjectMapper.convertValue(actualReport, JsonNode.class);
        assertThat(actualReportJson, is(equalTo(expectedReportJson)));
    }

    @Test
    void shouldSaveErrorReportInS3ContainingTheOriginalInputData() throws IOException {

        var inputData = CristinDataGenerator.objectWithCustomMainCategory(randomString());
        var eventBody = createEventBody(inputData);
        var eventReference = createEventReference(eventBody);

        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);
        var thrownException = assertThrows(UnsupportedMainCategoryException.class,
                                           action);

        var actualReport = extractActualReportFromS3Client(eventBody, thrownException);
        assertThat(actualReport.getInput().getContents(), is(equalTo(inputData)));
    }

    @Test
    void shouldThrowExceptionWhenSecondaryCategoryTypeIsNotKnown() throws IOException {

        var inputData = CristinDataGenerator.objectWithCustomSecondaryCategory(randomString());
        var eventBody = createEventBody(inputData);
        var eventReference = createEventReference(eventBody);

        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);
        var exception = assertThrows(UnsupportedSecondaryCategoryException.class, action);
        assertThat(exception.getMessage(),
                   containsString(UnsupportedSecondaryCategoryException.ERROR_PARSING_SECONDARY_CATEGORY));
    }

    @Test
    void shouldThrowInvalidIsbnRuntimeExceptionWhenTheIsbnIsInvalid() throws IOException {
        JsonNode cristinObjectWithInvalidIsbn = CristinDataGenerator.objectWithInvalidIsbn();
        var eventBody = createEventBody(cristinObjectWithInvalidIsbn);
        var eventReference = createEventReference(eventBody);

        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);

        assertThrows(InvalidIsbnRuntimeException.class, action);
    }

    @Test
    void shouldThrowInvalidIssnRuntimeExceptionWhenTheBookIssnIsInvalid() throws
                                                                          IOException {
        JsonNode cristinObjectWithInvalidIssn = CristinDataGenerator.bookObjectWithInvalidIssn();
        var eventBody = createEventBody(cristinObjectWithInvalidIssn);
        var eventReference = createEventReference(eventBody);

        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);

        assertThrows(InvalidIssnRuntimeException.class, action);
    }

    @Test
    void shouldThrowInvalidIssnRuntimeExceptionWhenTheJournalIssnIsInvalid() throws IOException {
        var cristinObjectWithInvalidIssn = CristinDataGenerator.journalObjectWithInvalidIssn();
        var eventBody = createEventBody(cristinObjectWithInvalidIssn);
        var eventReference = createEventReference(eventBody);
        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);

        assertThrows(InvalidIssnRuntimeException.class, action);
    }

    @Test
    void handlerThrowsMissingContributorsRuntimeExceptionWhenTheCristinObjectHasNoContributors()
        throws IOException {
        var cristinObjectWithoutContributors = CristinDataGenerator.objectWithoutContributors();
        var eventBody = createEventBody(cristinObjectWithoutContributors);
        var eventReference = createEventReference(eventBody);
        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);

        assertThrows(MissingContributorsException.class, action);
    }

    @Test
    void handlerThrowContributorWithoutAffiliationExceptionWhenTheCristinObjectHasContributorWithoutAffiliation()
        throws IOException {
        var cristinObjectWithoutAffiliations =
            CristinDataGenerator.objectWithContributorsWithoutAffiliation();
        var eventBody = createEventBody(cristinObjectWithoutAffiliations);
        var eventReference = createEventReference(eventBody);

        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);

        assertThrows(ContributorWithoutAffiliationException.class, action);
    }

    @Test
    void handlerThrowsAffiliationWithoutARoleExceptionWhenTheCristinObjectHasAffiliationsWithoutRoles()
        throws IOException {
        JsonNode cristinObjectWithAffiliationWithoutRoles = CristinDataGenerator
                                                                .objectWithAffiliationWithoutRole();
        var eventBody = createEventBody(cristinObjectWithAffiliationWithoutRoles);
        var eventReference = createEventReference(eventBody);

        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);
        assertThrows(AffiliationWithoutRoleException.class, action);
    }

    @Test
    void handlerCreatesFileWithCustomNameWhenCristinIdIsNotFound() throws IOException {
        JsonNode cristinObjectWithoutId = CristinDataGenerator.objectWithoutId();
        var eventBody = createEventBody(cristinObjectWithoutId);
        var eventReference = createEventReference(eventBody);

        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);
        runWithoutThrowingException(action);

        S3Driver s3Driver = new S3Driver(s3Client, IGNORED_VALUE);
        UnixPath errorReportFile = s3Driver.listAllFiles(ERRORS_FOLDER)
                                       .stream()
                                       .collect(SingletonCollector.collect());
        String errorReport = s3Driver.getFile(errorReportFile);

        ImportResult<FileContentsEvent<JsonNode>> actualReport =
            eventHandlerObjectMapper.readValue(errorReport, IMPORT_RESULT_JAVA_TYPE);

        assertThat(errorReportFile.toString(), containsString(UNKNOWN_CRISTIN_ID_ERROR_REPORT_PREFIX));
        assertThat(actualReport.getInput().getContents(), is(equalTo(cristinObjectWithoutId)));
    }

    @Test
    void shouldSaveFileInInputFolderAsErrorTimestampExceptionNameInputFileLocationInputFileWhenFailingToSaveInDynamo()
        throws Throwable {
        var cristinObjectWithCustomSecondaryCategory =
            CristinDataGenerator.objectWithCustomSecondaryCategory(randomString());
        var eventbody = createEventBody(cristinObjectWithCustomSecondaryCategory);
        var eventReference = createEventReference(eventbody);
        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);

        var exception =
            assertThrows(UnsupportedSecondaryCategoryException.class, action);

        var s3Driver = new S3Driver(s3Client, "bucket");
        var expectedFilePath = eventbody.getFileUri().getPath();
        var expectedTimestamp = eventbody.getTimestamp();
        var exceptionName = exception.getClass().getSimpleName();
        var fileIdWithEnding = cristinObjectWithCustomSecondaryCategory.get("id").asText() + JSON;
        var expectedErrorFileLocation = ERRORS_FOLDER
                                            .addChild(timestampToString(expectedTimestamp))
                                            .addChild(exceptionName)
                                            .addChild(expectedFilePath)
                                            .addChild(fileIdWithEnding);

        var actualErrorFile = s3Driver.getFile(expectedErrorFileLocation);
        assertThat(actualErrorFile, is(not(nullValue())));
    }

    @Test
    void shouldThrowExceptionWhenInputContainsUnknownProperty() throws IOException {
        var unknownProperty = randomString();
        var objectWithUnknownProperty = CristinDataGenerator.objectWithUnknownProperty(unknownProperty);
        var eventBody = createEventBody(objectWithUnknownProperty);
        var eventReference = createEventReference(eventBody);
        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);
        RuntimeException exception = assertThrows(RuntimeException.class, action);

        UnixPath expectedFilePath = constructExpectedFilePathForEntryWithUnkownFields(eventBody, exception);

        S3Driver s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        String file = s3Driver.getFile(expectedFilePath);

        assertThat(file, is(not(emptyString())));
        assertThat(file, containsString(unknownProperty));
    }

    @Test
    void handleRequestDoesNotThrowExceptionWhenInputDoesNotHaveUnknownProperties() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);

        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);
        assertDoesNotThrow(action);
    }

    @Test
    void shouldBeAbleToParseCristinTags() throws IOException {
        var cristinObjectWithTags = CristinDataGenerator.objectWithTags();
        var eventBody = createEventBody(cristinObjectWithTags);
        var eventReference = createEventReference(eventBody);
        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);
        assertDoesNotThrow(action);
    }

    @Test
    void shouldBeAbleToParseCristinHrcsCategoriesAndActivities() throws IOException {
        var cristinObjectWithCristinHrcsCategoriesAndActivities =
            CristinDataGenerator.objectWithCristinHrcsCategoriesAndActivities();
        var eventBody = createEventBody(cristinObjectWithCristinHrcsCategoriesAndActivities);
        var eventReference = createEventReference(eventBody);
        Executable action = () -> handler.handleRequest(eventReference, outputStream, CONTEXT);
        assertDoesNotThrow(action);
    }

    private static <T> FileContentsEvent<T> createEventBody(T cristinObject) {
        return new FileContentsEvent<>(randomString(), EVENT_SUBTOPIC, randomUri(), Instant.now(),
                                       cristinObject);
    }

    private static JavaType constructImportResultJavaType() {

        JavaType fileContentsType = eventHandlerObjectMapper.getTypeFactory()
                                        .constructParametricType(FileContentsEvent.class, JsonNode.class);
        return eventHandlerObjectMapper.getTypeFactory()
                   .constructParametricType(ImportResult.class, fileContentsType);
    }

    private <T> InputStream createEventReference(FileContentsEvent<T> eventBody) throws IOException {
        var eventFileUri = s3Driver.insertEvent(UnixPath.EMPTY_PATH, eventBody.toJsonString());
        var eventReference = new EventReference(randomString(), EVENT_SUBTOPIC, eventFileUri);
        return EventBridgeEventBuilder.sampleEvent(eventReference);
    }

    private <T> UnixPath constructExpectedFilePathForEntryWithUnkownFields(
        FileContentsEvent<T> event, RuntimeException exception) {
        return ERRORS_FOLDER
                   .addChild(timestampToString(event.getTimestamp()))
                   .addChild(exception.getClass().getSimpleName())
                   .addChild(event.getFileUri().getPath())
                   .addChild(extractCristinObjectId(event) + JSON);
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

    private <T> UriWrapper constructErrorFileUri(FileContentsEvent<T> eventBody,
                                                 Exception exception) {

        var cristinObjectId = extractCristinObjectId(eventBody);
        String errorReportFilename = cristinObjectId + JSON;
        UriWrapper inputFile = UriWrapper.fromUri(eventBody.getFileUri());
        Instant timestamp = eventBody.getTimestamp();
        UriWrapper bucket = inputFile.getHost();
        return bucket.addChild(ERRORS_FOLDER)
                   .addChild(timestampToString(timestamp))
                   .addChild(exception.getClass().getSimpleName())
                   .addChild(inputFile.getPath())
                   .addChild(errorReportFilename);
    }

    private <T> int extractCristinObjectId(FileContentsEvent<T> eventBody) {
        var cristinObject = eventBody.getContents();
        if (cristinObject instanceof CristinObject) {
            return ((CristinObject) cristinObject).getId();
        } else if (cristinObject instanceof ObjectNode) {
            var idString = ((ObjectNode) cristinObject).get("id").asText();
            return Integer.parseInt(idString);
        }
        throw new UnsupportedOperationException("Only CristinObject and ObjectNode currently supported");
    }

    private void runWithoutThrowingException(Executable action) {
        assertThrows(RuntimeException.class, action);
    }

    private <T> ImportResult<FileContentsEvent<T>> extractActualReportFromS3Client(
        FileContentsEvent<T> eventBody,
        Exception exception) throws JsonProcessingException {
        UriWrapper errorFileUri = constructErrorFileUri(eventBody, exception);
        S3Driver s3Driver = new S3Driver(s3Client, errorFileUri.getUri().getHost());
        String content = s3Driver.getFile(errorFileUri.toS3bucketPath());
        return eventHandlerObjectMapper.readValue(content, IMPORT_RESULT_JAVA_TYPE);
    }

    private <T> ImportResult<FileContentsEvent<T>> constructExpectedErrorReport(
        Throwable thrownException,
        FileContentsEvent<T> event) {
        return ImportResult.reportFailure(event, (Exception) thrownException);
    }

    private ResourceService resourceServiceThrowingExceptionWhenSavingResource() {
        return new ResourceService(client, Clock.systemDefaultZone()) {
            @Override
            public Publication createPublicationFromImportedEntry(Publication publication) {
                throw new RuntimeException(RESOURCE_EXCEPTION_MESSAGE);
            }
        };
    }

    private UserInstance createExpectedPublicationOwner() {
        UriWrapper customerId = UriWrapper.fromUri(NVA_API_DOMAIN).addChild(PATH_CUSTOMER, UNIT_CUSTOMER_ID);
        return UserInstance.create(HARDCODED_PUBLICATIONS_OWNER, customerId.getUri());
    }
}