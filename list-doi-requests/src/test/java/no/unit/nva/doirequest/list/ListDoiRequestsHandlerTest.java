package no.unit.nva.doirequest.list;

import static no.unit.nva.doirequest.list.ListDoiRequestsHandler.CREATOR_ROLE;
import static no.unit.nva.doirequest.list.ListDoiRequestsHandler.CURATOR_ROLE;
import static no.unit.nva.doirequest.list.ListDoiRequestsHandler.ROLE_QUERY_PARAMETER;
import static no.unit.nva.publication.PublicationGenerator.publicationWithoutIdentifier;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.emptyCollectionOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ListDoiRequestsHandlerTest extends ResourcesDynamoDbLocalTest {

    public static final String SOME_CURATOR = "SomeCurator";
    public static final String SOME_OTHER_OWNER = "someOther@owner.no";
    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");
    private static final URI SOME_OTHER_PUBLISHER = URI.create("https://some-other-publisher.com");
    public static final String SOME_INVALID_ROLE = "SomeInvalidRole";
    private ListDoiRequestsHandler handler;
    private ResourceService resourceService;
    private Clock mockClock;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private DoiRequestService doiRequestService;

    @BeforeEach
    public void initialize() {
        init();
        setupClock();
        resourceService = new ResourceService(client, mockClock);
        doiRequestService = new DoiRequestService(client, mockClock);
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
        Environment environment = mock(Environment.class);
        when(environment.readEnv(anyString())).thenReturn("*");
        handler = new ListDoiRequestsHandler(environment, doiRequestService);
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

        URI curatorsCustomer = publications.get(0).getPublisher().getId();

        InputStream request = createRequest(curatorsCustomer, SOME_CURATOR, CURATOR_ROLE);

        handler.handleRequest(request, outputStream, context);

        List<Publication> actualResponse = parseResponse();

        Publication expectedResponse = filterDoiRequests(createdDoiRequests,
            doiRequest -> doiRequestBelongsToCustomer(curatorsCustomer, doiRequest));

        Publication unexpectedDoiResponse = filterDoiRequests(createdDoiRequests,
            doiRequest -> !doiRequestBelongsToCustomer(curatorsCustomer, doiRequest));

        assertThat(actualResponse, hasItem(expectedResponse));
        assertThat(actualResponse, not(hasItem(unexpectedDoiResponse)));
    }

    @Test
    public void listDoiRequestsReturnsEmptyListForUserWhenUserHasNoDoiRequests()
        throws ApiGatewayException, IOException {
        List<Publication> publications = createPublishedPublicationOfSameOwner();
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
            doi -> doi.getOwner().equals(userInstance.getUserIdentifier()));

        List<Publication> responseBody = parseResponse();

        assertThat(responseBody, is(equalTo(List.of(expectedDoiRequest))));
    }

    @Test
    public void listDoiRequestsReturnsEmptyListForUserWhenUserHasNoValidRole()
        throws ApiGatewayException, IOException {
        List<Publication> publications = createPublishedPublicationOfSameOwner();
        createDoiRequests(publications);

        URI usersPublisher = publications.get(0).getPublisher().getId();

        InputStream request = createRequest(usersPublisher, SOME_OTHER_OWNER, SOME_INVALID_ROLE);

        handler.handleRequest(request, outputStream, context);

        List<Publication> responseBody = parseResponse();

        assertThat(responseBody, is(emptyCollectionOf(Publication.class)));
    }

    private boolean doiRequestBelongsToCustomer(URI curatorsCustomer, DoiRequest doiRequest) {
        return doiRequest.getCustomerId().equals(curatorsCustomer);
    }

    private InputStream createRequest(URI id, String someCurator, String curatorRole)
        throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.objectMapper)
            .withCustomerId(id.toString())
            .withFeideId(someCurator)
            .withRoles(curatorRole)
            .withQueryParameters(
                Map.of(ROLE_QUERY_PARAMETER, curatorRole))
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

    private List<Publication> createPublishedPublicationOfSameOwner() throws ApiGatewayException {

        Stream<Publication> publicationsToBeSaved = Stream
            .of(publicationWithoutIdentifier(), publicationWithoutIdentifier());
        List<Publication> publications = createPublications(publicationsToBeSaved);

        for (Publication pub : publications) {
            publishPublication(pub);
        }
        return publications;
    }

    private List<Publication> createPublishedPublicationsOfSamePublisherButDifferentOwner() throws ApiGatewayException {
        Publication publication = publicationWithoutIdentifier();
        Publication publicationWithDifferentOwner = publication.copy().withOwner(SOME_OTHER_OWNER).build();
        List<Publication> publications = createPublications(Stream.of(publication, publicationWithDifferentOwner));

        for (Publication pub : publications) {
            publishPublication(pub);
        }
        return publications;
    }

    private List<Publication> publishedPublicationsOfDifferentPublisher() throws ApiGatewayException {
        Publication publication = publicationWithoutIdentifier();
        Publication publicationWithDifferentPublisher = publication
            .copy()
            .withOwner(SOME_OTHER_OWNER)
            .withPublisher(new Organization.Builder().withId(SOME_OTHER_PUBLISHER).build())
            .build();
        List<Publication> publications = createPublications(Stream.of(publication, publicationWithDifferentPublisher));

        for (Publication pub : publications) {
            publishPublication(pub);
        }

        return publications;
    }

    private List<Publication> createPublications(Stream<Publication> publications) {
        return publications
            .map(attempt(pub -> resourceService.createPublication(pub)))
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
        when(mockClock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(PUBLICATION_UPDATE_TIME)
            .thenReturn(DOI_REQUEST_CREATION_TIME)
            .thenReturn(DOI_REQUEST_UPDATE_TIME);
    }
}