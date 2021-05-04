package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.lambda.ApplicationConstants.MAX_SLEEP_TIME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import java.util.Random;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinEntryEventConsumer extends EventHandler<CristinObjectEvent, Publication> {

    public static final String WRONG_DETAIL_TYPE_ERROR_TEMPLATE =
        "Unexpected detail-type: %s. Expected detail-type is: %s.";
    public static final int MAX_EFFORTS = 10;
    public static final String ERROR_SAVING_CRISTIN_RESULT = "Could not save cristin result with ID: ";
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final Logger logger = LoggerFactory.getLogger(CristinEntryEventConsumer.class);
    private final ResourceService resourceService;

    @JacocoGenerated
    public CristinEntryEventConsumer() {
        this(defaultDynamoDbClient());
    }

    @JacocoGenerated
    protected CristinEntryEventConsumer(AmazonDynamoDB dynamoDbClient) {
        this(new ResourceService(dynamoDbClient, Clock.systemDefaultZone()));
    }

    protected CristinEntryEventConsumer(ResourceService resourceService) {
        super(CristinObjectEvent.class);
        this.resourceService = resourceService;
    }

    @Override
    protected Publication processInput(CristinObjectEvent input,
                                       AwsEventBridgeEvent<CristinObjectEvent> event,
                                       Context context) {
        validateEvent(event);
        CristinObject cristinObject = extractCristinObject(input);
        Publication publication = cristinObject.toPublication();
        Try<Publication> attemptSave = persistInDatabase(publication);
        return attemptSave.orElseThrow(fail -> handleSavingError(fail, cristinObject));
    }

    @JacocoGenerated
    private static AmazonDynamoDB defaultDynamoDbClient() {
        return AmazonDynamoDBClientBuilder
                   .standard()
                   .withRegion(ApplicationConstants.AWS_REGION.id())
                   .build();
    }

    private CristinObject extractCristinObject(FileContentsEvent<CristinObject> input) {
        CristinObject cristinObject = input.getContents();
        cristinObject.setPublicationOwner(input.getPublicationsOwner());
        return cristinObject;
    }

    private void validateEvent(AwsEventBridgeEvent<CristinObjectEvent> event) {
        if (!CristinEntriesEventEmitter.EVENT_DETAIL_TYPE.equals(event.getDetailType())) {
            String errorMessage = String.format(WRONG_DETAIL_TYPE_ERROR_TEMPLATE,
                                                event.getDetailType(),
                                                CristinEntriesEventEmitter.EVENT_DETAIL_TYPE);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private Try<Publication> persistInDatabase(Publication publication) {
        Try<Publication> attemptSave = tryPersistingInDatabase(publication);

        for (int efforts = 0; shouldTryAgain(attemptSave, efforts); efforts++) {
            attemptSave = tryPersistingInDatabase(publication);
            sleep(RANDOM.nextInt(MAX_SLEEP_TIME));
        }
        return attemptSave;
    }

    private boolean shouldTryAgain(Try<Publication> attemptSave, int efforts) {
        return attemptSave.isFailure() && efforts < MAX_EFFORTS;
    }

    private Try<Publication> tryPersistingInDatabase(Publication publication) {
        return attempt(() -> resourceService.createPublicationWithPredefinedCreationDate(publication));
    }

    private void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private RuntimeException handleSavingError(Failure<Publication> fail, CristinObject cristinObject) {
        String errorMessage = ERROR_SAVING_CRISTIN_RESULT + cristinObject.getId();
        logger.error(errorMessage, fail.getException());
        return new RuntimeException(errorMessage, fail.getException());
    }
}
