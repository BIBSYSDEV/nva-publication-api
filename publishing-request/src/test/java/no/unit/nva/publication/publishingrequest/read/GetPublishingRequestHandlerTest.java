package no.unit.nva.publication.publishingrequest.read;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.publishingrequest.PublishingRequestCaseDto;
import no.unit.nva.publication.publishingrequest.PublishingRequestUtils;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class GetPublishingRequestHandlerTest extends ResourcesLocalTest {
    
    private ResourceService resourceService;
    private PublishingRequestService publishingRequestService;
    private GetPublishingRequestHandler handler;
    private ByteArrayOutputStream output;
    private FakeContext context;
    
    public static Stream<Function<PublishingRequestCase, UserInstance>> wrongUserProvider() {
        Function<PublishingRequestCase, UserInstance> wrongUsername =
            publishingRequest -> UserInstance.create(randomString(), publishingRequest.getCustomerId());
        Function<PublishingRequestCase, UserInstance> wrongClient =
            publishingRequest -> UserInstance.create(publishingRequest.getOwner(), randomUri());
        return Stream.of(wrongUsername, wrongClient);
    }
    
    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.publishingRequestService = new PublishingRequestService(client, Clock.systemDefaultZone());
        this.handler = new GetPublishingRequestHandler(publishingRequestService);
        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }
    
    @Test
    void shouldReturnTicketWhenTicketExistsAndRequesterIsTicketOwner()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var publishingRequest = createPublishingRequest(publication);
        var request = createRequest(publishingRequest);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, PublishingRequestCaseDto.class);
        var actualResponseObject = response.getBodyObject(PublishingRequestCaseDto.class);
        var expectedResponseObject = PublishingRequestCaseDto.createResponseObject(publishingRequest);
        assertThat(actualResponseObject, is(equalTo(expectedResponseObject)));
    }
    
    @ParameterizedTest(name = "should return not found when ticket exists but requester is not the ticket owner")
    @MethodSource("wrongUserProvider")
    void shouldReturnNotFoundWhenTicketExistsButRequesterIsNotTheTicketOwnerAndIsNotACurator(
        Function<PublishingRequestCase, UserInstance> wrongUserProvider)
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var publishingRequest = createPublishingRequest(publication);
        var request = createRequest(publishingRequest, wrongUserProvider);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, PublishingRequestCaseDto.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }
    
    @Test
    void shouldReturnTicketWhenTicketExistsAndRequesterIsCurator()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var publishingRequest = createPublishingRequest(publication);
        var request = createCuratorRequest(publishingRequest);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, PublishingRequestCaseDto.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        var actualResponseObject = response.getBodyObject(PublishingRequestCaseDto.class);
        var expectedResponseObject = PublishingRequestCaseDto.createResponseObject(publishingRequest);
        assertThat(actualResponseObject, is(equalTo(expectedResponseObject)));
    }
    
    private InputStream createRequest(PublishingRequestCase publishingRequest,
                                      Function<PublishingRequestCase, UserInstance> userProvider)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(userProvider.apply(publishingRequest).getUserIdentifier())
            .withCustomerId(userProvider.apply(publishingRequest).getOrganizationUri())
            .withPathParameters(constructPathParameters(publishingRequest))
            .build();
    }
    
    private InputStream createRequest(PublishingRequestCase publishingRequest) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(publishingRequest.getOwner())
            .withCustomerId(publishingRequest.getCustomerId())
            .withPathParameters(constructPathParameters(publishingRequest))
            .build();
    }
    
    private InputStream createCuratorRequest(PublishingRequestCase publishingRequest) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(randomString())
            .withCustomerId(publishingRequest.getCustomerId())
            .withAccessRights(publishingRequest.getCustomerId(), AccessRight.APPROVE_PUBLISH_REQUEST.toString())
            .withPathParameters(constructPathParameters(publishingRequest))
            .build();
    }
    
    private Map<String, String> constructPathParameters(PublishingRequestCase publishingRequest) {
        return Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER,
            publishingRequest.getResourceIdentifier().toString(),
            PublishingRequestUtils.PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER,
            publishingRequest.getIdentifier().toString());
    }
    
    private PublishingRequestCase createPublishingRequest(Publication publication) throws ApiGatewayException {
        var publishingRequest =
            PublishingRequestCase.createOpeningCaseObject(UserInstance.fromPublication(publication),
                publication.getIdentifier());
        return (PublishingRequestCase)
                   publishingRequestService.createTicket(publishingRequest,PublishingRequestCase.class);
    }
    
    private Publication createPublication() throws ApiGatewayException {
        var publication = randomPublication();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
}