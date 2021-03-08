package no.unit.nva.pubication.messages.list;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.javafaker.Faker;
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
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceConversation;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ListMessagesHandlerTest extends ResourcesDynamoDbLocalTest {

    public static final String SAMPLE_USER = "some@user";
    public static final URI SOME_ORG = URI.create("https://example.com/123");
    public static final Context CONTEXT = mock(Context.class);
    public static final String SOME_OTHER_USER = "some@otheruser";
    public static final Faker FAKER = Faker.instance();
    public static final String ALLOW_EVERYTHING = "*";
    public static final String CURATOR_ROLE = "Curator";
    public static final String ROLE_QUERY_PARAMETER = "role";
    public static final String CREATOR_ROLE = "Creator";
    public static final boolean NO_IDENTIFIER = false;
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
        Environment environment = mockEnvironment();
        handler = new ListMessagesHandler(environment, messageService);
    }

    @Test
    public void listMessagesReturnsOkWhenUserIsAuthenticated() throws IOException {
        input = sampleListRequest();
        handler.handleRequest(input, output, CONTEXT);
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(output);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    public void listMessagesReturnsMessagesPerPublicationsForUser()
        throws IOException {
        List<Message> savedMessages = insetSampleMessages();
        UserInstance owner = extractPublicationOwner(savedMessages.get(0));

        input = defaultUserRequest(owner.getUserIdentifier(), owner.getOrganizationUri());
        handler.handleRequest(input, output, CONTEXT);

        GatewayResponse<ResourceConversation[]> response = GatewayResponse.fromOutputStream(output);
        ResourceConversation[] responseObjects = response.getBodyObject(ResourceConversation[].class);

        assertThatResponseContainsAllExpectedMessages(savedMessages, responseObjects);

        assertThatResourceDescriptionsContainIdentifierAndTitle(savedMessages, responseObjects);
    }

    @Test
    public void listMessagesReturnsResourceMessagesOrderedByOldestCreationDate()
        throws IOException {
        List<Publication> publications = createSamplePublications();
        List<Message> messages = createSampleMessagesFromPublications(publications, this::notTheOwner);
        List<Message> moreMessages = createSampleMessagesFromPublications(publications, this::notTheOwner);
        List<Message> allMessages = new ArrayList<>();
        allMessages.addAll(messages);
        allMessages.addAll(moreMessages);

        UserInstance owner = extractPublicationOwner(allMessages.get(0));

        input = defaultUserRequest(owner.getUserIdentifier(), owner.getOrganizationUri());
        handler.handleRequest(input, output, CONTEXT);

        GatewayResponse<ResourceConversation[]> response = GatewayResponse.fromOutputStream(output);
        ResourceConversation[] responseObjects = response.getBodyObject(ResourceConversation[].class);

        assertThatMessagesInsideResponseObjectAreOrderedWithOldestFirst(responseObjects);
        assertThatResponseObjectsAreOrderedByOldestMessage(responseObjects);
    }

    @Test
    public void listMessagesShowsAllSupportMessagesOfAnOrgGroupedByPublicationWhenUserIsCurator()
        throws IOException {
        List<Publication> publications = createPublicationsOfDifferentOwners();
        final List<Message> messages = createSampleMessagesFromPublications(publications, this::theOwner);

        final ResourceConversation[] expectedResponse = constructExpectedResponse(messages);

        URI orgURI = messages.get(0).getCustomerId();
        UserInstance curator = someCurator(orgURI);

        input = defaultCuratorRequest(curator.getUserIdentifier(), curator.getOrganizationUri());
        handler.handleRequest(input, output, CONTEXT);

        GatewayResponse<ResourceConversation[]> response = GatewayResponse.fromOutputStream(output);
        ResourceConversation[] body = response.getBodyObject(ResourceConversation[].class);

        assertThat(Arrays.asList(body), containsInAnyOrder(expectedResponse));
    }

    private List<Publication> createPublicationsOfDifferentOwners() {

        return PublicationGenerator.samplePublicationsOfDifferentOwners(NUMBER_OF_PUBLICATIONS, NO_IDENTIFIER)
            .stream()
            .map(attempt(p -> resourceService.createPublication(p)))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private ResourceConversation[] constructExpectedResponse(List<Message> messages) {
        List<ResourceConversation> conversations = messages.stream()
                                                       .collect(Collectors.groupingBy(Message::getResourceIdentifier))
                                                       .values()
                                                       .stream()
                                                       .map(ResourceConversation::fromMessageList)
                                                       .flatMap(List::stream)
                                                       .collect(Collectors.toList());

        return conversations.toArray(ResourceConversation[]::new);
    }

    private UserInstance theOwner(Publication publication) {
        String owner = publication.getOwner();
        URI customerId = publication.getPublisher().getId();
        return new UserInstance(owner, customerId);
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
        MessageDto oldestMessageLeft = oldestMessage(left);
        MessageDto oldestMessageRight = oldestMessage(right);
        return oldestMessageLeft.getDate().compareTo(oldestMessageRight.getDate());
    }

    private MessageDto oldestMessage(ResourceConversation left) {
        return left.getMessages()
                   .stream()
                   .sorted(Comparator.comparing(MessageDto::getDate))
                   .collect(Collectors.toList())
                   .get(0);
    }

    private void assertThatMessagesInsideResponseObjectAreOrderedWithOldestFirst(
        ResourceConversation[] responseObjects) {

        for (ResourceConversation resourceMessages : responseObjects) {
            var messages = resourceMessages.getMessages();
            List<MessageDto> sortedMessages = messages.stream()
                                                  .sorted(Comparator.comparing(MessageDto::getDate))
                                                  .collect(Collectors.toList());
            assertThat(messages, is(not(sameInstance(sortedMessages))));
            assertThat(messages, is(equalTo(sortedMessages)));
        }
    }

    private Publication createPublication() throws TransactionFailedException {
        return resourceService.createPublication(PublicationGenerator.publicationWithoutIdentifier());
    }

    private void assertThatResourceDescriptionsContainIdentifierAndTitle(List<Message> savedMessages,
                                                                         ResourceConversation[] responseObjects) {

        List<Publication> actualPublicationDescriptions = extractPublicationDescriptionFromResponse(responseObjects);
        Publication[] expectedPublicationDescriptions = constructExpectedPublicationDescriptions(savedMessages);
        assertThat(actualPublicationDescriptions, containsInAnyOrder(expectedPublicationDescriptions));
    }

    private void assertThatResponseContainsAllExpectedMessages(List<Message> savedMessages,
                                                               ResourceConversation[] responseObjects) {
        List<MessageDto> actualMessages = extractAllMessagesFromResponse(responseObjects);
        MessageDto[] expectedMessages = constructExpectedMessages(savedMessages);
        assertThat(actualMessages, containsInAnyOrder(expectedMessages));
    }

    private InputStream defaultCuratorRequest(String userIdentifier, URI organizationUri)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.objectMapper)
                   .withFeideId(userIdentifier)
                   .withCustomerId(organizationUri.toString())
                   .withRoles(CURATOR_ROLE)
                   .withQueryParameters(Map.of("role", CURATOR_ROLE))
                   .build();
    }

    private InputStream defaultUserRequest(String userIdentifier, URI organizationUri)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.objectMapper)
                   .withFeideId(userIdentifier)
                   .withCustomerId(organizationUri.toString())
                   .withQueryParameters(Map.of(ROLE_QUERY_PARAMETER, CREATOR_ROLE))
                   .build();
    }

    private UserInstance extractPublicationOwner(Message message) {
        return new UserInstance(message.getOwner(), message.getCustomerId());
    }

    private List<Publication> extractPublicationDescriptionFromResponse(ResourceConversation[] responseObjects) {
        return Arrays.stream(responseObjects)
                   .map(ResourceConversation::getPublication)
                   .collect(Collectors.toList());
    }

    private Publication[] constructExpectedPublicationDescriptions(List<Message> savedMessages) {
        List<Publication> expectedPublicationDescriptions = savedMessages
                                                                .stream()
                                                                .map(this::createPublicationDescription)
                                                                .collect(Collectors.toList());
        Publication[] expectedPublicationDescriptionsArray = new Publication[NUMBER_OF_PUBLICATIONS];
        expectedPublicationDescriptions.toArray(expectedPublicationDescriptionsArray);
        return expectedPublicationDescriptionsArray;
    }

    private List<MessageDto> extractAllMessagesFromResponse(ResourceConversation[] responseObjects) {
        return Arrays.stream(responseObjects).flatMap(r -> r.getMessages().stream()).collect(
            Collectors.toList());
    }

    private MessageDto[] constructExpectedMessages(List<Message> savedMessages) {
        List<MessageDto> expectedMessages = savedMessages.stream().map(MessageDto::fromMessage)
                                                .collect(Collectors.toList());
        MessageDto[] expectedMessagesArray = new MessageDto[savedMessages.size()];
        expectedMessages.toArray(expectedMessagesArray);
        return expectedMessagesArray;
    }

    private Publication createPublicationDescription(Message message) {
        return ResourceConversation.createPublicationDescription(message);
    }

    private List<Message> insetSampleMessages() {
        List<Publication> publications = createSamplePublications();
        return createSampleMessagesFromPublications(publications, this::notTheOwner);
    }

    private UserInstance notTheOwner(Publication samplePublication) {
        return someCurator(samplePublication.getPublisher().getId());
    }

    private UserInstance someCurator(URI orgId) {
        return new UserInstance(SOME_OTHER_USER, orgId);
    }

    private List<Message> createSampleMessagesFromPublications(List<Publication> publications,
                                                               Function<Publication, UserInstance> sender) {
        return publications.stream()
                   .map(attempt(pub -> createMessage(pub, sender.apply(pub))))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    private List<Publication> createSamplePublications() {
        return IntStream.range(0, NUMBER_OF_PUBLICATIONS).boxed()
                   .map(attempt(i -> createPublication()))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    private Environment mockEnvironment() {
        var env = mock(Environment.class);
        when(env.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALLOW_EVERYTHING);
        return env;
    }

    private Message createMessage(Publication publication, UserInstance sender)
        throws TransactionFailedException, NotFoundException {
        SortableIdentifier messageIdentifier = messageService.createSimpleMessage(sender, publication, randomString());
        return messageService.getMessage(extractOwner(publication), messageIdentifier);
    }

    private String randomString() {
        return FAKER.lorem().sentence();
    }

    private InputStream sampleListRequest() throws JsonProcessingException {
        return defaultUserRequest(SAMPLE_USER, SOME_ORG);
    }
}