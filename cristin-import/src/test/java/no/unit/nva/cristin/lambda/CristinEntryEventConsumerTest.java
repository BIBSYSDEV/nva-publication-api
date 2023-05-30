package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.CristinImportConfig.eventHandlerObjectMapper;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERRORS_FOLDER;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERROR_SAVING_CRISTIN_RESULT;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.EVENT_SUBTOPIC;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.JSON;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.SUCCESS_FOLDER;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.UNKNOWN_CRISTIN_ID_ERROR_REPORT_PREFIX;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.CHAPTER_ACADEMIC;
import static no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedMainCategoryException.ERROR_PARSING_MAIN_CATEGORY;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinBookOrReportPartMetadata;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.NvaPublicationPartOf;
import no.unit.nva.cristin.mapper.NvaPublicationPartOfCristinPublication;
import no.unit.nva.cristin.mapper.nva.exceptions.AffiliationWithoutRoleException;
import no.unit.nva.cristin.mapper.nva.exceptions.ContributorWithoutAffiliationException;
import no.unit.nva.cristin.mapper.nva.exceptions.InvalidIsbnRuntimeException;
import no.unit.nva.cristin.mapper.nva.exceptions.InvalidIssnRuntimeException;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedMainCategoryException;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedSecondaryCategoryException;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
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

