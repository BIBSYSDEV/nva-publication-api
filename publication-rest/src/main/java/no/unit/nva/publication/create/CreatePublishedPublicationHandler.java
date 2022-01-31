package no.unit.nva.publication.create;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;

public class CreatePublishedPublicationHandler extends
                                               ApiGatewayHandler<CreatePublicationRequest, PublicationResponse> {

    public CreatePublishedPublicationHandler() {
        super(CreatePublicationRequest.class);
    }

    @Override
    protected PublicationResponse processInput(CreatePublicationRequest input, RequestInfo requestInfo,
                                               Context context) {
        return attempt(input::toPublication)
            .map(this::addArbitraryIdentifier)
            .map(PublicationResponse::fromPublication)
            .orElseThrow();
    }

    @Override
    protected Integer getSuccessStatusCode(CreatePublicationRequest input, PublicationResponse output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private Publication addArbitraryIdentifier(Publication p) {
        p.setIdentifier(SortableIdentifier.next());
        return p;
    }
}
