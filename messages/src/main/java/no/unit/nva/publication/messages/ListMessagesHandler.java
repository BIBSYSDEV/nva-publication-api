package no.unit.nva.publication.messages;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ListMessagesHandler extends ApiGatewayHandler<Void, ResourceConversation[]> {

    public static final String REQUESTED_ROLE = "role";
    public static final String CURATOR_ROLE = "Curator";
    public static final String CREATOR_ROLE = "Creator";
    public static final String APPROVE_DOI_REQUEST = "APPROVE_DOI_REQUEST";
    private final MessageService messageService;

    @JacocoGenerated
    public ListMessagesHandler() {
        this(new Environment(), defaultMessageService());
    }

    public ListMessagesHandler(Environment environment, MessageService messageService) {
        super(Void.class, environment);
        this.messageService = messageService;
    }

    @Override
    protected ResourceConversation[] processInput(Void input, RequestInfo requestInfo, Context context)
        throws BadRequestException {
        UserInstance userInstance = extractUserInstanceFromRequest(requestInfo);
        List<ResourceConversation> conversations = fetchResourceConversations(requestInfo, userInstance);
        return convertListToArray(conversations);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, ResourceConversation[] output) {
        return HttpURLConnection.HTTP_OK;
    }

    @JacocoGenerated
    private static MessageService defaultMessageService() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        return new MessageService(client, Clock.systemDefaultZone());
    }

    private List<ResourceConversation> fetchResourceConversations(RequestInfo requestInfo, UserInstance userInstance)
        throws BadRequestException {
        if (userIsCurator(requestInfo)) {
            return
                messageService.listMessagesForCurator(userInstance.getOrganizationUri(), MessageStatus.UNREAD);
        } else if (userIsCreator(requestInfo)) {
            return messageService.listMessagesForUser(userInstance);
        } else {
            return Collections.emptyList();
        }
    }

    private boolean userIsCreator(RequestInfo requestInfo) {
        return requestInfo.getCustomerId().isPresent();
    }

    private boolean userIsCurator(RequestInfo requestInfo) throws BadRequestException {
        return CURATOR_ROLE.equals(requestInfo.getQueryParameter(REQUESTED_ROLE))
            && requestInfo.userIsAuthorized(APPROVE_DOI_REQUEST);
    }

    private UserInstance extractUserInstanceFromRequest(RequestInfo requestInfo) {
        String feideId = requestInfo.getNvaUsername();
        URI customerId = requestInfo.getCustomerId().map(URI::create).orElse(null);
        return UserInstance.create(feideId, customerId);
    }

    private ResourceConversation[] convertListToArray(List<ResourceConversation> result) {
        return result.toArray(ResourceConversation[]::new);
    }
}
