package no.sikt.nva.brage.migration.lambda;

import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.UPDATE_REPORTS_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageMergingRollbackHandler.ERROR_MERGING_ROLLBACK;
import static no.sikt.nva.brage.migration.lambda.BrageMergingRollbackHandler.NEWER_VERSION_OF_THE_PUBLICATION_EXISTS;
import static no.sikt.nva.brage.migration.lambda.BrageMergingRollbackHandler.SUCCESS_MERGING_ROLLBACK;
import static no.sikt.nva.brage.migration.lambda.BrageMergingRollbackHandler.TOPIC;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import no.sikt.nva.brage.migration.merger.BrageMergingReport;
import no.sikt.nva.brage.migration.rollback.RollBackConflictException;
import no.sikt.nva.brage.migration.testutils.ExtendedFakeS3Client;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class BrageMergingRollbackHandlerTest extends ResourcesLocalTest {

    private static final String INPUT_BUCKET_NAME = "some-input-bucket-name";
    private final Context CONTEXT = mock(Context.class);
    private BrageMergingRollbackHandler brageMergingRollbackHandler;
    private FakeS3Client s3Client;
    private S3Driver s3Driver;
    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        this.resourceService = getResourceServiceBuilder().build();
        this.s3Client = new ExtendedFakeS3Client();
        this.s3Driver = new S3Driver(s3Client, INPUT_BUCKET_NAME);
        brageMergingRollbackHandler = new BrageMergingRollbackHandler(s3Client, resourceService);
    }

    @Test
    void shouldStoreErrorReportWhenS3BucketDoesNotContainReport() throws JsonProcessingException {
        var eventReferencePointingToNothing = new EventReference(TOPIC, null, mergeReportUri());
        var awsEventBridgeEvent = createAwsEventBridgeEvent(eventReferencePointingToNothing);
        brageMergingRollbackHandler.processInput(eventReferencePointingToNothing, awsEventBridgeEvent, CONTEXT);
        var actualErrorReport = extractErrorReportFromS3Client(eventReferencePointingToNothing,
                                                               NoSuchKeyException.class.getSimpleName());
        assertThat(actualErrorReport, is(notNullValue()));
    }

    @Test
    void shouldStoreErrorReportWhenMergeReportIsNoLongerParseable() throws IOException {
        var unparsablePublication = createUnParseablePublicationJson();
        var eventReference = createEventReference(unparsablePublication);
        var awsEventBridgeEvent = createAwsEventBridgeEvent(eventReference);
        brageMergingRollbackHandler.processInput(eventReference, awsEventBridgeEvent, CONTEXT);

        var actualErrorReport = extractErrorReportFromS3Client(eventReference,
                                                               InvalidTypeIdException.class.getSimpleName());
        var exception = actualErrorReport.get("exception").asText();
        assertThat(exception, is(notNullValue()));
        var input = actualErrorReport.get("input").asText();
        assertThat(input, is(equalTo(unparsablePublication)));
    }

    @Test
    void shouldStoreExceptionIfPublicationDoesNotExist() throws IOException {
        var mergeReport = new BrageMergingReport(PublicationGenerator.randomPublication(),
                                                 PublicationGenerator.randomPublication());
        var eventReference = createEventReference(mergeReport.toString());
        var awsEventBridgeEvent = createAwsEventBridgeEvent(eventReference);
        brageMergingRollbackHandler.processInput(eventReference, awsEventBridgeEvent, CONTEXT);
        var actualErrorReport = extractErrorReportFromS3Client(eventReference,
                                                               NotFoundException.class.getSimpleName());
        var exception = actualErrorReport.get("exception").asText();
        assertThat(exception, is(notNullValue()));
    }

    @Test
    void shouldStoreExceptionWhenPublicationHasBeenModifiedSinceBrageMerging() throws IOException {
        var oldImage = PublicationGenerator.randomPublication();
        var newImageInReport = resourceService.createPublicationFromImportedEntry(
            PublicationGenerator.randomPublication(), ImportSource.BRAGE);
        var updatedAfterMerge = newImageInReport.copy().withDoi(randomDoi()).build();
        resourceService.updatePublication(updatedAfterMerge);

        var mergeReport = new BrageMergingReport(oldImage, newImageInReport);
        var eventReference = createEventReference(mergeReport.toString());
        var awsEventBridgeEvent = createAwsEventBridgeEvent(eventReference);
        brageMergingRollbackHandler.processInput(eventReference, awsEventBridgeEvent, CONTEXT);
        var actualErrorReport = extractErrorReportFromS3Client(eventReference,
                                                               RollBackConflictException.class.getSimpleName());
        var exception = actualErrorReport.get("exception").asText();
        assertThat(exception, containsString(NEWER_VERSION_OF_THE_PUBLICATION_EXISTS));
    }

    @Test
    void dummyTestShouldDoNothingWhenProcessingAPublication() throws IOException {
        var oldImage = PublicationGenerator.randomPublication();
        var newImageInReport = resourceService.createPublicationFromImportedEntry(
            PublicationGenerator.randomPublication(), ImportSource.BRAGE);
        var mergeReport = new BrageMergingReport(oldImage, newImageInReport);
        var eventReference = createEventReference(mergeReport.toString());
        var awsEventBridgeEvent = createAwsEventBridgeEvent(eventReference);
        brageMergingRollbackHandler.processInput(eventReference, awsEventBridgeEvent, CONTEXT);
    }

    @Test
    void shouldRollbackPublicationInDatabaseWhenReportPassesChecks() throws IOException, NotFoundException {
        var newImageInReport = resourceService.createPublicationFromImportedEntry(
            PublicationGenerator.randomPublication(), ImportSource.BRAGE);
        var oldImageInReport = createFromNewImage(newImageInReport);
        var mergeReport = new BrageMergingReport(oldImageInReport, newImageInReport);

        var eventReference = createEventReference(mergeReport.toString());
        var awsEventBridgeEvent = createAwsEventBridgeEvent(eventReference);
        brageMergingRollbackHandler.processInput(eventReference, awsEventBridgeEvent, CONTEXT);

        var actualPublicationAfterRollback =
            resourceService.getPublicationByIdentifier(newImageInReport.getIdentifier());
        var ignoredFields = new String[]{"modifiedDate", "createdDate", "publishedDate"};
        assertThat(actualPublicationAfterRollback, is(samePropertyValuesAs(oldImageInReport, ignoredFields)));
    }

    @Test
    void shouldPersistRollbackReportWhenPublicationWasRolledBackInDatabase() throws IOException {
        var newImageInReport = resourceService.createPublicationFromImportedEntry(
            PublicationGenerator.randomPublication(), ImportSource.BRAGE);
        var oldImageInReport = createFromNewImage(newImageInReport);
        var mergeReport = new BrageMergingReport(oldImageInReport, newImageInReport);

        var eventReference = createEventReference(mergeReport.toString());
        var awsEventBridgeEvent = createAwsEventBridgeEvent(eventReference);
        brageMergingRollbackHandler.processInput(eventReference, awsEventBridgeEvent, CONTEXT);

        var rollbackReport = extractSuccessReportFromS3(eventReference);
        assertThat(rollbackReport, is(notNullValue()));
    }

    private String extractSuccessReportFromS3(EventReference eventReference) {
        var expectedReportUri = createExpectedReportUriWrapper(eventReference);
        return s3Driver.getFile(expectedReportUri.toS3bucketPath());
    }

    private UriWrapper createExpectedReportUriWrapper(EventReference eventReference) {
        var successUri = eventReference.getUri().toString().replace(UPDATE_REPORTS_PATH, SUCCESS_MERGING_ROLLBACK);
        return UriWrapper.fromUri(successUri);
    }

    private Publication createFromNewImage(Publication newImageInReport) {
        var oldImage = PublicationGenerator.randomPublication();
        oldImage.setCuratingInstitutions(null);
        oldImage.setIdentifier(newImageInReport.getIdentifier());
        oldImage.setResourceOwner(newImageInReport.getResourceOwner());
        oldImage.setStatus(newImageInReport.getStatus());
        oldImage.setPublisher(newImageInReport.getPublisher());
        return oldImage;
    }

    private URI mergeReportUri() {
        var unixpath = getRandomMergeReportUnixPath();
        var uriwrapper = new UriWrapper("s3", INPUT_BUCKET_NAME);
        return uriwrapper.addChild(unixpath).getUri();
    }

    private JsonNode extractErrorReportFromS3Client(EventReference eventReference,
                                                    String exceptionSimpleName)
        throws JsonProcessingException {
        var errorFileUri = constructErrorFileUri(eventReference, exceptionSimpleName);
        var s3Driver = new S3Driver(s3Client, INPUT_BUCKET_NAME);
        var content = s3Driver.getFile(errorFileUri.toS3bucketPath());
        return JsonUtils.dtoObjectMapper.readTree(content);
    }

    private UriWrapper constructErrorFileUri(EventReference event,
                                             String exceptionSimpleName) {
        var errorReport = event.getUri().toString().replace(UPDATE_REPORTS_PATH,
                                                            ERROR_MERGING_ROLLBACK + "/" + exceptionSimpleName);
        return UriWrapper.fromUri(errorReport);
    }

    private EventReference createEventReference(String mergeReport) throws IOException {
        var uri =
            s3Driver.insertFile(getRandomMergeReportUnixPath(),
                                mergeReport);
        return new EventReference(TOPIC, null, uri);
    }

    private UnixPath getRandomMergeReportUnixPath() {
        return UnixPath.of(UPDATE_REPORTS_PATH +
                           "/" + randomInstitutionShortName() +
                           "/" + randomInstant().toString() +
                           "/" + randomHandlePart() +
                           "/" + randomHandlePart() +
                           "/" + randomNvaIdentifier());
    }

    private String randomNvaIdentifier() {
        return SortableIdentifier.next().toString();
    }

    private String randomHandlePart() {
        return randomInteger().toString();
    }

    private String randomInstitutionShortName() {
        return randomString();
    }

    private String createUnParseablePublicationJson() {
        return "{\"newImage\": {}}";
    }

    private AwsEventBridgeEvent<EventReference> createAwsEventBridgeEvent(EventReference eventReference) {
        var awsEventBridgeEvent = new AwsEventBridgeEvent<EventReference>();
        awsEventBridgeEvent.setDetail(eventReference);
        awsEventBridgeEvent.setTime(Instant.now());
        return awsEventBridgeEvent;
    }
}