package no.unit.nva.doirequest.list;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.DoiRequestMessage;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceConversation;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListDoiRequestsHandler extends ApiGatewayHandler<Void, Publication[]> {

    public static final String ROLE_QUERY_PARAMETER = "role";
    public static final String CURATOR_ROLE = "Curator";
    public static final String CREATOR_ROLE = "Creator";
    public static final String EMPTY_STRING = "";
    private static final Logger LOGGER = LoggerFactory.getLogger(ListDoiRequestsHandler.class);
    private final DoiRequestService doiRequestService;
    private final MessageService messageService;

    @JacocoGenerated
    public ListDoiRequestsHandler() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone());
    }

    @JacocoGenerated
    private ListDoiRequestsHandler(AmazonDynamoDB client, Clock clock) {
        this(new Environment(), defaultDoiRequestService(client, clock), defaultMessageService(client, clock));
    }

    public ListDoiRequestsHandler(Environment environment,
                                  DoiRequestService doiRequestService,
                                  MessageService messageService) {
        super(Void.class, environment, LOGGER);
        this.doiRequestService = doiRequestService;
        this.messageService = messageService;
    }

    @Override
    protected Publication[] processInput(Void input, RequestInfo requestInfo, Context context) {
        URI customerId = requestInfo.getCustomerId().map(URI::create).orElse(null);
        String role = requestInfo.getQueryParameter(ROLE_QUERY_PARAMETER);
        String userId = requestInfo.getFeideId().orElse(null);
        String userRoles = requestInfo.getAssignedRoles().orElse(EMPTY_STRING);
        UserInstance userInstance = new UserInstance(userId, customerId);
        if (userIsACurator(role, userRoles)) {
            return fetchDoiRequestsForCurator(userInstance);
        } else if (userIsACreator(role, userRoles)) {
            return fetchDoiRequestsForUser(userInstance);
        }

        return emptyResult();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Publication[] output) {
        return HttpURLConnection.HTTP_OK;
    }

    @JacocoGenerated
    private static MessageService defaultMessageService(AmazonDynamoDB client, Clock clock) {
        return new MessageService(client, clock);
    }

    @JacocoGenerated
    private static DoiRequestService defaultDoiRequestService(AmazonDynamoDB client, Clock clock) {
        return new DoiRequestService(client, clock);
    }

    private Publication[] emptyResult() {
        return new Publication[0];
    }

    private boolean userIsACreator(String requestedRole, String userRolesCsv) {
        return userHasRequestedRole(requestedRole, userRolesCsv, CREATOR_ROLE);
    }

    private boolean userIsACurator(String role, String userRolesCsv) {
        return userHasRequestedRole(role, userRolesCsv, CURATOR_ROLE);
    }

    private boolean userHasRequestedRole(String requestedRole, String userRolesCsv, String systemDefinedRole) {
        boolean userHasRequestedRole = userHasRequestedRole(requestedRole, userRolesCsv);
        return requestedRole.equals(systemDefinedRole) && userHasRequestedRole;
    }

    private boolean userHasRequestedRole(String role, String userRolesCsv) {
        String userRolesUpperCased = userRolesCsv.toUpperCase(Locale.getDefault());
        String requestedRole = role.toUpperCase(Locale.getDefault());
        return userRolesUpperCased.contains(requestedRole);
    }

    private Publication[] fetchDoiRequestsForUser(UserInstance userInstance) {
        List<DoiRequest> doiRequests = doiRequestService.listDoiRequestsForUser(userInstance);
        return addDoiRequestMessagesToDoiRequests(doiRequests);
    }

    private Publication[] fetchDoiRequestsForCurator(UserInstance userInstance) {
        List<DoiRequest> doiRequests = doiRequestService.listDoiRequestsForPublishedPublications(userInstance);
        return addDoiRequestMessagesToDoiRequests(doiRequests);
    }

    private Publication[] addDoiRequestMessagesToDoiRequests(List<DoiRequest> doiRequests) {
        List<Publication> publicationDtos = convertDoiRequestsToPublicationDtos(doiRequests);
        List<Publication> enrichedWithMessages = enrichPublicationDtosWithDoiRequestMessages(publicationDtos);
        return publicationListToPublicationArray(enrichedWithMessages);
    }

    private List<Publication> enrichPublicationDtosWithDoiRequestMessages(List<Publication> dtos) {
        return dtos.stream()
                   .map(this::addMessagesToPublicationDto)
                   .sorted(sortByOldestDoiRequestFirst())
                   .collect(Collectors.toList());
    }

    private Comparator<Publication> sortByOldestDoiRequestFirst() {
        return Comparator.comparing(p -> p.getDoiRequest().getCreatedDate());
    }

    private Publication addMessagesToPublicationDto(Publication dto) {
        Stream<ResourceConversation> messages =
            messageService.getMessagesForResource(extractOwner(dto), dto.getIdentifier()).stream();
        List<DoiRequestMessage> doiRequestMessages = transformToLegacyDoiRequestMessagesDto(messages);
        dto.getDoiRequest().setMessages(doiRequestMessages);
        return dto;
    }

    private List<DoiRequestMessage> transformToLegacyDoiRequestMessagesDto(Stream<ResourceConversation> messages) {
        return messages
                   .flatMap(resourceMessages -> resourceMessages.getMessages().stream())
                   .filter(MessageDto::isDoiRequestRelated)
                   .map(this::toDoiRequestMessage)
                   .sorted(sortByTimeOldestFirst())
                   .collect(Collectors.toList());
    }

    private Comparator<DoiRequestMessage> sortByTimeOldestFirst() {
        return Comparator.comparing(DoiRequestMessage::getTimestamp);
    }

    private DoiRequestMessage toDoiRequestMessage(MessageDto message) {
        return new DoiRequestMessage.Builder()
                   .withText(message.getText())
                   .withTimestamp(message.getDate())
                   .withAuthor(message.getSenderIdentifier())
                   .build();
    }

    private Publication[] publicationListToPublicationArray(List<Publication> dtos) {
        return dtos.toArray(Publication[]::new);
    }

    private List<Publication> convertDoiRequestsToPublicationDtos(List<DoiRequest> doiRequests) {
        return doiRequests.stream()
                   .map(DoiRequest::toPublication)
                   .collect(Collectors.toList());
    }
}
