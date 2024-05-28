package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.CristinImportConfig.eventHandlerObjectMapper;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERRORS_FOLDER;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERROR_SAVING_CRISTIN_RESULT;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.EVENT_SUBTOPIC;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.JSON;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.NVI_FOLDER;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.SUCCESS_FOLDER;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.UNKNOWN_CRISTIN_ID_ERROR_REPORT_PREFIX;
import static no.unit.nva.cristin.mapper.CristinMainCategory.CHAPTER;
import static no.unit.nva.cristin.mapper.CristinMainCategory.EVENT;
import static no.unit.nva.cristin.mapper.CristinMainCategory.JOURNAL;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.CHAPTER_ACADEMIC;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.CONFERENCE_LECTURE;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.FEATURE_ARTICLE;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.INTERVIEW;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.JOURNAL_ARTICLE;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.LEXICAL_IMPORT;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.MUSICAL_PERFORMANCE;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.SHORT_COMMUNICATION;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.WRITTEN_INTERVIEW;
import static no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedMainCategoryException.ERROR_PARSING_MAIN_CATEGORY;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinBookOrReportPartMetadata;
import no.unit.nva.cristin.mapper.CristinMapper;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinSecondaryCategory;
import no.unit.nva.cristin.mapper.NvaPublicationPartOf;
import no.unit.nva.cristin.mapper.NvaPublicationPartOfCristinPublication;
import no.unit.nva.cristin.mapper.SearchResource2Response;
import no.unit.nva.cristin.mapper.artisticproduction.CristinArtisticProduction;
import no.unit.nva.cristin.mapper.nva.NviReport;
import no.unit.nva.cristin.mapper.nva.exceptions.AffiliationWithoutRoleException;
import no.unit.nva.cristin.mapper.nva.exceptions.ContributorWithoutAffiliationException;
import no.unit.nva.cristin.mapper.nva.exceptions.DuplicateDoiException;
import no.unit.nva.cristin.mapper.nva.exceptions.InvalidIssnRuntimeException;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedMainCategoryException;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedSecondaryCategoryException;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.MediaContribution;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.artistic.music.Concert;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.SingletonCollector;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

class CristinEntryEventConsumerTest extends AbstractCristinImportTest {

    public static final Context CONTEXT = null;
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final String RESOURCE_EXCEPTION_MESSAGE = "resourceExceptionMessage";
    public static final JavaType IMPORT_RESULT_JAVA_TYPE = constructImportResultJavaType();
    public static final String IGNORED_VALUE = "someBucket";
    public static final String NOT_IMPORTANT = "someBucketName";
    public static final int SINGLE_HIT = 1;
    public static final int SERIES_NSD_CODE = 339741;
    public static final int JOURNAL_NSD_CODE = 339717;
    public static final String ERROR_REPORT = "ERROR_REPORT";

    private CristinEntryEventConsumer handler;
    private ResourceService resourceService;
    private S3Client s3Client;
    private S3Driver s3Driver;
    private UriRetriever uriRetriever;
    private DoiDuplicateChecker doiDuplicateChecker;
    private CristinUnitsUtil cristinUnitsUtil;

    @BeforeEach
    public void init() {
        super.init();
        resourceService = getResourceServiceBuilder().build();
        s3Client = spy(new FakeS3Client());
        doReturn(S3Client.create().utilities()).when(s3Client).utilities();
        doReturn(getMockUnitsResponseBytes()).when(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        s3Driver = new S3Driver(s3Client, "ignored");
        uriRetriever = mock(UriRetriever.class);
        doiDuplicateChecker = new DoiDuplicateChecker(uriRetriever, "api.test.nva.aws.unit.no");
        cristinUnitsUtil = new CristinUnitsUtil(s3Client, "s3://some-bucket/some-key");
        handler = new CristinEntryEventConsumer(resourceService, s3Client, doiDuplicateChecker, cristinUnitsUtil);
    }

    private static ResponseBytes getMockUnitsResponseBytes() {
        var result = IoUtils.stringFromResources(Path.of("cristinUnits/units-norway.json"));
        var httpResponse = mock(ResponseBytes.class);
        when(httpResponse.asUtf8String()).thenReturn(result);
        return httpResponse;
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
        handler = new CristinEntryEventConsumer(resourceService, s3Client, doiDuplicateChecker, cristinUnitsUtil);
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);

        handler.handleRequest(sqsEvent, CONTEXT);
        var expectedExceptionName = RuntimeException.class.getSimpleName();

        var actualReport =
            extractActualReportFromS3Client(eventBody, expectedExceptionName);
        assertThat(actualReport, is(not(nullValue())));
    }

    @Test
    void shouldLogErrorWhenFailingToStorePublicationToDynamo() throws IOException {

        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client, doiDuplicateChecker, cristinUnitsUtil);

        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var event = createSqsEvent(eventBody);
        handler.handleRequest(event, CONTEXT);

