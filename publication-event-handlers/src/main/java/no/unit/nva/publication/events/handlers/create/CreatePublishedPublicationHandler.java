package no.unit.nva.publication.events.handlers.create;

import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.PublicationServiceConfig.EXTERNAL_SERVICES_HTTP_CLIENT;
import static no.unit.nva.publication.events.handlers.create.HardCodedValues.HARDCODED_OWNER_AFFILIATION;
import static no.unit.nva.publication.events.handlers.create.HardCodedValues.UNIT_CUSTOMER_ID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.http.HttpClient;
import java.time.Clock;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.create.CreatePublicationRequest;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class CreatePublishedPublicationHandler extends EventHandler<EventReference, PublicationResponse> {

    private final S3Client s3Client;
    private final ResourceService resourceService;
    private static final Logger logger = LoggerFactory.getLogger(CreatePublishedPublicationHandler.class);

    @JacocoGenerated
    public CreatePublishedPublicationHandler() {
        this(S3Driver.defaultS3Client().build(),
             DEFAULT_DYNAMODB_CLIENT,
             EXTERNAL_SERVICES_HTTP_CLIENT);
    }

    public CreatePublishedPublicationHandler(S3Client s3Client, AmazonDynamoDB dynamoClient, HttpClient httpClient) {
        super(EventReference.class);
        this.s3Client = s3Client;
        this.resourceService = new ResourceService(dynamoClient, httpClient, Clock.systemDefaultZone());
    }

    @Override
    protected PublicationResponse processInput(EventReference eventDetail,
                                               AwsEventBridgeEvent<EventReference> event,
                                               Context context) {
        var input = readEventBodyFromS3(eventDetail);

        return attempt(() -> parseInput(input))
            .map(CreatePublicationRequest::toPublication)
            .map(this::addOwnerAndPublisher)
            .map(this::storeAsPublishedPublication)
            .map(PublicationResponse::fromPublication)
            .orElseThrow();
    }

    private Publication addOwnerAndPublisher(Publication publication) {
        Organization customer = new Organization.Builder().withId(UNIT_CUSTOMER_ID).build();
        ResourceOwner resourceOwner = new ResourceOwner(randomUnitUser(), HARDCODED_OWNER_AFFILIATION);
        var publicationCopy = publication.copy().withPublisher(customer).withResourceOwner(resourceOwner).build();
        String message = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(publicationCopy)).orElseThrow();
        logger.debug("Publication to be stored" + message);
        return publicationCopy;
    }

    private String randomUnitUser() {
        return randomString() + "@unit.no";
    }

    private Publication storeAsPublishedPublication(Publication publication) throws TransactionFailedException {
        return resourceService.createPublicationFromImportedEntry(publication);
    }

    private String readEventBodyFromS3(EventReference eventBody) {
        var s3Bucket = eventBody.getUri().getHost();
        var s3Driver = new S3Driver(s3Client, s3Bucket);
        return s3Driver.getFile(new UriWrapper(eventBody.getUri()).toS3bucketPath());
    }

    private CreatePublicationRequest parseInput(String input) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(input, CreatePublicationRequest.class);
    }
}
