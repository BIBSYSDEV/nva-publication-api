package no.unit.nva.publication.messages;

import static no.unit.nva.publication.messages.ListMessagesHandler.APPROVE_DOI_REQUEST;
import static no.unit.nva.publication.messages.ListMessagesHandler.CREATOR_ROLE;
import static no.unit.nva.publication.messages.MessageTestsConfig.messageTestsObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.MessageCollection;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListMessagesHandlerTest extends ResourcesLocalTest {
    
    public static final String SAMPLE_USER = "some@user";
    public static final URI SOME_ORG = URI.create("https://example.com/123");
    public static final Context CONTEXT = mock(Context.class);
    public static final String SOME_OTHER_USER = "some@otheruser";
    public static final String ALLOW_EVERYTHING = "*";
    public static final String CURATOR_ROLE = "Curator";
    public static final String ROLE_QUERY_PARAMETER = "role";
    
    public static final String SOME_OTHER_ROLE = "SomeOtherRole";
    public static final URI NOT_IMPORTANT = null;
    private static final int NUMBER_OF_PUBLICATIONS = 3;
    private ListMessagesHandler handler;
    private ByteArrayOutputStream output;
    private InputStream input;
    private ResourceService resourceService;
    private MessageService messageService;
    
    @BeforeEach
    public void init() {
        super.init();
        output = new ByteArrayOutputStream();
        resourceService = new ResourceService(client, Clock.systemDefaultZone());
        messageService = new MessageService(client, Clock.systemDefaultZone());
        var environment = mockEnvironment();
        handler = new ListMessagesHandler(environment, messageService);
    }
    
    @Test
    void listMessagesReturnsOkWhenUserIsAuthenticated() throws IOException {
        input = sampleListRequest();
        handler.handleRequest(input, output, CONTEXT);
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }
    
    @Test
    void listMessagesReturnsMessagesPerPublicationsForUser()
        throws IOException {
        var savedMessages = insertSampleMessagesForSingleOwner();
        var owner = extractPublicationOwner(savedMessages.get(0));
        
        input = defaultUserRequest(owner.getUserIdentifier(), owner.getOrganizationUri());
        handler.handleRequest(input, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, ResourceConversation[].class);
        var responseObjects = response.getBodyObject(ResourceConversation[].class);
        
        assertThatResponseContainsAllExpectedMessages(savedMessages, responseObjects);
        assertThatResourceDescriptionsContainIdentifierAndTitle(savedMessages, responseObjects);
    }
    
    @Test
    void listMessagesReturnsResourceMessagesOrderedByOldestCreationDate()
        throws IOException {
        var userInstance = UserInstance.create(randomString(), randomUri());
        var publications = createSamplePublicationsForSingleOwner(userInstance);
        var messages = createSampleMessagesFromPublications(publications, this::notTheOwner);
        var moreMessages = createSampleMessagesFromPublications(publications, this::notTheOwner);
        var allMessages = new ArrayList<Message>();
        allMessages.addAll(messages);
        allMessages.addAll(moreMessages);
        
        var owner = extractPublicationOwner(allMessages.get(0));
        
        input = defaultUserRequest(owner.getUserIdentifier(), owner.getOrganizationUri());
        handler.handleRequest(input, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, ResourceConversation[].class);
        var responseObjects = response.getBodyObject(ResourceConversation[].class);
        
        assertThatMessagesInsideResponseObjectAreOrderedWithOldestFirst(responseObjects);
        assertThatResponseObjectsAreOrderedByOldestMessage(responseObjects);
    }
    
    @Test
    void listMessagesShowsAllSupportMessagesOfAnOrgGroupedByPublicationWhenUserIsCurator()
        throws IOException {
        var publications = createPublicationsOfDifferentOwnersButSamplePublisher();
        final var messages =
            createSampleMessagesFromPublications(publications, UserInstance::fromPublication);
        
        final var expectedResponse = constructExpectedResponse(messages);
        
        var orgURI = messages.get(0).getCustomerId();
        var curator = someCurator(orgURI);
        
        input = defaultCuratorRequest(curator.getUserIdentifier(), curator.getOrganizationUri());
        handler.handleRequest(input, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, ResourceConversation[].class);
        var body = response.getBodyObject(ResourceConversation[].class);
        
        assertThat(Arrays.asList(body), containsInAnyOrder(expectedResponse));
    }
    
    @Test
    void listMessagesReturnsEmptyListWhenUserIsNeitherCreatorOrCurator()
        throws IOException {
        var publications = createPublicationsOfDifferentOwnersButSamplePublisher();
        final var messages =
            createSampleMessagesFromPublications(publications, UserInstance::fromPublication);
        
        var orgURI = messages.get(0).getCustomerId();
        var curator = someCurator(orgURI);
        
        input = userRequest(curator.getUserIdentifier(), curator.getOrganizationUri(), SOME_OTHER_ROLE);
        handler.handleRequest(input, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, ResourceConversation[].class);
        var body = response.getBodyObject(ResourceConversation[].class);
        
        assertThat(Arrays.asList(body), is(empty()));
    }
    
    private List<Publication> createPublicationsOfDifferentOwnersButSamplePublisher() {
        
        var publisher = PublicationGenerator.randomPublication().getPublisher();
        return samplePublicationsOfDifferentOwners()
            .stream()
            .map(publication -> publication.copy().withPublisher(publisher).build())
            .map(attempt(p -> createPublication(resourceService, p)))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }
    
    private List<Publication> samplePublicationsOfDifferentOwners() {
        return IntStream.range(0, ListMessagesHandlerTest.NUMBER_OF_PUBLICATIONS).boxed()
            .map(ignored -> PublicationGenerator.randomPublication())
            .collect(Collectors.toList());
    }
    
    private ResourceConversation[] constructExpectedResponse(List<Message> messages) {
        var conversations = messages.stream()
            .collect(Collectors.groupingBy(Message::getResourceIdentifier))
            .values()
            .stream()
            .map(ResourceConversation::fromMessageList)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        return conversations.toArray(ResourceConversation[]::new);
    }
    
    private void assertThatResponseObjectsAreOrderedByOldestMessage(ResourceConversation[] responseObjects) {
        
        List<ResourceConversation> sorted = Arrays.stream(responseObjects)
            .sorted(this::objectWithOldestMessageFirst)
            .collect(Collectors.toList());
        List<ResourceConversation> actualResponseObjects = Arrays.asList(responseObjects);
        
        assertThat(actualResponseObjects, is(equalTo(sorted)));
        assertThat(actualResponseObjects, is(not(sameInstance(sorted))));
    }
    
    private int objectWithOldestMessageFirst(ResourceConversation left, ResourceConversation right) {
        var oldestMessageLeft = oldestMessage(left);
        var oldestMessageRight = oldestMessage(right);
        return oldestMessageLeft.getCreatedTime().compareTo(oldestMessageRight.getCreatedTime());
    }
    
    private Message oldestMessage(ResourceConversation left) {
        return left.getMessageCollections()
            .stream()
            .flatMap(messageCollection -> messageCollection.getMessages().stream())
            .sorted(Comparator.comparing(Message::getCreatedTime))
            .collect(Collectors.toList())
            .get(0);
    }
    
    private void assertThatMessagesInsideResponseObjectAreOrderedWithOldestFirst(
        ResourceConversation[] responseObjects) {
        
        for (ResourceConversation responseObject : responseObjects) {
            assertThatMessagesInEachResourceConversationAreOrderedWithOldestFirst(responseObject);
        }
    }
    
    private void assertThatMessagesInEachResourceConversationAreOrderedWithOldestFirst(
        ResourceConversation resourceConversation) {
        for (MessageCollection messageCollection : resourceConversation.getMessageCollections()) {
            assertThatMessagesInMessageCollectionAreOrderedByOldestFirst(messageCollection);
        }
    }
    
    private void assertThatMessagesInMessageCollectionAreOrderedByOldestFirst(MessageCollection messageCollection) {
        var messages = messageCollection.getMessages();
        List<Message> sortedMessages = messages.stream()
            .sorted(Comparator.comparing(Message::getCreatedTime))
            .collect(Collectors.toList());
        
        assertThat(messages, is(not(sameInstance(sortedMessages))));
        assertThat(messages, is(equalTo(sortedMessages)));
    }
    
    private Publication createPublication(ResourceService resourceService, Publication p) throws ApiGatewayException {
        return resourceService.createPublication(UserInstance.fromPublication(p), p);
    }
    
    private Publication createPublication(UserInstance userInstance) throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        publication.setResourceOwner(new ResourceOwner(userInstance.getUserIdentifier(), NOT_IMPORTANT));
        publication.setPublisher(new Organization.Builder().withId(userInstance.getOrganizationUri()).build());
        return createPublication(resourceService, publication);
    }
    
    private void assertThatResourceDescriptionsContainIdentifierAndTitle(List<Message> savedMessages,
                                                                         ResourceConversation[] responseObjects) {
        
        List<PublicationSummary> actualPublicationSummaries = extractPublicationSummaryFromResponse(responseObjects);
        PublicationSummary[] expectedPublicationSummaries = constructExpectedPublicationSummaries(savedMessages);
        assertThat(actualPublicationSummaries, containsInAnyOrder(expectedPublicationSummaries));
    }
    
    private void assertThatResponseContainsAllExpectedMessages(List<Message> savedMessages,
                                                               ResourceConversation[] responseObjects) {
        List<Message> actualMessages = extractAllMessagesFromResponse(responseObjects);
        assertThat(actualMessages, containsInAnyOrder(savedMessages.toArray(Message[]::new)));
    }
    
    private InputStream defaultCuratorRequest(String userIdentifier, URI organizationUri)
        throws JsonProcessingException {
        return userRequest(userIdentifier, organizationUri, CURATOR_ROLE);
    }
    
    private InputStream defaultUserRequest(String userIdentifier, URI organizationUri)
        throws JsonProcessingException {
        return userRequest(userIdentifier, organizationUri, CREATOR_ROLE);
    }
    
    private InputStream userRequest(String userIdentifier, URI organizationUri, String requestedRole)
        throws JsonProcessingException {
        var requestBuilder = new HandlerRequestBuilder<Void>(messageTestsObjectMapper)
            .withNvaUsername(userIdentifier)
            .withCustomerId(organizationUri)
            .withQueryParameters(Map.of(ROLE_QUERY_PARAMETER, requestedRole));
        
        if (CURATOR_ROLE.equals(requestedRole)) {
            requestBuilder.withAccessRights(organizationUri, APPROVE_DOI_REQUEST);
        }
        return requestBuilder.build();
    }
    
    private UserInstance extractPublicationOwner(Message message) {
        return UserInstance.create(message.getOwner(), message.getCustomerId());
    }
    
    private List<PublicationSummary> extractPublicationSummaryFromResponse(ResourceConversation[] responseObjects) {
        return Arrays.stream(responseObjects)
            .map(ResourceConversation::getPublicationSummary)
            .collect(Collectors.toList());
    }
    
    private PublicationSummary[] constructExpectedPublicationSummaries(List<Message> savedMessages) {
        return savedMessages
            .stream()
            .map(PublicationSummary::create)
            .collect(Collectors.toList())
            .toArray(PublicationSummary[]::new);
    }
    
    private List<Message> extractAllMessagesFromResponse(ResourceConversation[] responseObjects) {
        return Arrays.stream(responseObjects)
            .flatMap(responseObject -> responseObject.getMessageCollections().stream())
            .flatMap(messageCollections -> messageCollections.getMessages().stream())
            .collect(Collectors.toList());
    }
    
    private List<Message> insertSampleMessagesForSingleOwner() {
        UserInstance userInstance = UserInstance.create(randomString(), randomUri());
        List<Publication> publications = createSamplePublicationsForSingleOwner(userInstance);
        return createSampleMessagesFromPublications(publications, this::notTheOwner);
    }
    
    private UserInstance notTheOwner(Publication samplePublication) {
        return someCurator(samplePublication.getPublisher().getId());
    }
    
    private UserInstance someCurator(URI orgId) {
        return UserInstance.create(SOME_OTHER_USER, orgId);
    }
    
    private List<Message> createSampleMessagesFromPublications(List<Publication> publications,
                                                               Function<Publication, UserInstance> sender) {
        return publications.stream()
            .map(attempt(pub -> createMessage(pub, sender.apply(pub))))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }
    
    private List<Publication> createSamplePublicationsForSingleOwner(UserInstance userInstance) {
        return IntStream.range(0, NUMBER_OF_PUBLICATIONS).boxed()
            .map(attempt(i -> createPublication(userInstance)))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }
    
    private Environment mockEnvironment() {
        var env = mock(Environment.class);
        when(env.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALLOW_EVERYTHING);
        return env;
    }
    
    private Message createMessage(Publication publication, UserInstance sender)
        throws NotFoundException {
        
        var messageIdentifier = messageService.createMessage(sender, publication, randomString(),
            MessageType.SUPPORT);
        return messageService.getMessage(UserInstance.fromPublication(publication), messageIdentifier);
    }
    
    private InputStream sampleListRequest() throws JsonProcessingException {
        return defaultUserRequest(SAMPLE_USER, SOME_ORG);
    }
}