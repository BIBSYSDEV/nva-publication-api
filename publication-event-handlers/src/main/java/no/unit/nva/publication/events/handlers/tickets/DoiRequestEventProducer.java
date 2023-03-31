package no.unit.nva.publication.events.handlers.tickets;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.TicketEntry.TICKET_WITHOUT_REFERENCE_TO_PUBLICATION_ERROR;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.doi.requirements.DoiResourceRequirements;
import no.unit.nva.publication.doi.update.dto.DoiRegistrarEntryFields;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.bodies.DoiMetadataUpdateEvent;
import no.unit.nva.publication.exception.InvalidInputException;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Sends messages to DoiRegistrar service for creating and updating DOIs.
 */
public class DoiRequestEventProducer
    extends DestinationsEventBridgeEventHandler<EventReference, DoiMetadataUpdateEvent> {

    public static final Duration MIN_INTERVAL_FOR_REREQUESTING_A_DOI = Duration.ofSeconds(10);
    public static final String DOI_REQUEST_HAS_NO_IDENTIFIER = "DoiRequest has no identifier";
    public static final String HEAD = "HEAD";
    public static final String NVA_API_DOMAIN = "https://" + readDomainName();
    public static final DoiMetadataUpdateEvent EMPTY_EVENT = DoiMetadataUpdateEvent.empty();
    public static final String INPUT = "INPUT {}";
    public static final String OUTPUT = "OUTPUT {}";
    protected static final Integer HTTP_FOUND = 302;
    private static final String HANDLER_DOES_NOT_DEAL_WITH_DELETIONS = "Handler does not deal with deletions";
    private static final Logger logger = LoggerFactory.getLogger(DoiRequestEventProducer.class);
    private final ResourceService resourceService;
    private final HttpClient httpClient;
    private final S3Client s3Client;

    @JacocoGenerated
    public DoiRequestEventProducer() {
        this(ResourceService.defaultService(), HttpClient.newHttpClient(), S3Driver.defaultS3Client().build());
    }

    public DoiRequestEventProducer(ResourceService resourceService, HttpClient httpClient, S3Client s3Client) {
        super(EventReference.class);
        this.resourceService = resourceService;
        this.httpClient = httpClient;
        this.s3Client = s3Client;
    }

    @Override
    protected DoiMetadataUpdateEvent processInputPayload(
        EventReference inputEvent,
        AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
        Context context) {
        var s3Driver = new S3Driver(s3Client, inputEvent.getUri().getHost());
        var eventString = s3Driver.readEvent(inputEvent.getUri());
        logger.info(INPUT, eventString);
        var eventBody = DataEntryUpdateEvent.fromJson(eventString);
        validate(eventBody);
        var outputEvent = isEffectiveChange(eventBody)
                              ? propagateEvent(eventBody)
                              : EMPTY_EVENT;
        logger.info(OUTPUT, outputEvent.toJsonString());
        return outputEvent;
    }

    private static String readDomainName() {
        return new Environment().readEnv("DOMAIN_NAME");
    }

    private DoiMetadataUpdateEvent propagateEvent(DataEntryUpdateEvent input) {
        var newEntry = input.getNewData();

        if (newEntry instanceof Resource && DoiResourceRequirements.publicationSatisfiesDoiRequirements(
            ((Resource) newEntry).toPublication())) {
            return createDoiMetadataUpdateEvent((Resource) newEntry);
        }
        if (newEntry instanceof DoiRequest) {
            return createDoiMetadataUpdateEvent((DoiRequest) newEntry);
        }
        return EMPTY_EVENT;
    }

    private DoiMetadataUpdateEvent createDoiMetadataUpdateEvent( DoiRequest newEntry) {
        if (isDoiRequestApproval(newEntry)
            && DoiResourceRequirements.publicationSatisfiesDoiRequirements(
            newEntry.toPublication(resourceService))) {
            return createEventForMakingDoiFindable(newEntry);
        }
        return EMPTY_EVENT;
    }

    private DoiMetadataUpdateEvent createDoiMetadataUpdateEvent(Resource newEntry) {
        if (resourceWithFindableDoiHasBeenUpdated(newEntry)) {
            var publication = newEntry.toPublication();
            return DoiMetadataUpdateEvent.createUpdateDoiEvent(publication);
        }
        return EMPTY_EVENT;
    }

    private DoiMetadataUpdateEvent createEventForMakingDoiFindable(DoiRequest newEntry) {
        return DoiMetadataUpdateEvent.createUpdateDoiEvent(newEntry.toPublication(resourceService));
    }

    private boolean isDoiRequestApproval(DoiRequest newEntry) {
        return matchStatus(newEntry, TicketStatus.COMPLETED);
    }

    private Boolean matchStatus(DoiRequest oldEntry, TicketStatus approved) {
        return Optional.ofNullable(oldEntry)
                   .map(DoiRequest::getStatus)
                   .map(approved::equals)
                   .orElse(false);
    }

    private boolean resourceWithFindableDoiHasBeenUpdated(Resource newEntry) {
        return hasDoi(newEntry) && doiIsFindable(newEntry.getDoi());
    }

    private boolean doiIsFindable(URI doi) {
        var request = HttpRequest.newBuilder(doi).method(HEAD, BodyPublishers.noBody()).build();
        return attempt(() -> httpClient.send(request, BodyHandlers.ofString()))
                   .map(HttpResponse::statusCode)
                   .map(HTTP_FOUND::equals)
                   .toOptional()
                   .orElse(false);
    }

    private boolean hasDoi(Resource newEntry) {
        return nonNull(newEntry.getDoi());
    }

    private void validate(DataEntryUpdateEvent event) {
        validateEvent(event);
        validateDoiRequest(event);
    }

    private void validateEvent(DataEntryUpdateEvent event) {
        if (eventIsADeletion(event)) {
            throw new InvalidInputException(HANDLER_DOES_NOT_DEAL_WITH_DELETIONS);
        }
    }

    private void validateDoiRequest(DataEntryUpdateEvent event) {
        if (event.getNewData() instanceof DoiRequest) {
            var doiRequest = (DoiRequest) event.getNewData();
            if (eventCannotBeReferenced(doiRequest)) {
                throw new IllegalStateException(DOI_REQUEST_HAS_NO_IDENTIFIER);
            }
            if (eventHasNoPublication(doiRequest)) {
                throw new IllegalStateException(TICKET_WITHOUT_REFERENCE_TO_PUBLICATION_ERROR);
            }
        }
    }

    private boolean eventHasNoPublication(DoiRequest doiRequest) {
        return isNull(doiRequest.getPublicationDetails());
    }

    private boolean eventIsADeletion(DataEntryUpdateEvent event) {
        return isNull(event.getNewData());
    }

    private boolean eventCannotBeReferenced(DoiRequest doiRequest) {
        return isNull(doiRequest.getIdentifier());
    }

    private Publication toPublication(Entity dataEntry) {
        return nonNull(dataEntry)
                   ? dataEntry.toPublication(resourceService)
                   : null;
    }

    private boolean isEffectiveChange(DataEntryUpdateEvent updateEvent) {
        return isDoiRequestUpdate(updateEvent) || doiRelatedDataDifferFromOld(updateEvent);
    }

    private boolean doiRelatedDataDifferFromOld(DataEntryUpdateEvent updateEvent) {
        var newPublication = toPublication(updateEvent.getNewData());
        var oldPublication = toPublication(updateEvent.getOldData());
        if (nonNull(newPublication)) {
            return newDoiRelatedMetadataDifferFromOld(newPublication, oldPublication);
        }
        return false;
    }

    private boolean isDoiRequestUpdate(DataEntryUpdateEvent updateEvent) {
        return updateEvent.getNewData() instanceof DoiRequest;
    }

    private boolean newDoiRelatedMetadataDifferFromOld(Publication newPublication, Publication oldPublication) {
        DoiRegistrarEntryFields doiInfoNew = DoiRegistrarEntryFields.fromPublication(newPublication);
        DoiRegistrarEntryFields doiInfoOld = extractInfoFromOldInstance(oldPublication);
        return !doiInfoNew.equals(doiInfoOld);
    }

    private DoiRegistrarEntryFields extractInfoFromOldInstance(Publication oldPublication) {
        return nonNull(oldPublication) ? DoiRegistrarEntryFields.fromPublication(oldPublication) : null;
    }
}
