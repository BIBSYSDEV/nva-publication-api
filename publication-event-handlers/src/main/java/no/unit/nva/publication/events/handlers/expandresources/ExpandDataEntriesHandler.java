package no.unit.nva.publication.events.handlers.expandresources;

import static java.util.Objects.isNull;
import static no.unit.nva.model.PublicationStatus.DELETED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Clock;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.publication.events.handlers.tickets.identityservice.CustomerDto;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ExpandDataEntriesHandler
    extends DestinationsEventBridgeEventHandler<EventReference, EventReference> {

    public static final String ERROR_EXPANDING_RESOURCE_WARNING = "Error expanding resource:";
    public static final String HANDLER_EVENTS_FOLDER = "PublicationService-DataEntryExpansion";
    public static final String EXPANDED_ENTRY_UPDATED_EVENT_TOPIC = "PublicationService.ExpandedDataEntry.Update";
    public static final String EXPANDED_ENTRY_DELETE_EVENT_TOPIC = "PublicationService.ExpandedDataEntry.Delete";

    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String EMPTY_EVENT_TOPIC = "Event.Empty";
    public static final String PUBLICATION_SERVICE_DATA_ENTRY_DELETION = "Publicationservice-DataEntryDeletion";
    public static final String APPLICAITON_LD_JSON = "applicaiton/ld+json";
    public static final String COULD_NOT_RETRIEVE_CUSTOMER = "could not retrieve customer {}";
    private static final Logger logger = LoggerFactory.getLogger(ExpandDataEntriesHandler.class);
    private final S3Driver s3Driver;
    private final ResourceExpansionService resourceExpansionService;

    private final RawContentRetriever backendClient;

    @JacocoGenerated
    public ExpandDataEntriesHandler() {
        this(new S3Driver(EVENTS_BUCKET), defaultResourceExpansionService(),
             new AuthorizedBackendUriRetriever(BACKEND_CLIENT_AUTH_URL, BACKEND_CLIENT_SECRET_NAME));
    }

    public ExpandDataEntriesHandler(S3Client s3Client, ResourceExpansionService resourceExpansionService,
                                    RawContentRetriever backendClient) {
        this(new S3Driver(s3Client, EVENTS_BUCKET), resourceExpansionService, backendClient);
    }

    private ExpandDataEntriesHandler(S3Driver s3Driver, ResourceExpansionService resourceExpansionService,
                                     RawContentRetriever backendClient) {
        super(EventReference.class);
        this.s3Driver = s3Driver;
        this.resourceExpansionService = resourceExpansionService;
        this.backendClient = backendClient;
    }

    @Override
    protected EventReference processInputPayload(EventReference input,
                                                 AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                                 Context context) {
        var blobObject = readBlobFromS3(input);
        var eventReference = createEventReference(blobObject);
        logger.info("EventReference: {}", eventReference.toJsonString());
        return eventReference;
    }

    @JacocoGenerated
    private static ResourceExpansionService defaultResourceExpansionService() {
        return new ResourceExpansionServiceImpl(defaultResourceService(),
                                                TicketService.defaultService(),
                                                new UriRetriever());
    }

    @JacocoGenerated
    private static ResourceService defaultResourceService() {
        return new ResourceService(DEFAULT_DYNAMODB_CLIENT, Clock.systemDefaultZone());
    }

    private EventReference createEventReference(DataEntryUpdateEvent blobObject) {
        if (shouldBeDeleted(blobObject.getNewData())) {
            return createDeleteEventReference(blobObject.getNewData());
        } else if (shouldBeEnriched(blobObject.getNewData())) {
            return createEnrichedEventReference(blobObject.getNewData()).orElseGet(this::emptyEvent);
        } else {
            return emptyEvent();
        }
    }

    private EventReference createDeleteEventReference(Entity newData) {
        var resource = (Resource) newData;
        var publication = resource.toPublication();
        var uri = insertDeleteEventBodyToS3(publication.toString());
        return new EventReference(EXPANDED_ENTRY_DELETE_EVENT_TOPIC, uri);
    }

    private Optional<EventReference> createEnrichedEventReference(Entity newData) {
        return enrich(newData)
                   .map(this::insertEnrichEventBodyToS3)
                   .map(uri -> new EventReference(EXPANDED_ENTRY_UPDATED_EVENT_TOPIC, uri));
    }

    private boolean shouldBeDeleted(Entity entity) {
        return getPublicationStatus(entity).map(DELETED::equals).orElse(false);
    }

    private Optional<PublicationStatus> getPublicationStatus(Entity entity) {
        if (entity instanceof Resource) {
            Resource resource = (Resource) entity;
            return Optional.of(resource.getStatus());
        } else {
            return Optional.empty();
        }
    }

    private DataEntryUpdateEvent readBlobFromS3(EventReference input) {
        var blobString = s3Driver.readEvent(input.getUri());
        return DataEntryUpdateEvent.fromJson(blobString);
    }

    private EventReference emptyEvent() {
        return new EventReference(EMPTY_EVENT_TOPIC, null);
    }

    private boolean shouldBeEnriched(Entity entry) {
        if (isNull(entry)) {
            return false;
        }
        var publicationStatus = getPublicationStatus(entry);
        if (publicationStatus.isPresent()) {
            return PUBLISHED.equals(publicationStatus.get()) || PUBLISHED_METADATA.equals(publicationStatus.get());
        } else if (entry instanceof DoiRequest) {
            return isDoiRequestReadyForEvaluation((DoiRequest) entry);
        } else if (entry instanceof PublishingRequestCase) {
            return publishingRequestTicketIsNotAutomaticallyApproved((PublishingRequestCase) entry);
        } else {
            return true;
        }
    }

    private boolean publishingRequestTicketIsNotAutomaticallyApproved(PublishingRequestCase entry) {
        return !customerAllowsPublishing(entry);
    }

    private boolean customerAllowsPublishing(PublishingRequestCase publishingRequest) {
        var customerId = publishingRequest.getCustomerId();
        var customer = fetchCustomer(customerId);
        return customer.map(CustomerDto::customerAllowsRegistratorsToPublishDataAndMetadata).orElse(false);
    }

    private Optional<CustomerDto> fetchCustomer(URI customerId) {
        return attempt(() -> backendClient.getRawContent(customerId, APPLICAITON_LD_JSON))
                   .map(Optional::get)
                   .map(response -> JsonUtils.dtoObjectMapper.readValue(response, CustomerDto.class))
                   .map(Optional::of)
                   .orElse(this::logFailure);
    }

    private Optional<CustomerDto> logFailure(Failure<Optional<CustomerDto>> optionalFailure) {
        logger.error(COULD_NOT_RETRIEVE_CUSTOMER, optionalFailure.getException());
        return Optional.empty();
    }

    private boolean isDoiRequestReadyForEvaluation(DoiRequest doiRequest) {
        return PUBLISHED.equals(doiRequest.getResourceStatus());
    }

    private URI insertDeleteEventBodyToS3(String body) {
        return attempt(
            () -> s3Driver.insertEvent(UnixPath.of(PUBLICATION_SERVICE_DATA_ENTRY_DELETION), body)).orElseThrow();
    }

    private URI insertEnrichEventBodyToS3(String string) {
        return attempt(() -> s3Driver.insertEvent(UnixPath.of(HANDLER_EVENTS_FOLDER), string)).orElseThrow();
    }

    private Optional<String> enrich(Entity newData) {
        return attempt(() -> createExpandedResourceUpdate(newData))
                   .toOptional(fail -> logError(fail, newData));
    }

    private String createExpandedResourceUpdate(Entity input) throws JsonProcessingException, NotFoundException {
        return resourceExpansionService.expandEntry(input).toJsonString();
    }

    private void logError(Failure<?> fail, Entity input) {
        Exception exception = fail.getException();
        logger.warn(ERROR_EXPANDING_RESOURCE_WARNING + input.getIdentifier(), exception);
    }
}
