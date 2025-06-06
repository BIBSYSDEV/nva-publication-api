package no.unit.nva.publication.events.handlers.identifiers;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.HTTPS;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.HashSet;
import java.util.Set;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.additionalidentifiers.HandleIdentifier;
import no.unit.nva.model.additionalidentifiers.SourceName;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@SuppressWarnings("PMD.CouplingBetweenObjects")
public class HandleIdentifierEventHandler
    implements RequestHandler<SQSEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(HandleIdentifierEventHandler.class);
    public static final String LEGACY_HANDLE_SOURCE_NAME = "handle";
    public static final TypeReference<AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>>> SQS_VALUE_TYPE_REF =
        new TypeReference<>() {
    };
    private final String backendClientAuthUrl;
    private final String backendClientSecretName;
    private final ResourceService resourceService;
    private final S3Driver s3Driver;
    public static final String RESOURCE_UPDATE_EVENT_TOPIC = "PublicationService.Resource.Update";
    private static final Set<PublicationStatus> PUBLISHED_STATUSES = Set.of(PUBLISHED,
                                                                            PUBLISHED_METADATA);
    private static final String REGISTRATION_PATH = "registration";
    private final SecretsReader secretsManagerClient;
    private final HandleService handleService;
    private final String frontendDomain;

    @JacocoGenerated
    public HandleIdentifierEventHandler() {
        this(ResourceService.defaultService(),
             S3Driver.defaultS3Client().build(),
             new Environment(),
             HttpClient.newBuilder().build(),
             SecretsReader.defaultSecretsManagerClient());
    }

    protected HandleIdentifierEventHandler(ResourceService resourceService,
                                           S3Client s3Client,
                                           Environment environment,
                                           HttpClient httpClient,
                                           SecretsManagerClient secretsManagerClient) {
        var apiDomain = environment.readEnv("API_DOMAIN");
        var handleBasePath = environment.readEnv("HANDLE_BASE_PATH");
        this.backendClientSecretName = environment.readEnv("BACKEND_CLIENT_SECRET_NAME");
        this.backendClientAuthUrl = environment.readEnv("BACKEND_CLIENT_AUTH_URL");
        this.frontendDomain = environment.readEnv("NVA_FRONTEND_DOMAIN");
        this.resourceService = resourceService;
        this.s3Driver = new S3Driver(s3Client, PublicationEventsConfig.EVENTS_BUCKET);
        this.secretsManagerClient = new SecretsReader(secretsManagerClient);
        AuthorizedBackendClient authorizedBackendClient = AuthorizedBackendClient.prepareWithCognitoCredentials(
            httpClient,
            fetchCredentials());
        this.handleService = new HandleService(authorizedBackendClient, apiDomain, handleBasePath);
    }

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        sqsEvent.getRecords()
            .stream()
            .map(HandleIdentifierEventHandler::parseEventReference)
            .forEach(this::processInputPayload);
        return null;
    }

    private static EventReference parseEventReference(SQSMessage sqs) {
        logger.info("Processing sqsEvent: {}", sqs.getBody());
        try {
            return JsonUtils.dtoObjectMapper
                       .readValue(sqs.getBody(), SQS_VALUE_TYPE_REF)
                       .getDetail()
                       .getResponsePayload();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private CognitoCredentials fetchCredentials() {
        var credentials = secretsManagerClient.fetchClassSecret(backendClientSecretName,
                                                                BackendClientCredentials.class);
        var uri = UriWrapper.fromHost(backendClientAuthUrl).getUri();
        return new CognitoCredentials(credentials::getId, credentials::getSecret, uri);
    }

    protected Void processInputPayload(EventReference input) {
        var eventBlob = s3Driver.readEvent(input.getUri());

        if (RESOURCE_UPDATE_EVENT_TOPIC.equals(input.getTopic())) {
            var resourceUpdate = parseResourceUpdateInput(eventBlob);
            if (isPublished(resourceUpdate) && isMissingHandle(resourceUpdate)) {
                logger.info("Creating handle for publication: {}", resourceUpdate.getIdentifier());
                var publication = fetchPublication(resourceUpdate.getIdentifier());
                var additionalIdentifiers = new HashSet<>(publication.getAdditionalIdentifiers());
                var handle = createNewHandle(getLandingPage(publication.getIdentifier()));
                logger.info("Created handle: {}", handle.value());
                additionalIdentifiers.add(handle);
                publication.setAdditionalIdentifiers(additionalIdentifiers);
                attempt(() -> resourceService.updatePublication(publication));
            }
        }
        return null;
    }

    private URI getLandingPage(SortableIdentifier identifier) {
        return new UriWrapper(HTTPS, frontendDomain).addChild(REGISTRATION_PATH)
                   .addChild(identifier.toString())
                   .getUri();
    }

    private static boolean isMissingHandle(Resource resourceUpdate) {
        return !resourceContainsHandle(resourceUpdate) && !resourceContainsLegacyHandle(resourceUpdate);
    }

    private static boolean resourceContainsHandle(Resource resourceUpdate) {
        return resourceUpdate.getAdditionalIdentifiers().stream().anyMatch(HandleIdentifier.class::isInstance);
    }

    // this method can be deleted after AdditionalIdentifier handles is migrated to HandleIdentifier
    private static boolean resourceContainsLegacyHandle(Resource resourceUpdate) {
        return resourceUpdate.getAdditionalIdentifiers()
                   .stream()
                   .filter(AdditionalIdentifier.class::isInstance)
                   .anyMatch(a -> LEGACY_HANDLE_SOURCE_NAME.equals(a.sourceName()));
    }

    private static boolean isPublished(Resource resourceUpdate) {
        return PUBLISHED_STATUSES.contains(resourceUpdate.getStatus());
    }

    private HandleIdentifier createNewHandle(URI link) {
        return new HandleIdentifier(new SourceName("nva", "sikt"), handleService.createHandle(link));
    }

    private Publication fetchPublication(SortableIdentifier publicationIdentifier) {
        return attempt(() -> resourceService.getPublicationByIdentifier(publicationIdentifier)).orElseThrow();
    }

    private static Resource parseResourceUpdateInput(String eventBlob) {
        var entryUpdate = DataEntryUpdateEvent.fromJson(eventBlob);
        return (Resource) entryUpdate.getNewData();
    }
}
