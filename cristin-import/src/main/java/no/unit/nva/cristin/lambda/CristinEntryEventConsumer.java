package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.lambda.ApplicationConstants.MAX_SLEEP_TIME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import java.util.Random;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinEntryEventConsumer extends EventHandler<CristinObject, Publication> {

    public static final String WRONG_DETAIL_TYPE_ERROR_TEMPLATE =
        "Unexpected detail-type: %s. Expected detail-type is: %s.";
    public static final int MAX_EFFORTS = 10;
    public static final String ERROR_SAVING_CRISTIN_RESULT = "Could not save cristin result with ID: ";
    private static final Logger logger = LoggerFactory.getLogger(CristinEntryEventConsumer.class);
    private final ResourceService resourceService;

    protected CristinEntryEventConsumer(AmazonDynamoDB dynamoDbClient) {
        this(new ResourceService(dynamoDbClient, Clock.systemDefaultZone()));
    }

    protected CristinEntryEventConsumer(ResourceService resourceService) {
        super(CristinObject.class);
        this.resourceService = resourceService;
    }

    public void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Publication processInput(CristinObject input, AwsEventBridgeEvent<CristinObject> event, Context context) {
        validateEvent(event);
        Publication publication = input.toPublication();

        Try<Publication> attemptSave = persistInDatabase(publication);
        return attemptSave.orElseThrow(fail -> handleSavingError(fail, input));
    }

    private Try<Publication> persistInDatabase(Publication publication) {
        Try<Publication> attemptSave = tryToPersistInDatabase(publication);
        int efforts = 0;
        int sleepTime = new Random().nextInt(MAX_SLEEP_TIME);
        while (attemptSave.isFailure() && efforts < MAX_EFFORTS) {
            attemptSave = tryToPersistInDatabase(publication);
            efforts++;
            sleep(sleepTime);
        }
        return attemptSave;
    }

    private Try<Publication> tryToPersistInDatabase(Publication publication) {
        return attempt(() -> resourceService.createPublicationWithPredefinedCreationDate(publication));
    }

    private void validateEvent(AwsEventBridgeEvent<CristinObject> event) {
        if (!CristinEntriesEventEmitter.EVENT_DETAIL_TYPE.equals(event.getDetailType())) {
            String errorMessage = String.format(WRONG_DETAIL_TYPE_ERROR_TEMPLATE,
                                                event.getDetailType(),
                                                CristinEntriesEventEmitter.EVENT_DETAIL_TYPE);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private RuntimeException handleSavingError(Failure<Publication> fail, CristinObject cristinObject) {
        String errorMessage = ERROR_SAVING_CRISTIN_RESULT + cristinObject.getId();
        logger.error(errorMessage, fail.getException());
        return new RuntimeException(errorMessage, fail.getException());
    }
}