        var cristinIdentifier = cristinObject.getId();
        assertThat(appender.getMessages(), containsString(ERROR_SAVING_CRISTIN_RESULT + cristinIdentifier));
        assertThat(appender.getMessages(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    void shouldReturnAnNvaPublicationEntryWhenInputIsEventWithCristinResult() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var actualPublications = handler.handleRequest(sqsEvent, CONTEXT);
        var actualPublication = actualPublications.getFirst();

        var expectedPublication = mapToPublication(cristinObject);
        injectValuesThatAreCreatedWhenSavingInDynamo(actualPublication, expectedPublication);

        assertThat(actualPublication, is(equalTo(expectedPublication)));
        assertThat(actualPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldPersistCristinIdInFileNamedWithPublicationIdentifier() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var publications = handler.handleRequest(sqsEvent, CONTEXT);
        var actualPublication = publications.getFirst();
        var expectedFileNameStoredInS3 = actualPublication.getIdentifier().toString();

        var expectedTimestamp = eventBody.getTimestamp();
        var expectedErrorFileLocation = SUCCESS_FOLDER
                                            .addChild(timestampToString(expectedTimestamp))
                                            .addChild(expectedFileNameStoredInS3);

        assertDoesNotThrow(() -> s3Driver.getFile(expectedErrorFileLocation));
    }

    @Test
    void shouldPersistNviDataWhenCristinObjectContainsNviData() throws IOException {
        var cristinObject = CristinDataGenerator.randomObjectWithReportedYear(2011);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var publications = handler.handleRequest(sqsEvent, CONTEXT);
        var actualPublication = publications.getFirst();
        var expectedFileNameStoredInS3 = actualPublication.getIdentifier().toString();

        var expectedTimestamp = eventBody.getTimestamp();
        var expectedFileLocation = NVI_FOLDER
                                       .addChild(timestampToString(expectedTimestamp))
                                       .addChild(expectedFileNameStoredInS3);
        var expectedNviReport = createExpectedNviReport(cristinObject, actualPublication);

        var file = s3Driver.getFile(expectedFileLocation);
        var nviReport = JsonUtils.dtoObjectMapper.readValue(file, NviReport.class);

        assertThat(nviReport, is(equalTo(expectedNviReport)));
    }

    @Test
    void shouldOverrideExistingNviReportWhenUpdatingPublicationThatAlreadyHasBeenNviReported() throws IOException {
        var cristinObject = CristinDataGenerator.randomObjectWithReportedYear(2011);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var publications = handler.handleRequest(sqsEvent, CONTEXT);
        var actualPublication = publications.getFirst();
        var expectedFileNameStoredInS3 = actualPublication.getIdentifier().toString();

        var expectedTimestamp = eventBody.getTimestamp();
        var expectedFileLocation = NVI_FOLDER
                                       .addChild(timestampToString(expectedTimestamp))
                                       .addChild(expectedFileNameStoredInS3);

        var nviReportBeforeUpdate = s3Driver.getFile(expectedFileLocation);
        var update = CristinDataGenerator.randomObjectWithReportedYear(2015);
        update.setId(cristinObject.getId());
        var updateEventBody = createEventBody(update);
        handler.handleRequest(createSqsEvent(updateEventBody), CONTEXT);
        var updateFileLocation = NVI_FOLDER
                                       .addChild(timestampToString(updateEventBody.getTimestamp()))
                                       .addChild(expectedFileNameStoredInS3);
        var nviReportAfterUpdate = s3Driver.getFile(updateFileLocation);

        assertThat(nviReportAfterUpdate, is(not(equalTo(nviReportBeforeUpdate))));
    }

    private NviReport createExpectedNviReport(CristinObject cristinObject, Publication publication) {
        return NviReport.builder()
                   .withScientificResource(cristinObject.getScientificResources())
                   .withCristinLocales(cristinObject.getCristinLocales())
                   .withCristinIdentifier(cristinObject.getSourceRecordIdentifier())
                   .withPublicationIdentifier(publication.getIdentifier().toString())
                   .withYearReported(cristinObject.getYearReported().toString())
                   .withPublicationDate(publication.getEntityDescription().getPublicationDate())
                   .withInstanceType(
                       publication.getEntityDescription().getReference().getPublicationInstance().getInstanceType())
                   .withReference(publication.getEntityDescription().getReference())
                   .build();
    }

    @Test
    void shouldSavePublicationToDynamoDbWhenInputIsEventWithCristinResult() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);

        handler.handleRequest(sqsEvent, CONTEXT);

        var actualPublication = fetchPublicationDirectlyFromDatabase(cristinObject.getId().toString());
        var expectedPublication = mapToPublication(cristinObject);
        injectValuesThatAreCreatedWhenSavingInDynamo(actualPublication, expectedPublication);

        var diff = JAVERS.compare(expectedPublication, actualPublication);
        assertThat(diff.prettyPrint(), actualPublication, is(equalTo(expectedPublication)));
    }

    private Publication mapToPublication(CristinObject cristinObject) {
        return new CristinMapper(cristinObject, cristinUnitsUtil, s3Client).generatePublication();
    }

