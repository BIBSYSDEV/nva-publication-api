package no.sikt.nva.brage.migration.lambda;

import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.UPDATED_PUBLICATIONS_REPORTS_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageMergingRollbackHandler.ERROR_MERGING_ROLLBACK;
import static no.sikt.nva.brage.migration.lambda.BrageMergingRollbackHandler.TOPIC;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import no.sikt.nva.brage.migration.testutils.ExtendedFakeS3Client;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class BrageMergingRollbackHandlerTest {

    private static final String INPUT_BUCKET_NAME = "some-input-bucket-name";
    private final Context CONTEXT = mock(Context.class);
    private BrageMergingRollbackHandler brageMergingRollbackHandler;
    private FakeS3Client s3Client;
    private S3Driver s3Driver;

    @BeforeEach
    void init() {
        this.s3Client = new ExtendedFakeS3Client();
        this.s3Driver = new S3Driver(s3Client, INPUT_BUCKET_NAME);
        brageMergingRollbackHandler = new BrageMergingRollbackHandler(s3Client);
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
    void shouldStoreErrorReportWhenPublicationIsNoLongerParseable() throws IOException {
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
    void dummyTestShouldDoNothingWhenProcessingAPublication() throws IOException {
        //obviously this will be changed as we start to actually process the publication.
        var publication = PublicationGenerator.randomPublication().toString();
        var eventReference = createEventReference(publication);
        var awsEventBridgeEvent = createAwsEventBridgeEvent(eventReference);
        brageMergingRollbackHandler.processInput(eventReference, awsEventBridgeEvent, CONTEXT);
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
        var errorReport = event.getUri().toString().replace(UPDATED_PUBLICATIONS_REPORTS_PATH,
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
        return UnixPath.of(UPDATED_PUBLICATIONS_REPORTS_PATH +
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
        return "{}";
    }

    private AwsEventBridgeEvent<EventReference> createAwsEventBridgeEvent(EventReference eventReference) {
        var awsEventBridgeEvent = new AwsEventBridgeEvent<EventReference>();
        awsEventBridgeEvent.setDetail(eventReference);
        awsEventBridgeEvent.setTime(Instant.now());
        return awsEventBridgeEvent;
    }
}