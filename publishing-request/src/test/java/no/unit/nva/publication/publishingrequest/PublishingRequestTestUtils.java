package no.unit.nva.publication.publishingrequest;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.publishingrequest.create.CreatePublishingRequestHandler;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.API_PUBLICATION_PATH_IDENTIFIER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PublishingRequestTestUtils {

    public static final String ALLOW_ALL_ORIGINS = "*";
    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");


    public static Environment mockEnvironment() {
        var environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALLOW_ALL_ORIGINS);
        return environment;
    }

    public static Clock setupMockClock() {
        var mockClock = mock(Clock.class);
        when(mockClock.instant())
                .thenReturn(PUBLICATION_CREATION_TIME)
                .thenReturn(PUBLICATION_UPDATE_TIME);
        return mockClock;
    }

    public static InputStream createListPublishingRequest(Publication publication, URI customerId)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdatePublishingRequest>(JsonUtils.dtoObjectMapper)
                .withNvaUsername(publication.getOwner())
                .withCustomerId(customerId)
                .withAccessRights(customerId, AccessRight.APPROVE_PUBLISH_REQUEST.toString())
                .withPathParameters(Map.of(API_PUBLICATION_PATH_IDENTIFIER, publication.getIdentifier().toString()))
                .build();
    }

    public static InputStream createListPublishingRequestWithMissingAccessRightToApprove(Publication publication,
                                                                                         URI customerId)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdatePublishingRequest>(JsonUtils.dtoObjectMapper)
                .withNvaUsername(publication.getOwner())
                .withCustomerId(customerId)
                .withPathParameters(Map.of(API_PUBLICATION_PATH_IDENTIFIER, publication.getIdentifier().toString()))
                .build();
    }


    public static void createAndPersistPublishingRequest(PublishingRequestService requestService,
                                                         Publication publication,
                                                         Environment environment,
                                                         Context context) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        new CreatePublishingRequestHandler(requestService, environment).handleRequest(
                createPublishingRequest(publication, publication.getPublisher().getId()),
                outputStream,
                context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_CREATED));
    }

    public static InputStream createPublishingRequest(Publication publication, URI customerId)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                .withNvaUsername(publication.getOwner())
                .withCustomerId(customerId)
                .withPathParameters(Map.of(API_PUBLICATION_PATH_IDENTIFIER, publication.getIdentifier().toString()))
                .build();
    }

    public static InputStream createGetPublishingRequest(Publication publication, URI customerId)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                .withNvaUsername(publication.getOwner())
                .withCustomerId(customerId)
                .withAccessRights(customerId, AccessRight.APPROVE_PUBLISH_REQUEST.toString())
                .withPathParameters(Map.of(API_PUBLICATION_PATH_IDENTIFIER, publication.getIdentifier().toString()))
                .build();
    }

    public static InputStream createGetPublishingRequestMissingAccessRightApprove(Publication publication,
                                                                                  URI customerId)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                .withNvaUsername(publication.getOwner())
                .withCustomerId(customerId)
                .withPathParameters(Map.of(API_PUBLICATION_PATH_IDENTIFIER, publication.getIdentifier().toString()))
                .build();
    }

    public static InputStream createUpdatePublishingRequestWithAccessRight(Publication publication,
                                                                           UpdatePublishingRequest updateRequest,
                                                                           URI customerId,
                                                                           String publicationIdentifier)
            throws JsonProcessingException {
        final HandlerRequestBuilder<UpdatePublishingRequest> builder = getRequestBuilder(publication,
                updateRequest,
                customerId,
                publicationIdentifier);
        return builder
                .withAccessRights(customerId, AccessRight.APPROVE_PUBLISH_REQUEST.toString())
                .build();
    }

    private static HandlerRequestBuilder<UpdatePublishingRequest>
        getRequestBuilder(Publication publication,
                          UpdatePublishingRequest updateRequest,
                          URI customerId,
                          String publicationIdentifier)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdatePublishingRequest>(JsonUtils.dtoObjectMapper)
                .withBody(updateRequest)
                .withNvaUsername(publication.getOwner())
                .withCustomerId(customerId)
                .withPathParameters(Map.of(API_PUBLICATION_PATH_IDENTIFIER, publicationIdentifier));
    }

    public static InputStream createUpdatePublishingRequestMissingAccessRight(Publication publication,
                                                                              UpdatePublishingRequest updateRequest,
                                                                              URI customerId,
                                                                              String publicationIdentifier)
            throws JsonProcessingException {
        return getRequestBuilder(publication, updateRequest, customerId, publicationIdentifier).build();
    }


    public static UpdatePublishingRequest createUpdateRequest() {
        var updateRequest = new UpdatePublishingRequest();
        updateRequest.setPublishingRequestStatus(PublishingRequestStatus.APPROVED);
        return updateRequest;
    }

    public static ResourceOwner randomResourceOwner() {
        return new ResourceOwner(randomString(), randomUri());
    }

    public static Publication createAndPersistPublication(ResourceService resourceService,
                                                          Organization publisher,
                                                          ResourceOwner resourceOwner) throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        publication.setPublisher(publisher);
        publication.setResourceOwner(resourceOwner);
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
}
