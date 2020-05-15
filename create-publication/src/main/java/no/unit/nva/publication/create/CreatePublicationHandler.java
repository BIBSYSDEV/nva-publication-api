package no.unit.nva.publication.create;

import static nva.commons.utils.JsonUtils.objectMapper;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.model.util.OrgNumberMapper;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

public class CreatePublicationHandler extends ApiGatewayHandler<CreatePublicationRequest, PublicationResponse> {

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
        super(CreatePublicationRequest.class, environment, LoggerFactory.getLogger(CreatePublicationHandler.class));
        this.publicationService = publicationService;
    }

    @Override
    protected PublicationResponse processInput(CreatePublicationRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        Publication newPublication = PublicationMapper.toNewPublication(
            input,
            RequestUtil.getOwner(requestInfo),
            null, //TODO: set handle
            null, //TODO: set link
            createPublisher(RequestUtil.getOrgNumber(requestInfo)));

        Publication createdPublication = publicationService.createPublication(newPublication);

        setLocationHeader(createdPublication.getIdentifier());

        return PublicationMapper.toResponse(createdPublication, PublicationResponse.class);
    }

    private void setLocationHeader(UUID identifier) {
        setAdditionalHeadersSupplier(
            () -> Map.of(HttpHeaders.LOCATION, "publication/" + identifier.toString()));
    }

    private Organization createPublisher(String orgNumber) {
        return new Builder()
            .withId(toPublisherId(orgNumber))
            .build();
    }

    private URI toPublisherId(String orgNumber) {

        if (orgNumber.startsWith(ORG_NUMBER_COUNTRY_PREFIX_NORWAY)) {
            //TODO: Remove this if and when datamodel has support for OrgNumber country prefix
            return OrgNumberMapper.toCristinId(orgNumber.substring(ORG_NUMBER_COUNTRY_PREFIX_NORWAY.length()));
        }
        return OrgNumberMapper.toCristinId(orgNumber);
    }

    @Override
    protected Integer getSuccessStatusCode(CreatePublicationRequest input, PublicationResponse output) {
        return HttpStatus.SC_CREATED;
    }
}