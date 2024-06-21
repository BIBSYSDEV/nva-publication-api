package no.unit.nva.publication.events.handlers.create;

import static no.unit.nva.publication.events.handlers.create.HardCodedValues.HARDCODED_OWNER_AFFILIATION;
import static no.unit.nva.publication.events.handlers.create.HardCodedValues.UNIT_CUSTOMER_ID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.events.bodies.CreatePublicationRequest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.s3.S3Client;

public class CreatePublishedPublicationHandler extends EventHandler<EventReference, PublicationResponseElevatedUser> {

    private final S3Client s3Client;
    private final ResourceService resourceService;
    
    @JacocoGenerated
    public CreatePublishedPublicationHandler() {
        this(S3Driver.defaultS3Client().build(), ResourceService.defaultService());
    }
    
    public CreatePublishedPublicationHandler(S3Client s3Client, ResourceService resourceService) {
        super(EventReference.class);
        this.s3Client = s3Client;
        this.resourceService = resourceService;
    }
    
    @Override
    protected PublicationResponseElevatedUser processInput(EventReference eventDetail,
                                               AwsEventBridgeEvent<EventReference> event,
                                               Context context) {
        var input = readEventBodyFromS3(eventDetail);
        
        return attempt(() -> parseInput(input))
                   .map(CreatePublicationRequest::toPublication)
                   .map(this::addOwnerAndPublisher)
                   .map(this::storeAsPublishedPublication)
                   .map(PublicationResponseElevatedUser::fromPublication)
                   .orElseThrow();
    }
    
    private Publication addOwnerAndPublisher(Publication publication) {
        Organization customer = new Organization.Builder().withId(UNIT_CUSTOMER_ID).build();
        ResourceOwner resourceOwner = new ResourceOwner(randomUnitUsername(),
                                                        HARDCODED_OWNER_AFFILIATION);
        return publication.copy().withPublisher(customer).withResourceOwner(resourceOwner).build();
    }

    private Username randomUnitUsername() {
        return new Username(randomUnitUser());
    }

    private String randomUnitUser() {
        return randomString() + "@unit.no";
    }
    
    private Publication storeAsPublishedPublication(Publication publication) {
        return resourceService.createPublicationFromImportedEntry(publication, null);
    }
    
    private String readEventBodyFromS3(EventReference eventBody) {
        var s3Bucket = eventBody.getUri().getHost();
        var s3Driver = new S3Driver(s3Client, s3Bucket);
        return s3Driver.getFile(UriWrapper.fromUri(eventBody.getUri()).toS3bucketPath());
    }
    
    private CreatePublicationRequest parseInput(String input) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(input, CreatePublicationRequest.class);
    }
}
