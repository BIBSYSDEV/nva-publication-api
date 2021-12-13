package no.unit.nva.doirequest.list;

import static no.unit.nva.doirequest.DoiRequestsTestConfig.doiRequestsObjectMapper;
import static no.unit.nva.doirequest.list.ListDoiRequestsHandler.CREATOR_ROLE;
import static no.unit.nva.doirequest.list.ListDoiRequestsHandler.CURATOR_ROLE;
import static no.unit.nva.doirequest.list.ListDoiRequestsHandler.ROLE_QUERY_PARAMETER;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.collection.IsEmptyCollection.emptyCollectionOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.DoiRequestMessage;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.publication.testing.http.RandomPersonServiceResponse;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ListDoiRequestsHandlerTest extends ResourcesLocalTest {

    public static final String SOME_CURATOR = "SomeCurator";
    public static final String SOME_OTHER_OWNER = "someOther@owner.no";
    public static final String SOME_INVALID_ROLE = "SomeInvalidRole";
    public static final int FIRST_ELEMENT = 0;
    public static final String ALLOW_ALL_ORIGIN = "*";
    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");
    private ListDoiRequestsHandler handler;
    private ResourceService resourceService;
    private Clock mockClock;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private DoiRequestService doiRequestService;
    private MessageService messageService;

    @BeforeEach
    public void initialize() {
        init();
        setupClock();
        var httpClient = new FakeHttpClient<>(new RandomPersonServiceResponse().toString());
        resourceService = new ResourceService(client, httpClient, mockClock);

        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
        Environment environment = mockEnvironment();

        doiRequestService = new DoiRequestService(client, httpClient, mockClock);
        messageService = new MessageService(client, mockClock);
        handler = new ListDoiRequestsHandler(environment, doiRequestService, messageService);
    }

    @Test
    public void handlerReturnsListOfDoiRequestsWhenUserIsCuratorAndThereAreDoiRequestsForSamePublisher()
        throws ApiGatewayException, IOException {
        List<Publication> publications = createPublishedPublicationsOfSamePublisherButDifferentOwner();
        List<DoiRequest> expectedDoiRequests = createDoiRequests(publications);

        URI curatorsPublisher = publications.get(0).getPublisher().getId();
        InputStream request = createRequest(curatorsPublisher, SOME_CURATOR, CURATOR_ROLE);

        handler.handleRequest(request, outputStream, context);

        List<Publication> actualResponse = parseResponse();

        List<Publication> expectedResponse = toPublications(expectedDoiRequests);
        assertThat(actualResponse, is(equalTo(expectedResponse)));
    }

    public List<Publication> parseResponse() throws com.fasterxml.jackson.core.JsonProcessingException {
        GatewayResponse<Publication[]> response = GatewayResponse.fromOutputStream(outputStream);
        return Arrays.asList(response.getBodyObject(Publication[].class));
    }

    @Test
    public void handlerReturnsListOfDoiRequestsOnlyForTheCuratorsOrganization()
        throws ApiGatewayException, IOException {
        List<Publication> publications = publishedPublicationsOfDifferentPublisher();
        List<DoiRequest> createdDoiRequests = createDoiRequests(publications);

        URI curatorsCustomer = publications.get(FIRST_ELEMENT).getPublisher().getId();

        InputStream request = createRequest(curatorsCustomer, SOME_CURATOR, CURATOR_ROLE);

        handler.handleRequest(request, outputStream, context);

        List<Publication> actualResponse = parseResponse();

        Publication expectedResponse =
            filterDoiRequests(createdDoiRequests,
                              doiRequest -> doiRequestBelongsToCustomer(curatorsCustomer, doiRequest)
            );

        Publication unexpectedDoiResponse = filterDoiRequests(createdDoiRequests,
                                                              doiRequest -> !doiRequestBelongsToCustomer(
                                                                  curatorsCustomer, doiRequest));

        assertThat(actualResponse, hasItem(expectedResponse));
        assertThat(actualResponse, not(hasItem(unexpectedDoiResponse)));
    }

    @Test
    public void listDoiRequestsReturnsEmptyListForUserWhenUserHasNoDoiRequests()
        throws ApiGatewayException, IOException {
        List<Publication> publications = createPublishedPublicationsOfSameOwner();
        createDoiRequests(publications);

        URI usersPublisher = publications.get(0).getPublisher().getId();

        InputStream request = createRequest(usersPublisher, SOME_OTHER_OWNER, CREATOR_ROLE);

        handler.handleRequest(request, outputStream, context);

        List<Publication> responseBody = parseResponse();

        assertThat(responseBody, is(emptyCollectionOf(Publication.class)));
    }

    @Test
    public void listDoiRequestsReturnsOnlyDoiRequestsOwnedByTheUserWhenRequestIsFromNotACurator()
        throws ApiGatewayException, IOException {
        List<Publication> publications = createPublishedPublicationsOfSamePublisherButDifferentOwner();
        List<DoiRequest> doiRequests = createDoiRequests(publications);

        UserInstance userInstance = createUserInstance(publications.get(0));

        InputStream request = createRequest(
            userInstance.getOrganizationUri(),
            userInstance.getUserIdentifier(),
            CREATOR_ROLE
        );

        handler.handleRequest(request, outputStream, context);

        Publication expectedDoiRequest = filterDoiRequests(doiRequests,
                                                           doi -> doi.getOwner()
                                                               .equals(userInstance.getUserIdentifier()));

        List<Publication> responseBody = parseResponse();

        assertThat(responseBody, is(equalTo(List.of(expectedDoiRequest))));
    }

    @Test
    public void listDoiRequestsReturnsEmptyListForUserWhenUserHasNoValidRole()
        throws ApiGatewayException, IOException {
        List<Publication> publications = createPublishedPublicationsOfSameOwner();
        createDoiRequests(publications);

        URI usersPublisher = publications.get(0).getPublisher().getId();

        InputStream request = createRequest(usersPublisher, SOME_OTHER_OWNER, SOME_INVALID_ROLE);

        handler.handleRequest(request, outputStream, context);

        List<Publication> responseBody = parseResponse();

        assertThat(responseBody, is(emptyCollectionOf(Publication.class)));
    }

    @Test
    public void listDoiRequestsForUserReturnsDtosWithMessagesIncludedWhenThereAreDoiRequestMessagesForTheDoiRequest()
        throws ApiGatewayException, IOException {
        List<Publication> publications = createPublishedPublicationsOfSameOwner();
        createDoiRequests(publications);
        final var doiRequestMessages = createDoiRequestMessagesForPublications(publications);

        URI commonPublisherId = publications.get(FIRST_ELEMENT).getPublisher().getId();
        String commonOwner = publications.get(FIRST_ELEMENT).getOwner();
        List<String> actualMessages = sendRequestAndReadMessages(commonOwner, commonPublisherId, CREATOR_ROLE);

        String[] expectedMessages = extractMessageTexts(doiRequestMessages);
        assertThat(actualMessages, containsInAnyOrder(expectedMessages));
    }

    @Test
    public void listDoiRequestsForCuratorReturnsDtosWithMessagesIncludedWhenThereAreDoiRequestMessagesForTheDoiRequest()
        throws ApiGatewayException, IOException {
        var publications = createPublishedPublicationsOfSamePublisherButDifferentOwner();
        createDoiRequests(publications);
        final var doiRequestMessages = createDoiRequestMessagesForPublications(publications);

        URI commonPublisherId = publications.get(0).getPublisher().getId();
        List<String> actualMessages = sendRequestAndReadMessages(SOME_CURATOR, commonPublisherId, CURATOR_ROLE);

        String[] expectedMessages = extractMessageTexts(doiRequestMessages);
        assertThat(actualMessages, containsInAnyOrder(expectedMessages));
    }

    @Test
    public void listDoiRequestsForUserReturnsDtosContainingOnlyDoiRequestsMessagesAndNotOtherTypeOfMessages()
        throws ApiGatewayException, IOException {
        final var publications = createPublishedPublicationsOfSameOwner();
        final var publicationsOwner = extractUserInstance(publications.get(0));
        final var publicationsOwnerIdentifier = publicationsOwner.getUserIdentifier();
        final var commonPublisherId = publicationsOwner.getOrganizationUri();

        createDoiRequests(publications);
        final var doiRequestMessages = createDoiRequestMessagesForPublications(publications);
        final var otherMessages = createSupportMessagesForPublications(publications);

        var actualMessages = sendRequestAndReadMessages(publicationsOwnerIdentifier, commonPublisherId,
                                                        CREATOR_ROLE);

        var expectedMessages = extractMessageTexts(doiRequestMessages);
        var notExpectedMessages = extractMessageTexts(otherMessages);

        assertThat(actualMessages, hasItems(expectedMessages));
        assertThatActualMessagesDoNotContainAnyOf(actualMessages, notExpectedMessages);
    }

    private void assertThatActualMessagesDoNotContainAnyOf(List<String> actualMessages, String[] notExpectedMessages) {
        for (String notExpectedMessage : notExpectedMessages) {
            assertThat(actualMessages, not(hasItem(notExpectedMessage)));
        }
    }

    private List<MessageDto> createSupportMessagesForPublications(List<Publication> publications) {
        return publications.stream()
            .map(attempt(this::createSupportMessage))
            .map(attempt -> attempt.map(MessageDto::fromMessage))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private Message createSupportMessage(Publication pub) throws TransactionFailedException, NotFoundException {
        UserInstance owner = extractUserInstance(pub);
        var messageIdentifier = messageService.createSimpleMessage(owner, pub, randomString());
        return messageService.getMessage(owner, messageIdentifier);
    }

    private List<String> sendRequestAndReadMessages(String userIdentifier, URI commonPublisherId, String curatorRole)
        throws IOException {
        InputStream input = createRequest(commonPublisherId, userIdentifier, curatorRole);
        handler.handleRequest(input, outputStream, context);
        GatewayResponse<Publication[]> response = GatewayResponse.fromOutputStream(outputStream);

        Publication[] doiRequestDtos = response.getBodyObject(Publication[].class);
        return extractMessageTexts(doiRequestDtos);
    }

    private String[] extractMessageTexts(List<MessageDto> doiRequestMessages) {
        var texts = doiRequestMessages.stream()
            .map(MessageDto::getText)
            .collect(Collectors.toList());

        return texts.toArray(String[]::new);
    }

    private List<String> extractMessageTexts(Publication[] doiRequestDtos) {
        return Arrays.stream(doiRequestDtos)
            .map(Publication::getDoiRequest)
            .flatMap(d -> d.getMessages().stream())
            .map(DoiRequestMessage::getText)
            .collect(Collectors.toList());
    }

    private Environment mockEnvironment() {
        Environment environment = mock(Environment.class);
        Mockito.when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALLOW_ALL_ORIGIN);
        return environment;
    }

    private List<MessageDto> createDoiRequestMessagesForPublications(List<Publication> publications) {
        return publications.stream()
            .map(attempt(this::createDoiRequestMessage))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private MessageDto createDoiRequestMessage(Publication pub) throws TransactionFailedException, NotFoundException {
        UserInstance owner = extractUserInstance(pub);
        var messageID = messageService.createDoiRequestMessage(owner, pub, randomString());
        Message message = messageService.getMessage(owner, messageID);
        return MessageDto.fromMessage(message);
    }

    private boolean doiRequestBelongsToCustomer(URI curatorsCustomer, DoiRequest doiRequest) {
        return doiRequest.getCustomerId().equals(curatorsCustomer);
    }

    private InputStream createRequest(URI customerId, String userIdentifier, String userRole)
        throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<Void>(doiRequestsObjectMapper)
            .withCustomerId(customerId.toString())
            .withFeideId(userIdentifier)
            .withRoles(userRole)
            .withQueryParameters(
                Map.of(ROLE_QUERY_PARAMETER, userRole))
            .build();
    }

    private List<Publication> toPublications(List<DoiRequest> expectedDoiRequests) {
        return expectedDoiRequests.stream()
            .map(DoiRequest::toPublication)
            .collect(Collectors.toList());
    }

    private Publication filterDoiRequests(List<DoiRequest> createdDoiRequests,
                                          Function<DoiRequest, Boolean> filter) {
        return createdDoiRequests
            .stream()
            .filter(filter::apply)
            .map(DoiRequest::toPublication)
            .collect(SingletonCollector.collect());
    }

    private List<DoiRequest> createDoiRequests(List<Publication> publications) {
        return publications.stream()
            .map(attempt(this::creteDoiRequest))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private List<Publication> createPublishedPublicationsOfSameOwner() throws ApiGatewayException {
        var owner = randomString();
        var publisher = randomUri();
        var userInstance = new UserInstance(owner, publisher);
        Stream<Publication> publicationsToBeSaved = Stream.of(randomPublication(),
                                                              randomPublication());
        List<Publication> publications = createPublicationsForOwner(userInstance, publicationsToBeSaved);

        for (Publication pub : publications) {
            publishPublication(pub);
        }
        return publications;
    }

    private List<Publication> createPublishedPublicationsOfSamePublisherButDifferentOwner() throws ApiGatewayException {
        Publication publication = PublicationGenerator.randomPublication();
        Publication publicationWithDifferentOwner =
            publication.copy()
                .withResourceOwner(new ResourceOwner(randomString(), randomUri()))
                .build();

        return saveAndPublishPublications(publication, publicationWithDifferentOwner);
    }

    private List<Publication> saveAndPublishPublications(Publication... publications) {
        return Arrays.stream(publications)
            .map(attempt(this::createPublishedPublication))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private Publication createPublishedPublication(Publication pub) throws ApiGatewayException {
        UserInstance userInstance = extractUserInstance(pub);
        var createdPublication = resourceService.createPublication(userInstance, pub);
        resourceService.publishPublication(extractUserInstance(createdPublication), createdPublication.getIdentifier());
        return resourceService.getPublication(userInstance, createdPublication.getIdentifier());
    }

    private List<Publication> publishedPublicationsOfDifferentPublisher() throws ApiGatewayException {
        Publication publication = PublicationGenerator.randomPublication();
        Publication publicationWithDifferentPublisher = PublicationGenerator.randomPublication();
        return saveAndPublishPublications(publication, publicationWithDifferentPublisher);
    }

    private List<Publication> createPublicationsForOwner(UserInstance userInstance,
                                                         Stream<Publication> sameOwnerPublications) {
        return sameOwnerPublications
            .map(attempt(pub -> resourceService.createPublication(userInstance, pub)))

            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private void publishPublication(Publication pub) throws ApiGatewayException {
        UserInstance userInstance = new UserInstance(pub.getOwner(), pub.getPublisher().getId());
        resourceService.publishPublication(userInstance, pub.getIdentifier());
    }

    private DoiRequest creteDoiRequest(Publication pub)
        throws BadRequestException, TransactionFailedException, NotFoundException {
        UserInstance userInstance = createUserInstance(pub);
        doiRequestService.createDoiRequest(userInstance, pub.getIdentifier());
        return doiRequestService.getDoiRequestByResourceIdentifier(userInstance, pub.getIdentifier());
    }

    private UserInstance createUserInstance(Publication pub) {
        return new UserInstance(pub.getOwner(), pub.getPublisher().getId());
    }

    private void setupClock() {
        mockClock = mock(Clock.class);
        Mockito.when(mockClock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(PUBLICATION_UPDATE_TIME)
            .thenReturn(DOI_REQUEST_CREATION_TIME)
            .thenReturn(DOI_REQUEST_UPDATE_TIME);
    }
}