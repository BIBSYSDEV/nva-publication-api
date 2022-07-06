package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
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

    public static PublishingRequest createAndPersistPublishingRequest(PublishingRequestService requestService,
                                                                      Publication publication)
        throws ApiGatewayException {
        var userInstance = UserInstance.fromPublication(publication);
        return requestService.createPublishingRequest(userInstance, publication.getIdentifier());
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

    public static InputStream createUpdatePublishingRequestWithAccessRight(Publication publication,
                                                                           UpdatePublishingRequest updateRequest,
                                                                           URI customerId,
                                                                           SortableIdentifier publishingRequestIdentifier
    )
        throws JsonProcessingException {
        final HandlerRequestBuilder<UpdatePublishingRequest> builder = getRequestBuilder(publication,
                                                                                         updateRequest,
                                                                                         customerId,
                                                                                         publishingRequestIdentifier);
        return builder
            .withAccessRights(customerId, AccessRight.APPROVE_PUBLISH_REQUEST.toString())
            .build();
    }

    public static InputStream createUpdatePublishingRequestMissingAccessRight(Publication publication,
                                                                              UpdatePublishingRequest updateRequest,
                                                                              URI customerId,
                                                                              SortableIdentifier publishingIdentifier)
        throws JsonProcessingException {
        return getRequestBuilder(publication, updateRequest, customerId, publishingIdentifier).build();
    }

    public static UpdatePublishingRequest createUpdateRequest() {
        var updateRequest = new UpdatePublishingRequest();
        updateRequest.setPublishingRequestStatus(PublishingRequestStatus.APPROVED);
        return updateRequest;
    }

    public static ResourceOwner randomResourceOwner() {
        return new ResourceOwner(randomString(), randomUri());
    }

    public static Publication createAndPersistPublication(ResourceService resourceService) throws ApiGatewayException {
        return createAndPersistPublication(resourceService, randomOrganization(), randomResourceOwner());
    }

    private static HandlerRequestBuilder<UpdatePublishingRequest> getRequestBuilder(
        Publication publication,
        UpdatePublishingRequest updateRequest,
        URI customerId,
        SortableIdentifier publishingIdentifier)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdatePublishingRequest>(JsonUtils.dtoObjectMapper)
            .withBody(updateRequest)
            .withNvaUsername(publication.getResourceOwner().getOwner())
            .withCustomerId(customerId)
            .withPathParameters(Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, publication.getIdentifier().toString(),
                                       PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER, publishingIdentifier.toString()));
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
}
