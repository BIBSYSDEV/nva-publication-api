package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.patcher.exception.ExceptionHandling.castToCorrectRuntimeException;
import static no.unit.nva.publication.PublicationServiceConfig.defaultDynamoDbClient;
import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultS3Client;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import no.unit.nva.cristin.mapper.NvaPublicationPartOfCristinPublication;
import no.unit.nva.cristin.patcher.CristinPatcher;
import no.unit.nva.cristin.patcher.exception.ParentPublicationException;
import no.unit.nva.cristin.patcher.model.ParentAndChild;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinPatchEventConsumer extends EventHandler<EventReference, Publication> {

    public static final String TOPIC = "PublicationService.DataImport.Filename";
    public static final String SUBTOPIC = "PublicationService.CristinData.PatchEntry";

    public static final String WRONG_SUBTOPIC_ERROR_TEMPLATE = "Unexpected subtopic: %s. Expected subtopic is: %s.";

    public static final String INVALID_PARENT_MESSAGE = "Could not retrieve a single valid parent publciation";

    private static final Clock CLOCK = Clock.systemDefaultZone();

    private static final Logger logger = LoggerFactory.getLogger(CristinPatchEventConsumer.class);

    private final ResourceService resourceService;
    private final S3Client s3Client;

    @JacocoGenerated
    public CristinPatchEventConsumer() {
        this(defaultDynamoDbClient(), defaultS3Client());
    }

    @JacocoGenerated
    protected CristinPatchEventConsumer(AmazonDynamoDB dynamoDbClient, S3Client s3Client) {
        this(new ResourceService(dynamoDbClient, CLOCK), s3Client);
    }

    public CristinPatchEventConsumer(ResourceService resourceService, S3Client s3Client) {
        super(EventReference.class);
        this.resourceService = resourceService;
        this.s3Client = s3Client;
    }

    @Override
    protected Publication processInput(EventReference input, AwsEventBridgeEvent<EventReference> event,
                                       Context context) {
        validateEvent(event);
        var eventBody = readEventBody(input);
        return attempt(() -> retrieveChildAndParentPublications(eventBody))
                   .map(CristinPatcher::updateChildPublication)
                   .map(ParentAndChild::getChildPublication)
                   .orElseThrow(fail -> castToCorrectRuntimeException(fail.getException()));
    }


    private ParentAndChild retrieveChildAndParentPublications(NvaPublicationPartOfCristinPublication eventBody)
        throws NotFoundException {

        var childPublication = getChildPublication(eventBody);
        var parentPublication = getParentPublication(eventBody);
        return new ParentAndChild(childPublication, parentPublication);
    }

    private Publication getChildPublication(
        NvaPublicationPartOfCristinPublication nvaPublicationPartOfCristinPublication) throws NotFoundException {
        return resourceService.getPublicationByIdentifier(
            new SortableIdentifier(nvaPublicationPartOfCristinPublication.getNvaPublicationIdentifier()));
    }

    private Publication getParentPublication(
        NvaPublicationPartOfCristinPublication nvaPublicationPartOfCristinPublication) {
        var parentPublications = resourceService.getPublicationsByCristinIdentifier(
            nvaPublicationPartOfCristinPublication.getPartOf().getCristinId());
        return attempt(() -> parentPublications
                                 .stream()
                                 .collect(SingletonCollector.collect())).orElseThrow(
            fail -> new ParentPublicationException(INVALID_PARENT_MESSAGE, fail.getException()));
    }

    private NvaPublicationPartOfCristinPublication readEventBody(EventReference input) {
        var s3Driver = new S3Driver(s3Client, input.extractBucketName());
        var json = s3Driver.readEvent(input.getUri());
        return NvaPublicationPartOfCristinPublication.fromJson(json);
    }

    private void validateEvent(AwsEventBridgeEvent<EventReference> event) {
        if (!TOPIC.equalsIgnoreCase(event.getDetail().getTopic())) {
            String errorMessage = messageIndicatingTheCorrectTopicType(event);
            logger.info(event.toJsonString());
            throw new IllegalArgumentException(errorMessage);
        }
        if (!SUBTOPIC.equalsIgnoreCase(event.getDetail().getSubtopic())) {
            String errorMessage = messageIndicatingTheCorrectSubtopicType(event);
            logger.info(event.toJsonString());
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String messageIndicatingTheCorrectTopicType(AwsEventBridgeEvent<EventReference> event) {
        return String.format("Unexpected topic: %s. Expected topic is: %s", event.getDetail().getTopic(), TOPIC);
    }

    private String messageIndicatingTheCorrectSubtopicType(AwsEventBridgeEvent<EventReference> event) {
        return String.format(WRONG_SUBTOPIC_ERROR_TEMPLATE, event.getDetail().getSubtopic(), SUBTOPIC);
    }
}