    @Test
    void shouldStoreErrorReportWhenFailingToStorePublicationToDynamo() throws IOException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client, doiDuplicateChecker, cristinUnitsUtil);

        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);

        handler.handleRequest(sqsEvent, CONTEXT);
        var exceptionName = RuntimeException.class.getSimpleName();
        var actualReport =
            extractActualReportFromS3Client(eventBody, exceptionName);
        assertThat(actualReport.getException(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    @Test
    void shouldStoreErrorReportContainingS3eventUri() throws IOException {
        resourceService = resourceServiceThrowingExceptionWhenSavingResource();
        handler = new CristinEntryEventConsumer(resourceService, s3Client, doiDuplicateChecker, cristinUnitsUtil);
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var eventReference = createEventReference(eventBody);
        var sqsEvent = createSqsEvent(eventReference);
        var exceptionName = RuntimeException.class.getSimpleName();
        handler.handleRequest(sqsEvent, CONTEXT);
        var actualReport =
            extractActualReportFromS3Client(eventBody, exceptionName);
        assertThat(actualReport.getInput().getFileUri(), is(equalTo(eventReference.getUri())));
    }

    @Test
    void shouldStoreErrorReportWhenCristinMainCategoryTypeIsNotKnown() throws IOException {
        var inputData = CristinDataGenerator.objectWithCustomMainCategory(randomString());
        var eventBody = createEventBody(inputData);
        var sqsEvent = createSqsEvent(eventBody);

        handler.handleRequest(sqsEvent, CONTEXT);
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
        var sqsEvent = createSqsEvent(eventBody);

        handler = new CristinEntryEventConsumer(resourceService, s3Client, doiDuplicateChecker, cristinUnitsUtil);
        handler.handleRequest(sqsEvent, CONTEXT);
        var expectedExceptionName = RuntimeException.class.getSimpleName();
        var expectedFilePath = constructExpectedErrorFilePaths(eventBody,
                                                               expectedExceptionName);

        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(expectedFilePath);

        assertThat(file, is(not(emptyString())));
        assertThat(file, containsString(expectedExceptionName));
    }

    @Test
    void shouldSaveErrorReportInS3ContainingTheOriginalInputData() throws IOException {

        var inputData = CristinDataGenerator.objectWithCustomMainCategory(randomString());
        var eventBody = createEventBody(inputData);
        var sqsEvent = createSqsEvent(eventBody);

        handler.handleRequest(sqsEvent, CONTEXT);
        var expectedExceptionName = UnsupportedMainCategoryException.class.getSimpleName();

        var actualReport = extractActualReportFromS3Client(eventBody, expectedExceptionName);
        assertThat(actualReport.getInput().getContents(), is(equalTo(inputData)));
    }

    @Test
    void shouldStoreErrorReportWhenSecondaryCategoryTypeIsNotKnown() throws IOException {

        var inputData = CristinDataGenerator.objectWithCustomSecondaryCategory(randomString());
        var eventBody = createEventBody(inputData);
        var sqsEvent = createSqsEvent(eventBody);

        handler.handleRequest(sqsEvent, CONTEXT);
        var actualReport = extractActualReportFromS3Client(eventBody,
                                                           UnsupportedSecondaryCategoryException.class.getSimpleName());
        assertThat(actualReport.getException(),
                   containsString(UnsupportedSecondaryCategoryException.ERROR_PARSING_SECONDARY_CATEGORY));
    }

    @Test
    void shouldStoreIssnReportWhenTheBookIssnIsInvalid() throws
                                                                   IOException {
        var cristinObjectWithInvalidIssn = CristinDataGenerator.bookObjectWithInvalidIssn();
        var eventBody = createEventBody(cristinObjectWithInvalidIssn);
        var sqsEvent = createSqsEvent(eventBody);

        handler.handleRequest(sqsEvent, CONTEXT);

        var cristinId = cristinObjectWithInvalidIssn.at("/id").asText();
        var errorReportLocation =
            UnixPath.of(ERROR_REPORT).addChild("InvalidIssnException").addChild(cristinId);
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(errorReportLocation);

        var importedPublication = resourceService.getPublicationsByCristinIdentifier(cristinId);
        assertThat(importedPublication, hasSize(1));
        assertThat(file, notNullValue());
    }

    @Test
    void shouldStoreInvalidIssnRuntimeExceptionWhenTheJournalIssnIsInvalid() throws IOException {
        var cristinObjectWithInvalidIssn = CristinDataGenerator.journalObjectWithInvalidIssn();
        var eventBody = createEventBody(cristinObjectWithInvalidIssn);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);

        var actualReport = extractActualReportFromS3Client(eventBody,
                                                           InvalidIssnRuntimeException.class.getSimpleName());
        assertThat(actualReport.getException(), notNullValue());
    }

    @Test
    void handlerDoesNotThrowsExceptionRuntimeExceptionWhenTheCristinObjectHasNoContributors()
        throws IOException {
        var cristinObjectWithoutContributors = CristinDataGenerator.objectWithoutContributors();
        var eventBody = createEventBody(cristinObjectWithoutContributors);
        var sqsEvent = createSqsEvent(eventBody);
        var publications = handler.handleRequest(sqsEvent, CONTEXT);
        var actualtPublication = publications.getFirst();
        assertThat(actualtPublication.getEntityDescription().getContributors(), hasSize(0));
    }

    @Test
    void handlerThrowContributorWithoutAffiliationExceptionWhenTheCristinObjectHasContributorWithoutAffiliation()
        throws IOException {
        var cristinObjectWithoutAffiliations =
            CristinDataGenerator.objectWithContributorsWithoutAffiliation();
        var eventBody = createEventBody(cristinObjectWithoutAffiliations);
        var sqsEvent = createSqsEvent(eventBody);

        handler.handleRequest(sqsEvent, CONTEXT);
        var expectedExceptionName = ContributorWithoutAffiliationException.class.getSimpleName();

        var expectedFilePath = constructExpectedErrorFilePaths(eventBody,
                                                               expectedExceptionName);

        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(expectedFilePath);

        assertThat(file, is(not(emptyString())));
        assertThat(file, containsString(expectedExceptionName));
    }

    @Test
    void handlerStoresAffiliationWithoutARoleExceptionWhenTheCristinObjectHasAffiliationsWithoutRoles()
        throws IOException {
        var cristinObjectWithAffiliationWithoutRoles = CristinDataGenerator
                                                           .objectWithAffiliationWithoutRole();
        var eventBody = createEventBody(cristinObjectWithAffiliationWithoutRoles);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);

        var expectedExceptionName = AffiliationWithoutRoleException.class.getSimpleName();
        var expectedFilePath = constructExpectedErrorFilePaths(eventBody,
                                                               expectedExceptionName);
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(expectedFilePath);

        assertThat(file, is(not(emptyString())));
        assertThat(file, containsString(expectedExceptionName));
    }

    @Test
    void handlerCreatesFileWithCustomNameWhenCristinIdIsNotFound() throws IOException {
        var cristinObjectWithoutId = CristinDataGenerator.objectWithoutId();
        var eventBody = createEventBody(cristinObjectWithoutId);
        var sqsEvent = createSqsEvent(eventBody);

        handler.handleRequest(sqsEvent, CONTEXT);

        var s3Driver = new S3Driver(s3Client, IGNORED_VALUE);
        var errorReportFile = s3Driver.listAllFiles(ERRORS_FOLDER)
                                  .stream()
                                  .collect(SingletonCollector.collect());
        var errorReport = s3Driver.getFile(errorReportFile);

        ImportResult<FileContentsEvent<JsonNode>> actualReport =
            eventHandlerObjectMapper.readValue(errorReport, IMPORT_RESULT_JAVA_TYPE);

        assertThat(errorReportFile.toString(), containsString(UNKNOWN_CRISTIN_ID_ERROR_REPORT_PREFIX));
        assertThat(actualReport.getInput().getContents(), is(equalTo(cristinObjectWithoutId)));
    }

    @Test
    void shouldStoreErrorReportWhenInputContainsUnknownProperty() throws IOException {
        var unknownProperty = randomString();
        var objectWithUnknownProperty = CristinDataGenerator.objectWithUnknownProperty(unknownProperty);
        var eventBody = createEventBody(objectWithUnknownProperty);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);
        var expectedFilePath =
            constructExpectedErrorFilePaths(eventBody,
                                            IllegalArgumentException.class.getSimpleName());

        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(expectedFilePath);
        assertThat(file, is(not(emptyString())));
        assertThat(file, containsString(unknownProperty));
    }

    @Test
    void handleRequestDoesNotThrowExceptionWhenInputDoesNotHaveUnknownProperties() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);

        var publications = handler.handleRequest(sqsEvent, CONTEXT);
        assertThat(publications.getFirst(), notNullValue());
    }

    @Test
    void shouldBeAbleToParseCristinTagsAndRemoveDuplicates() throws IOException {
        var cristinObjectWithTags = CristinDataGenerator.objectWithTags();
        var eventBody = createEventBody(cristinObjectWithTags);
        var sqsEvent = createSqsEvent(eventBody);
        var publications = handler.handleRequest(sqsEvent, CONTEXT);
        assertThat(publications.getFirst().getEntityDescription().getTags(), hasSize(1));
    }

    @Test
    void shouldBeAbleToParseCristinHrcsCategoriesAndActivities() throws IOException {
        var cristinObjectWithCristinHrcsCategoriesAndActivities =
            CristinDataGenerator.objectWithCristinHrcsCategoriesAndActivities();
        var eventBody = createEventBody(cristinObjectWithCristinHrcsCategoriesAndActivities);
        var sqsEvent = createSqsEvent(eventBody);
        var publications = handler.handleRequest(sqsEvent, CONTEXT);
        assertThat(publications.getFirst(), notNullValue());
    }

    @Test
    void shouldPersistPartOfCristinIdToS3ForPatchPurposes() throws IOException {
        var partOfIdentifier = randomString();
        var cristinObject =
            CristinDataGenerator.randomObject(CHAPTER_ACADEMIC.getValue());
        cristinObject.setBookOrReportPartMetadata(
            CristinBookOrReportPartMetadata.builder().withPartOf(partOfIdentifier).build());

        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var publications = handler.handleRequest(sqsEvent, CONTEXT);

        var actualPublication = publications.getFirst();
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

    @Test
    void shouldNotUpdateExistingPublicationWhenImportingCristinPostTwiceAndPublicationsAreIdentical() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var publication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();
        var updatedPublication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();

        assertThat(publication.getModifiedDate(), is(equalTo(updatedPublication.getModifiedDate())));

        var reportLocation =  UnixPath.of("UPDATE").addChild(publication.getIdentifier().toString());
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);

        assertThrows(NoSuchKeyException.class,() -> s3Driver.getFile(reportLocation));
    }

    @Test
    void shouldAddNewAssociatedArtifactsToExistingCristinPublicationWhenAssociatedArtifactHasNotBeenImported() throws IOException {
        var cristinObject =
            CristinDataGenerator.createRandomMediaWithSpecifiedSecondaryCategory(INTERVIEW);
        var existingPublication = persistPublicationWithCristinId(cristinObject.getId(), Lecture.class);
        var existingAssociatedArtifacts = existingPublication.getAssociatedArtifacts();
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var updatedPublication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();
        var updatedAssociatedArtifacts = updatedPublication.getAssociatedArtifacts();

        assertTrue(updatedAssociatedArtifacts.containsAll(existingAssociatedArtifacts));
        assertThat(updatedAssociatedArtifacts.size(), is(not(equalTo(existingAssociatedArtifacts.size()))));
    }

    @Test
    void shouldAddNewFieldsWhenExistingPublicationIsEmpty() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        persistEmptyPublicationWithCristinId(cristinObject.getId());
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var updatedPublication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();

        assertNotNull(updatedPublication.getProjects());
        assertNotNull(updatedPublication.getFundings());
    }

    @Test
    void shouldOverrideCuratingInstitutionsWhenUpdatingExistingPublicationContributors() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var existingPublication = persistPublicationWithCristinId(cristinObject.getId(), Lecture.class);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var updatedPublication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();


        assertThat(existingPublication.getCuratingInstitutions(),
                is(not(equalTo(updatedPublication.getCuratingInstitutions()))));
    }

    @Test
    void shouldUpdateExistingPublicationEventWithEventPlaceWhenMissing() throws IOException {
        var cristinObject = CristinDataGenerator.createObjectWithCategory(EVENT, CONFERENCE_LECTURE);
        var existingPublication = persistPublicationWithCristinId(cristinObject.getId(), Lecture.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(new Event.Builder().withPlace(null).build());
        resourceService.updatePublication(existingPublication);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var updatedPublication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();

        assertNull(((Event) existingPublication.getEntityDescription().getReference().getPublicationContext()).getPlace());
        assertNotNull(((Event) updatedPublication.getEntityDescription().getReference().getPublicationContext()).getPlace());
    }

    @Test
    void shouldUpdateExistingPublicationEventWithEventPlaceWhenPlaceMissingLabel() throws IOException {
        var cristinObject = CristinDataGenerator.createObjectWithCategory(EVENT, CONFERENCE_LECTURE);
        var existingPublication = persistPublicationWithCristinId(cristinObject.getId(), Lecture.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(new Event.Builder().withPlace(new UnconfirmedPlace(null, null)).build());
        resourceService.updatePublication(existingPublication);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var updatedPublication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();

        assertNotNull(((Event) existingPublication.getEntityDescription().getReference().getPublicationContext()).getPlace());
        assertNotEquals(((Event) updatedPublication.getEntityDescription().getReference().getPublicationContext()).getPlace(),
                        ((Event) existingPublication.getEntityDescription().getReference().getPublicationContext()).getPlace());
    }

    @Test
    void shouldNotUpdateExistingPublicationContributorsWhenImportSamePublicationTwice() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var publication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();
        var updatedPublication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();

        assertThat(publication.getModifiedDate(), is(equalTo(updatedPublication.getModifiedDate())));

        var reportLocation =  UnixPath.of("UPDATE").addChild(publication.getIdentifier().toString());
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);

        assertThrows(NoSuchKeyException.class,() -> s3Driver.getFile(reportLocation));
    }

    @Test
    void shouldUpdateExistingPublicationContributorsWhenContributorsToIncomingPublicationDiffer() throws IOException {
        var cristinObject = CristinDataGenerator.createObjectWithCategory(EVENT, CONFERENCE_LECTURE);
        var existingPublication = persistPublicationWithCristinId(cristinObject.getId(), Lecture.class);
        existingPublication.getEntityDescription().setContributors(List.of(new Contributor.Builder().build()));
        resourceService.updatePublication(existingPublication);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var updatedPublication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();

        assertThat(updatedPublication.getEntityDescription().getContributors(),
                   is(not(equalTo(existingPublication.getEntityDescription().getContributors()))));
    }

    @Test
    void shouldUpdateExistingPublicationWithJournalArticleNumberWhenMissing() throws IOException {
        var cristinObject = CristinDataGenerator.createObjectWithCategory(JOURNAL, SHORT_COMMUNICATION);
        var existingPublication = persistPublicationWithCristinId(cristinObject.getId(), ProfessionalArticle.class);
        existingPublication.getEntityDescription().getReference().setPublicationInstance(
            new AcademicArticle(null, null, null, null));
        resourceService.updatePublication(existingPublication);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var updatedPublication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();

        assertNotNull(((JournalArticle) updatedPublication.getEntityDescription().getReference().getPublicationInstance()).getArticleNumber());
    }

    private Publication persistPublicationWithCristinId(Integer id, Class<?> instance) {
        var publication = randomPublication(instance);
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Cristin", id.toString())));
        return resourceService.createPublicationFromImportedEntry(publication);
    }

    private Publication persistEmptyPublicationWithCristinId(Integer id) {
        var publication = randomPublication();
        publication.setLink(null);
        publication.setHandle(null);
        publication.setFundings(null);
        publication.setDoi(null);
        publication.setEntityDescription(new EntityDescription().copy().withReference(new Reference()).build());
        publication.setProjects(null);
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Cristin", id.toString())));
        return resourceService.createPublicationFromImportedEntry(publication);
    }

    @Test
    void shouldPersistUpdateReportWhenUpdatingExistingPublicationAndUpdateHasEffectiveChanges() throws IOException {
        var cristinObject = CristinDataGenerator.createObjectWithCategory(EVENT, CONFERENCE_LECTURE);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var publication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();

        var newObject = CristinDataGenerator.randomObject();
        newObject.setId(cristinObject.getId());
        var newBody = createEventBody(newObject);
        var newEvent = createSqsEvent(newBody);
        handler.handleRequest(newEvent, CONTEXT).getFirst();

        var reportLocation =  UnixPath.of("UPDATE").addChild(publication.getIdentifier().toString());
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(reportLocation);

        assertThat(file, is(not(nullValue())));
    }

    @Test
    void shouldPersistExceptionWhenImportingCristinPostWithAlreadyExistingDoi() throws IOException {
        var searchResource2ResponseSingleHit = new SearchResource2Response(SINGLE_HIT);
        var singleHitOptional = Optional.of(searchResource2ResponseSingleHit.toString());
        when(uriRetriever.getRawContent(any(), any())).thenReturn(singleHitOptional);

        var cristinObject = CristinDataGenerator.randomObject(CristinSecondaryCategory.JOURNAL_ARTICLE.toString());
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);

        var actualReport = extractActualReportFromS3Client(eventBody, DuplicateDoiException.class.getSimpleName());
        assertThat(actualReport.getException(), notNullValue());
    }

    @Test
    void shouldPersistPublicationInDatabaseWhenThereIsNoDoiDuplicate() throws IOException {
        var searchResource2ResponseSingleHit = new SearchResource2Response(0);
        var nullHitsOptional = Optional.of(searchResource2ResponseSingleHit.toString());
        when(uriRetriever.getRawContent(any(), any())).thenReturn(nullHitsOptional);

        var cristinObject = CristinDataGenerator.randomObject(CristinSecondaryCategory.JOURNAL_ARTICLE.toString());
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var publications = handler.handleRequest(sqsEvent, CONTEXT);
        assertThat(publications.getFirst(), notNullValue());
    }

    @Test
    void shouldPersistInvalidIsrcErrorReportWhenImportingCristinObjectWithInvalidIsrc() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject(MUSICAL_PERFORMANCE.getValue());
        cristinObject.getCristinArtisticProduction().setIsrc("i_am_an_invalid_isrc");
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);
        var cristinIdentifier = String.valueOf(cristinObject.getId());
        var reportLocation =  UnixPath.of("ERROR_REPORT").addChild("InvalidIsrcException").addChild(cristinIdentifier);
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(reportLocation);
        var importedPublication = resourceService.getPublicationsByCristinIdentifier(cristinIdentifier);

        assertThat(importedPublication, hasSize(1));
        assertThat(file, is(not(emptyString())));
    }

    @Test
    void shouldPersistInvalidIsmnErrorReportWhenImportingCristinObjectWithInvalidIsmn() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject(MUSICAL_PERFORMANCE.getValue());
        cristinObject.getCristinArtisticProduction().setIsmn("i_am_an_invalid_ismn");
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);
        var cristinIdentifier = String.valueOf(cristinObject.getId());
        var reportLocation =
            UnixPath.of("ERROR_REPORT").addChild("InvalidIsmnException").addChild(cristinIdentifier);
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(reportLocation);
        var importedPublication = resourceService.getPublicationsByCristinIdentifier(cristinIdentifier);

        assertThat(importedPublication, hasSize(1));
        assertThat(file, is(not(emptyString())));
    }

    @Test
    void shouldPersistWrongChannelTypeErrorReportWhenCreatingJournalForNonJournal() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject(FEATURE_ARTICLE.getValue());
        cristinObject.getJournalPublication().getJournal().setNsdCode(SERIES_NSD_CODE);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);
        var cristinId = String.valueOf(cristinObject.getId());
        var expectedErrorFileLocation = UnixPath.of(ERROR_REPORT).addChild("WrongChannelTypeException").addChild(
            cristinId);
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(expectedErrorFileLocation);
        var importedPublication = resourceService.getPublicationsByCristinIdentifier(cristinId);

        assertThat(importedPublication, hasSize(1));
        assertThat(file, is(not(emptyString())));
    }

    @Test
    void shouldPersistWrongChannelTypeReportWhenCreatingSeriesForNonSeries() throws IOException {
        var cristinObject = CristinDataGenerator.randomBook();
        cristinObject.getBookOrReportMetadata().getBookSeries().setNsdCode(JOURNAL_NSD_CODE);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);
        var expectedErrorFileLocation =  UnixPath.of(ERROR_REPORT).addChild("WrongChannelTypeException").addChild(String.valueOf(cristinObject.getId()));
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(expectedErrorFileLocation);

        assertThat(file, is(not(emptyString())));
    }

    @ParameterizedTest(name = "Cristin entry with secondary category {arguments}")
    @EnumSource(value = CristinSecondaryCategory.class, mode = Mode.INCLUDE,
        names = {"ANTHOLOGY", "MONOGRAPH", "NON_FICTION_BOOK", "TEXTBOOK", "ENCYCLOPEDIA",
            "POPULAR_BOOK", "REFERENCE_MATERIAL", "RESEARCH_REPORT", "DEGREE_PHD",
            "DEGREE_MASTER", "SECOND_DEGREE_THESIS", "MEDICAL_THESIS"})
    void shouldPersistNoPublisherReportWhenCristinObjectMissesPublisherName(CristinSecondaryCategory category) throws IOException {
        var cristinObject = CristinDataGenerator.randomObject(category.getValue());
        cristinObject.setSecondaryCategory(category);
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setNsdCode(null);
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setPublisherName(null);
        cristinObject.getBookOrReportMetadata().getBookSeries().setNsdCode(randomInteger());
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);
        var expectedReportFileLocation =
            UnixPath.of(ERROR_REPORT).addChild("ChannelRegistryException").addChild(String.valueOf(cristinObject.getId()));
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(expectedReportFileLocation);
        assertThat(file, is(not(emptyString())));
    }

    @ParameterizedTest(name = "Cristin entry with secondary category {arguments}")
    @EnumSource(value = CristinSecondaryCategory.class, mode = Mode.INCLUDE,
        names = {"ANTHOLOGY", "MONOGRAPH"})
    void shouldPersistChannelRegistryExceptionReportWhenNsdCodeIsNotInChannelRegistry(CristinSecondaryCategory category) throws IOException {
        var cristinObject = CristinDataGenerator.randomObject(category.getValue());
        cristinObject.setSecondaryCategory(category);
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setPublisherName(randomString());
        cristinObject.getBookOrReportMetadata().getBookSeries().setNsdCode(randomInteger());
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);
        var cristinId = String.valueOf(cristinObject.getId());
        var expectedReportFileLocation =
            UnixPath.of(ERROR_REPORT).addChild("ChannelRegistryException").addChild(cristinId);
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(expectedReportFileLocation);
        var importedPublication = resourceService.getPublicationsByCristinIdentifier(cristinId);
        assertThat(importedPublication, hasSize(1));
        assertThat(file, is(not(emptyString())));
    }

    @Test
    void shouldPersistInvalidDoiExceptionReportWhenInvalidDoi() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        cristinObject.getBookOrReportMetadata().setDoi(randomString());
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);
        var cristinIdentifier = String.valueOf(cristinObject.getId());
        var expectedErrorFileLocation =
            UnixPath.of("ERROR_REPORT").addChild("InvalidDoiException").addChild(cristinIdentifier);
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(expectedErrorFileLocation);
        var importedPublication = resourceService.getPublicationsByCristinIdentifier(cristinIdentifier);

        assertThat(importedPublication, hasSize(1));
        assertThat(file, is(not(emptyString())));
    }

    @Test
    void shouldCreateConcertWithTimeNullWhenCristinConcertDoesNotHaveTime()
        throws IOException {
        var cristinObject = CristinDataGenerator.randomObject(MUSICAL_PERFORMANCE.getValue());
        cristinObject.setCristinArtisticProduction(readCristinArtisticProductionFromJson());
        cristinObject.getCristinArtisticProduction().getEvent().setDateFrom(null);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var publications = handler.handleRequest(sqsEvent, CONTEXT);

        var concert = ((MusicPerformance) publications.getFirst().getEntityDescription().getReference()
                                              .getPublicationInstance()).getManifestations().stream()
                          .filter(Concert.class::isInstance)
                          .map(Concert.class::cast)
                          .collect(SingletonCollector.collect());

        assertThat(concert.getTime(), is(nullValue()));
    }

    private static CristinArtisticProduction readCristinArtisticProductionFromJson() {
        return attempt(() -> Path.of("type_kunstneriskproduksjon.json"))
                   .map(IoUtils::stringFromResources)
                   .map(s -> JsonUtils.dtoObjectMapper.readValue(s, CristinArtisticProduction.class))
                   .orElseThrow();
    }

    @Test
    void shouldCreatePagesWithPagesBeginEqualToPagesEndWhenCristinJournalPagesBeginIsNull()
        throws IOException {
        var cristinObject = CristinDataGenerator.randomObject(JOURNAL_ARTICLE.getValue());
        cristinObject.getJournalPublication().setPagesBegin(null);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var publications = handler.handleRequest(sqsEvent, CONTEXT);

        var pages = ((JournalArticle) publications.getFirst().getEntityDescription().getReference()
                                          .getPublicationInstance()).getPages();

        assertThat(pages.getBegin(), is(equalTo(pages.getEnd())));
    }

    @Test
    void shouldMediaContributionDisseminationChannelAsJournalTitleWhenCristinObjectHasJournalTitle()
        throws IOException {
        var cristinObject = CristinDataGenerator.createObjectWithCategory(JOURNAL,
                                                                          WRITTEN_INTERVIEW);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        var publications = handler.handleRequest(sqsEvent, CONTEXT);

        var disseminationChannel = ((MediaContribution) publications.getFirst().getEntityDescription().getReference()
                                          .getPublicationContext()).getDisseminationChannel();

        assertThat(disseminationChannel,
                   is(equalTo(cristinObject.getJournalPublication().getJournal().getJournalTitle())));
    }

    @Test
    void shouldUpdateMediaContributionDisseminationChannelWhenExistingPublicationIsMissing()
        throws IOException {
        var cristinObject = CristinDataGenerator.createRandomMediaWithSpecifiedSecondaryCategory(WRITTEN_INTERVIEW);
        cristinObject.getMediaContribution().setMediaPlaceName(null);
        var eventBody = createEventBody(cristinObject);
        var sqsEvent = createSqsEvent(eventBody);
        handler.handleRequest(sqsEvent, CONTEXT);
        var cristinObjectWithJournalTitle = CristinDataGenerator.createObjectWithCategory(JOURNAL,
                                                                          WRITTEN_INTERVIEW);
        cristinObjectWithJournalTitle.setId(cristinObject.getId());
        var newSqsEvent = createSqsEvent(createEventBody(cristinObjectWithJournalTitle));
        var updatedPublication = handler.handleRequest(newSqsEvent, CONTEXT).getFirst();
        var reportLocation =  UnixPath.of("UPDATE").addChild(updatedPublication.getIdentifier().toString());
        var s3Driver = new S3Driver(s3Client, NOT_IMPORTANT);
        var file = s3Driver.getFile(reportLocation);

        assertThat(file, is(not(nullValue())));

        var disseminationChannel = ((MediaContribution) updatedPublication.getEntityDescription().getReference()
                                                            .getPublicationContext()).getDisseminationChannel();
        assertThat(disseminationChannel,
                   is(equalTo(cristinObjectWithJournalTitle.getJournalPublication().getJournal().getJournalTitle())));
    }

    @Test
    void shouldExtractNpiSubjectHeadingForChapter()
        throws IOException {
        var cristinObject = CristinDataGenerator.createObjectWithCategory(CHAPTER, LEXICAL_IMPORT);
        var sqsEvent = createSqsEvent(createEventBody(cristinObject));
        var publication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();
        var expectedNpiSubjectHeading =
            cristinObject.getBookOrReportPartMetadata().getSubjectField().getSubjectFieldCode().toString();

        assertThat(publication.getEntityDescription().getNpiSubjectHeading(), is(equalTo(expectedNpiSubjectHeading)));
    }

    @Test
    void shouldUpdateNpiSubjectHeadingWhenMissing()
        throws IOException {
        var cristinObject = CristinDataGenerator.createObjectWithCategory(CHAPTER, LEXICAL_IMPORT);
        var existingPublication = persistPublicationWithCristinId(cristinObject.getId(), ChapterArticle.class);
        existingPublication.getEntityDescription().setNpiSubjectHeading(null);
        resourceService.updatePublication(existingPublication);
        var sqsEvent = createSqsEvent(createEventBody(cristinObject));
        var publication = handler.handleRequest(sqsEvent, CONTEXT).getFirst();
        var expectedNpiSubjectHeading =
            cristinObject.getBookOrReportPartMetadata().getSubjectField().getSubjectFieldCode().toString();

        assertThat(publication.getEntityDescription().getNpiSubjectHeading(), is(equalTo(expectedNpiSubjectHeading)));
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

    private EventReference createEventReference(FileContentsEvent eventBody) throws IOException {
        var eventFileUri = s3Driver.insertEvent(UnixPath.EMPTY_PATH, eventBody.toJsonString());
        return new EventReference(randomString(), EVENT_SUBTOPIC, eventFileUri);
    }

    private SQSEvent createSqsEvent(FileContentsEvent eventBody) throws IOException {
        var eventReference = createEventReference(eventBody);
        var sqsEvent = new SQSEvent();
        var sqsMessage = new SQSMessage();
        sqsMessage.setBody(eventReference.toJsonString());
        sqsEvent.setRecords(List.of(sqsMessage));
        return sqsEvent;
    }

    private SQSEvent createSqsEvent(EventReference eventReference) {
        var sqsEvent = new SQSEvent();
        var sqsMessage = new SQSMessage();
        sqsMessage.setBody(eventReference.toJsonString());
        sqsEvent.setRecords(List.of(sqsMessage));
        return sqsEvent;
    }

    private <T> UnixPath constructExpectedErrorFilePaths(
        FileContentsEvent<T> event, String exceptionName) {
        return ERRORS_FOLDER
                   .addChild(exceptionName)
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
        var errorReportFilename = cristinObjectId + JSON;
        var inputFile = UriWrapper.fromUri(eventBody.getFileUri());
        var bucket = inputFile.getHost();
        return bucket.addChild(ERRORS_FOLDER)
                   .addChild(exceptionName)
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
        var errorFileUri = constructErrorFileUri(eventBody, exceptionName);
        var s3Driver = new S3Driver(s3Client, errorFileUri.getUri().getHost());
        var content = s3Driver.getFile(errorFileUri.toS3bucketPath());
        return eventHandlerObjectMapper.readValue(content, IMPORT_RESULT_JAVA_TYPE);
    }

    private ResourceService resourceServiceThrowingExceptionWhenSavingResource() {
        var resourceService = spy(getResourceServiceBuilder().build());
        doThrow(new RuntimeException(RESOURCE_EXCEPTION_MESSAGE)).when(resourceService)
            .createPublicationFromImportedEntry(any());
        return resourceService;
    }
}