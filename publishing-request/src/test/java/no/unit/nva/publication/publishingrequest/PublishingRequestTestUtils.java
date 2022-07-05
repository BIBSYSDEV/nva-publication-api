package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.publishingrequest.create.CreatePublishingRequestHandler;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.paths.UriWrapper;

public class PublishingRequestTestUtils {

    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");

    public static Clock setupMockClock() {
        var mockClock = mock(Clock.class);
        when(mockClock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(PUBLICATION_UPDATE_TIME);
        return mockClock;
    }

    public static InputStream createListPublishingRequest(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdatePublishingRequest>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(publication.getResourceOwner().getOwner())
            .withCustomerId(publication.getPublisher().getId())
            .withAccessRights(publication.getPublisher().getId(), AccessRight.APPROVE_PUBLISH_REQUEST.toString())
            .withPathParameters(Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, publication.getIdentifier().toString()))
            .build();
    }

    public static InputStream createListPublishingRequestWithMissingAccessRightToApprove(Publication publication,
                                                                                         URI customerId)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdatePublishingRequest>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(publication.getResourceOwner().getOwner())
            .withCustomerId(customerId)
            .withPathParameters(Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, publication.getIdentifier().toString()))
            .build();
    }

    public static URI createAndPersistPublishingRequest(PublishingRequestService requestService,
                                                        Publication publication,
                                                        Context context) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        var httpRequest = createPublishingRequest(publication);
        new CreatePublishingRequestHandler(requestService).handleRequest(httpRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        return Optional.of(response.getHeaders().get(HttpHeaders.LOCATION)).map(URI::create).orElseThrow();
    }

    public static InputStream createPublishingRequest(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(publication.getResourceOwner().getOwner())
            .withCustomerId(publication.getPublisher().getId())
            .withPathParameters(Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, publication.getIdentifier().toString()))
            .build();
    }

    public static InputStream createGetPublishingRequest(URI publishingRequestId, Publication publication)
        throws JsonProcessingException {
        var customerId = publication.getPublisher().getId();
        var publishingRequestUri = UriWrapper.fromUri(publishingRequestId);
        var publishingRequestIdentifier = publishingRequestUri.getLastPathElement();
        var publicationIdentifier = extractPublicationIdentifierFromPublishingRequestId(publishingRequestUri);

        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(publication.getResourceOwner().getOwner())
            .withCustomerId(customerId)
            .withAccessRights(customerId, AccessRight.APPROVE_PUBLISH_REQUEST.toString())
            .withPathParameters(
                Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, publicationIdentifier,
                       PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER, publishingRequestIdentifier)
            )
            .build();
    }

    private static String extractPublicationIdentifierFromPublishingRequestId(UriWrapper publishingRequestUri) {
        return publishingRequestUri.getParent()
            .flatMap(UriWrapper::getParent)
            .map(UriWrapper::getLastPathElement)
            .orElseThrow();
    }

    public static InputStream createGetPublishingRequest(Publication publication,
                                                         PublishingRequest publishingRequest,
                                                         URI customerId)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(publication.getResourceOwner().getOwner())
            .withCustomerId(customerId)
            .withAccessRights(customerId, AccessRight.APPROVE_PUBLISH_REQUEST.toString())
            .withPathParameters(
                Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, publication.getIdentifier().toString(),
                       PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER,
                       publishingRequest.getIdentifier().toString())
            )
            .build();
    }

    public static InputStream createGetPublishingRequestMissingAccessRightApprove(Publication publication,
                                                                                  URI customerId)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(publication.getResourceOwner().getOwner())
            .withCustomerId(customerId)
            .withPathParameters(Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, publication.getIdentifier().toString()))
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
            .withNvaUsername(publication.getResourceOwner().getOwner())
            .withCustomerId(customerId)
            .withPathParameters(Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, publicationIdentifier));
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

    private static Publication createAndPersistPublication(ResourceService resourceService,
                                                           Organization publisher,
                                                           ResourceOwner resourceOwner) throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        publication.setPublisher(publisher);
        publication.setResourceOwner(resourceOwner);
        var storeResult = resourceService.createPublication(UserInstance.fromPublication(publication), publication);
        return resourceService.getPublication(storeResult);
    }

    public static Publication createAndPersistPublication(ResourceService resourceService) throws ApiGatewayException {
        return createAndPersistPublication(resourceService, randomOrganization(), randomResourceOwner());
    }
}
