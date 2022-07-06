package no.unit.nva.publication.publishingrequest.create;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static no.unit.nva.publication.PublicationServiceConfig.SUPPORT_MESSAGE_PATH;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.createUserInstance;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.defaultRequestService;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.HttpHeaders;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.PublishingRequest;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class CreatePublishingRequestHandler extends ApiGatewayHandler<PublicationPublishRequest, Void> {

    private final PublishingRequestService requestService;

    @JacocoGenerated
    public CreatePublishingRequestHandler() {
        this(defaultRequestService());
    }

    public CreatePublishingRequestHandler(PublishingRequestService requestService) {
        super(PublicationPublishRequest.class);
        this.requestService = requestService;
    }

    @Override
    protected Void processInput(PublicationPublishRequest input,
                                RequestInfo requestInfo,
                                Context context) throws ApiGatewayException {
        final var userInstance = createUserInstance(requestInfo);
        final var publicationIdentifier =
            new SortableIdentifier(requestInfo.getPathParameter(PUBLICATION_IDENTIFIER_PATH_PARAMETER));

        var publishingRequest = PublishingRequest.create(userInstance,publicationIdentifier);
        var newPublishingRequest = requestService.createPublishingRequest(publishingRequest);

        var persistedRequest = requestService.getPublishingRequest(newPublishingRequest);
        addLocationHeader(persistedRequest);
        return null;
    }

    private void addLocationHeader(PublishingRequest persistedRequest) {
        addAdditionalHeaders(() -> Map.of(HttpHeaders.LOCATION, locationHeader(persistedRequest).toString()));
    }

    private URI locationHeader(PublishingRequest persistedRequest) {
        return UriWrapper.fromHost(API_HOST)
            .addChild(PUBLICATION_PATH)
            .addChild(persistedRequest.getResourceIdentifier().toString())
            .addChild(SUPPORT_MESSAGE_PATH)
            .addChild(persistedRequest.getIdentifier().toString())
            .getUri();
    }

    @Override
    protected Integer getSuccessStatusCode(PublicationPublishRequest input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }
}
