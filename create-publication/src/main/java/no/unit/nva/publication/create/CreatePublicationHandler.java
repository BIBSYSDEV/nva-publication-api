package no.unit.nva.publication.create;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.util.ContextUtil;
import no.unit.nva.model.util.OrgNumberMapper;
import no.unit.nva.publication.JsonLdContextUtil;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static nva.commons.utils.JsonUtils.objectMapper;

public class CreatePublicationHandler extends ApiGatewayHandler<Publication, JsonNode> {

    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";
    public static final String INPUT_ERROR = "Input is not a valid Publication";
    public static final String ORG_NUMBER_COUNTRY_PREFIX_NORWAY = "NO";

    private final PublicationService publicationService;

    /**
     * Default constructor for CreatePublicationHandler.
     */
    @JacocoGenerated
    public CreatePublicationHandler() {
        this(new DynamoDBPublicationService(
                        AmazonDynamoDBClientBuilder.defaultClient(),
                        objectMapper,
                        new Environment()),
                new Environment());
    }

    /**
     * Constructor for CreatePublicationHandler.
     *
     * @param publicationService publicationService
     * @param environment        environment
     */
    public CreatePublicationHandler(PublicationService publicationService,
                                    Environment environment) {
        super(Publication.class, environment, LoggerFactory.getLogger(CreatePublicationHandler.class));
        this.publicationService = publicationService;
    }

    @Override
    protected JsonNode processInput(Publication input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        Optional<Publication> publication = Optional.ofNullable(input);
        Publication createdPublication;
        if (publication.isPresent()) {
            createdPublication = publicationService.createPublication(publication.get());
        } else {
            createdPublication = publicationService.createPublication(newPublication(requestInfo));
        }
        return toJsonNodeWithContext(createdPublication);
    }

    protected Publication newPublication(RequestInfo requestInfo) throws ApiGatewayException {
        Instant now = Instant.now();
        return new Publication.Builder()
                .withIdentifier(UUID.randomUUID())
                .withCreatedDate(now)
                .withModifiedDate(now)
                .withOwner(RequestUtil.getOwner(requestInfo))
                .withPublisher(new Organization.Builder()
                        .withId(toPublisherId(RequestUtil.getOrgNumber(requestInfo)))
                        .build()
                )
                .withStatus(PublicationStatus.DRAFT)
                .build();
    }

    private URI toPublisherId(String orgNumber) {

        if (orgNumber.startsWith(ORG_NUMBER_COUNTRY_PREFIX_NORWAY)) {
            //TODO: Remove this if and when datamodel has support for OrgNumber country prefix
            return OrgNumberMapper.toCristinId(orgNumber.substring(ORG_NUMBER_COUNTRY_PREFIX_NORWAY.length()));
        }
        return OrgNumberMapper.toCristinId(orgNumber);
    }

    protected JsonNode toJsonNodeWithContext(Publication publication) {
        JsonNode publicationJson = objectMapper.valueToTree(publication);
        new JsonLdContextUtil(objectMapper)
                .getPublicationContext(PUBLICATION_CONTEXT_JSON)
                .ifPresent(publicationContext -> ContextUtil.injectContext(publicationJson, publicationContext));
        return publicationJson;
    }

    @Override
    protected Integer getSuccessStatusCode(Publication input, JsonNode output) {
        return HttpStatus.SC_CREATED;
    }
}