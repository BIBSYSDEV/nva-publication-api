package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER;
import static nva.commons.core.attempt.Try.attempt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.ApiGatewayException;

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

    public static PublishingRequest createPublishingRequest(Publication publication) {
        return PublishingRequest.create(UserInstance.fromPublication(publication), publication.getIdentifier());
    }

    public static PublishingRequest createAndPersistPublishingRequest(PublishingRequestService requestService,
                                                                      Publication publication)
        throws ApiGatewayException {
        return requestService.createPublishingRequest(createPublishingRequest(publication));
    }

    public static InputStream createGetPublishingRequest(PublishingRequest publishingRequest)
        throws JsonProcessingException {
        return createGetPublishingRequest(publishingRequest, publishingRequest.getIdentifier().toString());
    }

    public static InputStream createGetPublishingRequest(PublishingRequest publishingRequest,
                                                         String identifierCandidate)
        throws JsonProcessingException {
        var customerId = publishingRequest.getCustomerId();
        var publicationIdentifier = publishingRequest.getResourceIdentifier();

        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(publishingRequest.getOwner())
            .withCustomerId(customerId)
            .withAccessRights(customerId, AccessRight.APPROVE_PUBLISH_REQUEST.toString())
            .withPathParameters(
                Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, publicationIdentifier.toString(),
                       PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER, identifierCandidate)
            )
            .build();
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

    public static InputStream createUpdatePublishingRequestWithAccessRight(PublishingRequestUpdate updateRequest,
                                                                           PublishingRequest publishingRequest
    )
        throws JsonProcessingException {
        final HandlerRequestBuilder<PublishingRequestUpdate> builder =
            requestWithPublishingRequestPathParameters(updateRequest,publishingRequest);
        var customerId = publishingRequest.getCustomerId();
        return builder
            .withAccessRights(customerId, AccessRight.APPROVE_PUBLISH_REQUEST.toString())
            .build();
    }

    public static InputStream createUpdatePublishingRequestMissingAccessRight(PublishingRequestUpdate updateRequest,
                                                                              PublishingRequest publishingRequest)
        throws JsonProcessingException {
        return requestWithPublishingRequestPathParameters(updateRequest,publishingRequest).build();
    }

    public static Publication createAndPersistDraftPublication(ResourceService resourceService)
        throws ApiGatewayException {
        return createAndPersistPublicationAndThenActOnIt(resourceService, publication -> {
        });
    }

    public static Publication createPersistAndPublishPublication(ResourceService resourceService)
        throws ApiGatewayException {
        return createAndPersistPublicationAndThenActOnIt(resourceService,
                                                         publication -> publish(resourceService, publication));
    }

    public static Publication createAndPersistPublicationAndMarkForDeletion(ResourceService resourceService)
        throws ApiGatewayException {
        return createAndPersistPublicationAndThenActOnIt(resourceService,
                                                         publication -> markForDeletion(resourceService, publication));
    }

    private static void publish(ResourceService resourceService, Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        attempt(() -> resourceService.publishPublication(userInstance, publication.getIdentifier()))
            .orElseThrow();
    }

    private static void markForDeletion(ResourceService resourceService, Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        attempt(() -> resourceService.markPublicationForDeletion(userInstance, publication.getIdentifier()))
            .orElseThrow();
    }

    private static Publication createAndPersistPublicationAndThenActOnIt(ResourceService resourceService,
                                                                         Consumer<Publication> action)
        throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var storedResult = resourceService.createPublication(userInstance, publication);
        action.accept(storedResult);
        return resourceService.getPublication(storedResult);
    }

    private static HandlerRequestBuilder<PublishingRequestUpdate> requestWithPublishingRequestPathParameters(
        PublishingRequestUpdate updateRequest,
        PublishingRequest publishingRequest)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<PublishingRequestUpdate>(JsonUtils.dtoObjectMapper)
            .withBody(updateRequest)
            .withNvaUsername(publishingRequest.getOwner())
            .withCustomerId(publishingRequest.getCustomerId())
            .withPathParameters(pathParametersForPublishingRequest(publishingRequest));
    }

    private static Map<String, String> pathParametersForPublishingRequest(PublishingRequest publishingRequest) {
        return Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER,
                      publishingRequest.getResourceIdentifier().toString(),
                      PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER,
                      publishingRequest.getIdentifier().toString());
    }
}
