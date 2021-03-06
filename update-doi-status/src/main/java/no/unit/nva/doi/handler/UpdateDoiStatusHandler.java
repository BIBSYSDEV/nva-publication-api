package no.unit.nva.doi.handler;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import no.unit.nva.doi.UpdateDoiStatusProcess;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.doi.update.dto.DoiUpdateHolder;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;

public class UpdateDoiStatusHandler extends DestinationsEventBridgeEventHandler<DoiUpdateHolder, Void> {

    public static final Void SUCCESSESFULLY_HANDLED_EVENT = null;
    private final ResourceService resourceService;

    /**
     * Default constructor for MainHandler.
     */
    @JacocoGenerated
    public UpdateDoiStatusHandler() {
        this(defaultResourceService());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param resourceService publicationService
     */
    public UpdateDoiStatusHandler(ResourceService resourceService) {
        super(DoiUpdateHolder.class);
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInputPayload(DoiUpdateHolder input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<DoiUpdateHolder>> event,
                                       Context context) {
        attempt(() -> updateDoi(input)).orElseThrow(this::handleFailure);
        return SUCCESSESFULLY_HANDLED_EVENT;
    }

    @JacocoGenerated
    private static ResourceService defaultResourceService() {
        return new ResourceService(
            AmazonDynamoDBClientBuilder.defaultClient(),
            Clock.systemDefaultZone());
    }

    private RuntimeException handleFailure(Failure<Void> fail) {
        Exception exception = fail.getException();
        if (exceptionInstanceOfRuntimeException(exception)) {
            return (RuntimeException) exception;
        } else {
            return new RuntimeException(exception);
        }
    }

    private boolean exceptionInstanceOfRuntimeException(Exception exception) {
        return exception instanceof RuntimeException;
    }

    private Void updateDoi(DoiUpdateHolder input) throws TransactionFailedException {
        new UpdateDoiStatusProcess(resourceService, input).updateDoiStatus();
        return null;
    }
}
