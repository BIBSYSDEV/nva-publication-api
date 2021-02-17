package no.unit.nva.publication.messages.create;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.HttpHeaders;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateMessageHandler extends ApiGatewayHandler<CreateMessageRequest, Void> {

    public static final Logger LOGGER = LoggerFactory.getLogger(CreateMessageHandler.class);
    private final MessageService messageService;
    private final ResourceService resourceService;

    public CreateMessageHandler() {
        this(defaultClient(), new Environment());
    }

    public CreateMessageHandler(AmazonDynamoDB client, Environment environment) {
        this(environment, defaultMessageService(client), defaultResourceService(client));
    }

    public CreateMessageHandler(Environment environment,
                                MessageService messageService,
                                ResourceService resourceService) {
        super(CreateMessageRequest.class, environment, LOGGER);
        this.messageService = messageService;
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInput(CreateMessageRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        UserInstance sender = createSender(requestInfo);

        Publication publication = resourceService.getPublicationByIdentifier(input.getPublicationIdentifier());
        UserInstance owner = extractOwner(publication);

        SortableIdentifier messageIdentifier =
            messageService.createMessage(sender, owner, publication.getIdentifier(), input.getMessage());

        addAdditionalHeaders(() -> locationHeader(messageIdentifier.toString()));

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateMessageRequest input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private static ResourceService defaultResourceService(AmazonDynamoDB client) {
        return new ResourceService(client, Clock.systemDefaultZone());
    }

    private static MessageService defaultMessageService(AmazonDynamoDB client) {
        return new MessageService(client, Clock.systemDefaultZone());
    }

    private static AmazonDynamoDB defaultClient() {
        return AmazonDynamoDBClientBuilder.defaultClient();
    }

    private UserInstance createSender(RequestInfo requestInfo) {
        String loggedInUser = requestInfo.getFeideId().orElseThrow();
        URI orgUri = requestInfo.getCustomerId().map(URI::create).orElseThrow();
        return new UserInstance(loggedInUser, orgUri);
    }

    private Map<String, String> locationHeader(String messageIdentifier) {
        return Map.of(HttpHeaders.LOCATION, messageIdentifier);
    }

    private UserInstance extractOwner(Publication publication) {
        return new UserInstance(publication.getOwner(), publication.getPublisher().getId());
    }
}