class CristinEntryEventConsumerTest extends AbstractCristinImportTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final String RESOURCE_EXCEPTION_MESSAGE = "resourceExceptionMessage";
    public static final JavaType IMPORT_RESULT_JAVA_TYPE = constructImportResultJavaType();
    public static final String IGNORED_VALUE = "someBucket";
    public static final String NOT_IMPORTANT = "someBucketName";

    private CristinEntryEventConsumer handler;
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
    }

    @Test
    void shouldContinueProcessingRecordsWhenSomeOfTheRecordsAreInvalid() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var validEventBody = createEventBody(cristinObject);
        var event = createEventReferenceWithInvalidMessagesAlongWithValidEventBody(validEventBody);
        var actualPublications = handler.handleRequest(event, CONTEXT);
        assertThat(actualPublications, hasSize(1));
    }

    @Test
    void shouldSaveErrorReportInS3OutsideTheInputFolderAndWithFilenameTheObjectId()
        throws IOException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, CONTEXT);
        var expectedExceptionName = RuntimeException.class.getSimpleName();

        var actualReport =
            extractActualReportFromS3Client(eventBody, expectedExceptionName);
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
        handler.handleRequest(event, CONTEXT);

        Integer cristinIdentifier = cristinObject.getId();
        assertThat(appender.getMessages(), containsString(ERROR_SAVING_CRISTIN_RESULT + cristinIdentifier));
        assertThat(appender.getMessages(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    void shouldReturnAnNvaPublicationEntryWhenInputIsEventWithCristinResult() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);
        var actualPublications = handler.handleRequest(eventReference, CONTEXT);
        var actualPublication = actualPublications.get(0);

        var expectedPublication = cristinObject.toPublication();
        injectValuesThatAreCreatedWhenSavingInDynamo(actualPublication, expectedPublication);

        assertThat(actualPublication, is(equalTo(expectedPublication)));
        assertThat(actualPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldPersistCristinIdInFileNamedWithPublicationIdentifier() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);
        var publications = handler.handleRequest(eventReference, CONTEXT);
        var actualPublication = publications.get(0);
        var expectedFileNameStoredInS3 = actualPublication.getIdentifier().toString();

        var expectedTimestamp = eventBody.getTimestamp();
        var expectedErrorFileLocation = SUCCESS_FOLDER
                                            .addChild(timestampToString(expectedTimestamp))
                                            .addChild(expectedFileNameStoredInS3);

        assertDoesNotThrow(() -> s3Driver.getFile(expectedErrorFileLocation));
    }

    @Test
    void shouldSavePublicationToDynamoDbWhenInputIsEventWithCristinResult() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, CONTEXT);

        var actualPublication = fetchPublicationDirectlyFromDatabase(cristinObject.getId().toString());
        var expectedPublication = cristinObject.toPublication();
        injectValuesThatAreCreatedWhenSavingInDynamo(actualPublication, expectedPublication);

        Diff diff = JAVERS.compare(expectedPublication, actualPublication);
        assertThat(diff.prettyPrint(), actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void shouldStoreErrorReportWhenFailingToStorePublicationToDynamo() throws IOException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client);

        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, CONTEXT);
        var exceptionName = RuntimeException.class.getSimpleName();
        var actualReport =
            extractActualReportFromS3Client(eventBody, exceptionName);
        assertThat(actualReport.getException(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    void shouldStoreErrorReportWhenCristinMainCategoryTypeIsNotKnown() throws IOException {
        var inputData = CristinDataGenerator.objectWithCustomMainCategory(randomString());
        var eventBody = createEventBody(inputData);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, CONTEXT);
        var expectedExceptionName = UnsupportedMainCategoryException.class.getSimpleName();
        var actualReport = extractActualReportFromS3Client(eventBody, expectedExceptionName);
        assertThat(actualReport.getException(), containsString(ERROR_PARSING_MAIN_CATEGORY));
    }

    @Test
    void shouldSaveErrorReportFileInS3ContainingInputDataWhenFailingToStorePublicationToDynamo()
        throws IOException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);

        handler = new CristinEntryEventConsumer(resourceService, s3Client);
        handler.handleRequest(eventReference, CONTEXT);
        var expectedExceptionName = RuntimeException.class.getSimpleName();
        UnixPath expectedFilePath = constructExpectedFilePathForEntryWithUnkownFields(eventBody,
                                                                                      expectedExceptionName);

        S3Driver s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        String file = s3Driver.getFile(expectedFilePath);

        assertThat(file, is(not(emptyString())));
        assertThat(file, containsString(expectedExceptionName));
    }

    @Test
    void shouldSaveErrorReportInS3ContainingTheOriginalInputData() throws IOException {

        var inputData = CristinDataGenerator.objectWithCustomMainCategory(randomString());
        var eventBody = createEventBody(inputData);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, CONTEXT);
        var expectedExceptionName = UnsupportedMainCategoryException.class.getSimpleName();

        var actualReport = extractActualReportFromS3Client(eventBody, expectedExceptionName);
        assertThat(actualReport.getInput().getContents(), is(equalTo(inputData)));
    }

    @Test
    void shouldStoreErrorReportWhenSecondaryCategoryTypeIsNotKnown() throws IOException {

        var inputData = CristinDataGenerator.objectWithCustomSecondaryCategory(randomString());
        var eventBody = createEventBody(inputData);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, CONTEXT);
        var actualReport = extractActualReportFromS3Client(eventBody,
                                                           UnsupportedSecondaryCategoryException.class.getSimpleName());
        assertThat(actualReport.getException(),
                   containsString(UnsupportedSecondaryCategoryException.ERROR_PARSING_SECONDARY_CATEGORY));
    }

    @Test
    void shouldStoreInvalidIsbnRuntimeExceptionWhenTheIsbnIsInvalid() throws IOException {
        JsonNode cristinObjectWithInvalidIsbn = CristinDataGenerator.objectWithInvalidIsbn();
        var eventBody = createEventBody(cristinObjectWithInvalidIsbn);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, CONTEXT);

        var actualReport = extractActualReportFromS3Client(eventBody,
                                                            InvalidIsbnRuntimeException.class.getSimpleName());
        assertThat(actualReport.getException(), notNullValue());
    }

    @Test
    void shouldStoreIssnRuntimeExceptionWhenTheBookIssnIsInvalid() throws
                                                                          IOException {
        JsonNode cristinObjectWithInvalidIssn = CristinDataGenerator.bookObjectWithInvalidIssn();
        var eventBody = createEventBody(cristinObjectWithInvalidIssn);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, CONTEXT);

        var actualReport = extractActualReportFromS3Client(eventBody,
                                                           InvalidIssnRuntimeException.class.getSimpleName());
        assertThat(actualReport.getException(), notNullValue());
    }

    @Test
    void shouldStoreInvalidIssnRuntimeExceptionWhenTheJournalIssnIsInvalid() throws IOException {
        var cristinObjectWithInvalidIssn = CristinDataGenerator.journalObjectWithInvalidIssn();
        var eventBody = createEventBody(cristinObjectWithInvalidIssn);
        var eventReference = createEventReference(eventBody);
        handler.handleRequest(eventReference, CONTEXT);

        var actualReport = extractActualReportFromS3Client(eventBody,
                                                           InvalidIssnRuntimeException.class.getSimpleName());
        assertThat(actualReport.getException(), notNullValue());
    }

    @Test
    void handlerDoesNotThrowsExceptionRuntimeExceptionWhenTheCristinObjectHasNoContributors()
        throws IOException {
        var cristinObjectWithoutContributors = CristinDataGenerator.objectWithoutContributors();
        var eventBody = createEventBody(cristinObjectWithoutContributors);
        var eventReference = createEventReference(eventBody);
        var publications = handler.handleRequest(eventReference, CONTEXT);
        var actualtPublication = publications.get(0);
        assertThat(actualtPublication.getEntityDescription().getContributors(), hasSize(0));
    }

    @Test
    void handlerThrowContributorWithoutAffiliationExceptionWhenTheCristinObjectHasContributorWithoutAffiliation()
        throws IOException {
        var cristinObjectWithoutAffiliations =
            CristinDataGenerator.objectWithContributorsWithoutAffiliation();
        var eventBody = createEventBody(cristinObjectWithoutAffiliations);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, CONTEXT);
        var expectedExceptionName = ContributorWithoutAffiliationException.class.getSimpleName();

        UnixPath expectedFilePath = constructExpectedFilePathForEntryWithUnkownFields(eventBody,
                                                                                      expectedExceptionName);

        S3Driver s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        String file = s3Driver.getFile(expectedFilePath);

        assertThat(file, is(not(emptyString())));
        assertThat(file, containsString(expectedExceptionName));
    }

    @Test
    void handlerStoresAffiliationWithoutARoleExceptionWhenTheCristinObjectHasAffiliationsWithoutRoles()
        throws IOException {
        JsonNode cristinObjectWithAffiliationWithoutRoles = CristinDataGenerator
                                                                .objectWithAffiliationWithoutRole();
        var eventBody = createEventBody(cristinObjectWithAffiliationWithoutRoles);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, CONTEXT);

        var expectedExceptionName = AffiliationWithoutRoleException.class.getSimpleName();
        UnixPath expectedFilePath = constructExpectedFilePathForEntryWithUnkownFields(eventBody,
                                                                                      expectedExceptionName);
        S3Driver s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        String file = s3Driver.getFile(expectedFilePath);

        assertThat(file, is(not(emptyString())));
        assertThat(file, containsString(expectedExceptionName));
    }

    @Test
    void handlerCreatesFileWithCustomNameWhenCristinIdIsNotFound() throws IOException {
        JsonNode cristinObjectWithoutId = CristinDataGenerator.objectWithoutId();
        var eventBody = createEventBody(cristinObjectWithoutId);
        var eventReference = createEventReference(eventBody);

        handler.handleRequest(eventReference, CONTEXT);

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
        handler.handleRequest(eventReference, CONTEXT);

        var s3Driver = new S3Driver(s3Client, "bucket");
        var expectedFilePath = eventbody.getFileUri().getPath();
        var expectedTimestamp = eventbody.getTimestamp();
        var exceptionName = UnsupportedSecondaryCategoryException.class.getSimpleName();
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
    void shouldStoreErrorReportWhenInputContainsUnknownProperty() throws IOException {
        var unknownProperty = randomString();
        var objectWithUnknownProperty = CristinDataGenerator.objectWithUnknownProperty(unknownProperty);
        var eventBody = createEventBody(objectWithUnknownProperty);
        var eventReference = createEventReference(eventBody);
        handler.handleRequest(eventReference, CONTEXT);
        UnixPath expectedFilePath =
            constructExpectedFilePathForEntryWithUnkownFields(eventBody,
                                                              IllegalArgumentException.class.getSimpleName());

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

        var publications = handler.handleRequest(eventReference, CONTEXT);
        assertThat(publications.get(0), notNullValue());
    }

    @Test
    void shouldBeAbleToParseCristinTags() throws IOException {
        var cristinObjectWithTags = CristinDataGenerator.objectWithTags();
        var eventBody = createEventBody(cristinObjectWithTags);
        var eventReference = createEventReference(eventBody);
        var publications = handler.handleRequest(eventReference, CONTEXT);
        assertThat(publications.get(0), notNullValue());
    }

    @Test
    void shouldBeAbleToParseCristinHrcsCategoriesAndActivities() throws IOException {
        var cristinObjectWithCristinHrcsCategoriesAndActivities =
            CristinDataGenerator.objectWithCristinHrcsCategoriesAndActivities();
        var eventBody = createEventBody(cristinObjectWithCristinHrcsCategoriesAndActivities);
        var eventReference = createEventReference(eventBody);
        var publications = handler.handleRequest(eventReference, CONTEXT);
        assertThat(publications.get(0), notNullValue());
    }

    @Test
    void shouldPersistPartOfCristinIdToS3ForPatchPurposes() throws IOException {
        var partOfIdentifier = randomString();
        var cristinObject =
            CristinDataGenerator.randomObject(CHAPTER_ACADEMIC.getValue());
        cristinObject.setBookOrReportPartMetadata(
            CristinBookOrReportPartMetadata.builder().withPartOf(partOfIdentifier).build());

        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);
        var publications = handler.handleRequest(eventReference, CONTEXT);

        var actualPublication = publications.get(0);
        var expectedFileNameStoredInS3 = actualPublication.getIdentifier().toString();
        var expectedPartOFCristinPublication = NvaPublicationPartOfCristinPublication.builder()
                                                   .withPartOf(NvaPublicationPartOf.builder()
                                                                   .withCristinId(partOfIdentifier)
                                                                   .build())
                                                   .withNvaPublicationIdentifier(expectedFileNameStoredInS3)
                                                   .build();

        var expectedTimestamp = eventBody.getTimestamp();
        var expectedErrorFileLocation = UnixPath.of("PUBLICATIONS_THAT_ARE_PART_OF_OTHER_PUBLICATIONS")
                                            .addChild(timestampToString(expectedTimestamp))
                                            .addChild(expectedFileNameStoredInS3);

        var partOfFile = s3Driver.getFile(expectedErrorFileLocation);
        var actualPartOfCristinPublication =
            JsonUtils.dtoObjectMapper.readValue(partOfFile,
                                                NvaPublicationPartOfCristinPublication.class);
        assertThat(actualPartOfCristinPublication,
                   is(equalTo(expectedPartOFCristinPublication)));
    }

    private SQSEvent createEventReferenceWithInvalidMessagesAlongWithValidEventBody(
        FileContentsEvent<CristinObject> validEventBody)
        throws IOException {
        var eventFileUri = s3Driver.insertEvent(UnixPath.EMPTY_PATH, validEventBody.toJsonString());
        var eventReference = new EventReference(randomString(), EVENT_SUBTOPIC, eventFileUri);
        var sqsEvent = new SQSEvent();
        var sqsMessage = new SQSMessage();
        var invalidSqsMessage = new SQSMessage();
        invalidSqsMessage.setBody(randomString());
        sqsMessage.setBody(eventReference.toJsonString());
        sqsEvent.setRecords(List.of(invalidSqsMessage, sqsMessage));
        return sqsEvent;
    }

    private static <T> FileContentsEvent<T> createEventBody(T cristinObject) {
        return new FileContentsEvent<>(randomString(), EVENT_SUBTOPIC, randomUri(), Instant.now(),
                                       cristinObject);
    }

    private static JavaType constructImportResultJavaType() {
        var fileContentsType = eventHandlerObjectMapper.getTypeFactory()
                                   .constructParametricType(FileContentsEvent.class, JsonNode.class);
        return eventHandlerObjectMapper.getTypeFactory()
                   .constructParametricType(ImportResult.class, fileContentsType);
    }

    private SQSEvent createEventReference(FileContentsEvent eventBody) throws IOException {
        var eventFileUri = s3Driver.insertEvent(UnixPath.EMPTY_PATH, eventBody.toJsonString());
        var eventReference = new EventReference(randomString(), EVENT_SUBTOPIC, eventFileUri);
        var sqsEvent = new SQSEvent();
        var sqsMessage = new SQSMessage();
        sqsMessage.setBody(eventReference.toJsonString());
        sqsEvent.setRecords(List.of(sqsMessage));
        return sqsEvent;
    }

    private <T> UnixPath constructExpectedFilePathForEntryWithUnkownFields(
        FileContentsEvent<T> event, String exceptionName) {
        return ERRORS_FOLDER
                   .addChild(timestampToString(event.getTimestamp()))
                   .addChild(exceptionName)
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

    private Publication fetchPublicationDirectlyFromDatabase(String cristinIdentifier) {
        return resourceService.getPublicationsByCristinIdentifier(cristinIdentifier)
                   .stream()
                   .collect(SingletonCollector.collect());
    }

    private <T> UriWrapper constructErrorFileUri(FileContentsEvent<T> eventBody,
                                                 String exceptionName) {

        var cristinObjectId = extractCristinObjectId(eventBody);
        String errorReportFilename = cristinObjectId + JSON;
        UriWrapper inputFile = UriWrapper.fromUri(eventBody.getFileUri());
        Instant timestamp = eventBody.getTimestamp();
        UriWrapper bucket = inputFile.getHost();
        return bucket.addChild(ERRORS_FOLDER)
                   .addChild(timestampToString(timestamp))
                   .addChild(exceptionName)
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

    private <T> ImportResult<FileContentsEvent<T>> extractActualReportFromS3Client(
        FileContentsEvent<T> eventBody,
        String exceptionName) throws JsonProcessingException {
        UriWrapper errorFileUri = constructErrorFileUri(eventBody, exceptionName);
        S3Driver s3Driver = new S3Driver(s3Client, errorFileUri.getUri().getHost());
        String content = s3Driver.getFile(errorFileUri.toS3bucketPath());
        return eventHandlerObjectMapper.readValue(content, IMPORT_RESULT_JAVA_TYPE);
    }

    private ResourceService resourceServiceThrowingExceptionWhenSavingResource() {
        return new ResourceService(client, Clock.systemDefaultZone()) {
            @Override
            public Publication createPublicationFromImportedEntry(Publication publication) {
                throw new RuntimeException(RESOURCE_EXCEPTION_MESSAGE);
            }
        };
    }
}