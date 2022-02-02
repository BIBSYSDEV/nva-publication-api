package no.unit.nva.publication.events.handlers.create;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.create.CreatePublicationRequest;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.s3.S3Client;

public class CreatePublishedPublicationHandler extends EventHandler<EventReference, PublicationResponse> {

    private final S3Client s3Client;

    public CreatePublishedPublicationHandler(S3Client s3Client) {
        super(EventReference.class);
        this.s3Client = s3Client;
    }

    @Override
    protected PublicationResponse processInput(EventReference eventDetail,
                                               AwsEventBridgeEvent<EventReference> event,
                                               Context context) {
        String input = readEventBodyFromS3(eventDetail);

        return attempt(() -> parseInput(input))
            .map(CreatePublicationRequest::toPublication)
            .map(this::addArbitraryIdentifierToAvoidExceptionWhenGeneratingResponse)
            .map(PublicationResponse::fromPublication)
            .orElseThrow();
    }

    private String readEventBodyFromS3(EventReference eventBody) {
        String s3Bucket = eventBody.getUri().getHost();
        var s3Driver = new S3Driver(s3Client, s3Bucket);
        return s3Driver.getFile(new UriWrapper(eventBody.getUri()).toS3bucketPath());
    }

    private Publication addArbitraryIdentifierToAvoidExceptionWhenGeneratingResponse(Publication p) {
        p.setIdentifier(SortableIdentifier.next());
        return p;
    }

    private CreatePublicationRequest parseInput(String input) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(input, CreatePublicationRequest.class);
    }
}
