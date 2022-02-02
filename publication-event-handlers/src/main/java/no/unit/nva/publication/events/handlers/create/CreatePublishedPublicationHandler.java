package no.unit.nva.publication.events.handlers.create;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.create.CreatePublicationRequest;

public class CreatePublishedPublicationHandler extends
                                               EventHandler<CreatePublicationRequest, PublicationResponse> {

    public CreatePublishedPublicationHandler() {
        super(CreatePublicationRequest.class);
    }

    @Override
    protected PublicationResponse processInput(CreatePublicationRequest input,
                                               AwsEventBridgeEvent<CreatePublicationRequest> event,
                                               Context context) {
        return attempt(input::toPublication)
            .map(this::addArbitraryIdentifier)
            .map(PublicationResponse::fromPublication)
            .orElseThrow();
    }

    private Publication addArbitraryIdentifier(Publication p) {
        p.setIdentifier(SortableIdentifier.next());
        return p;
    }
}
