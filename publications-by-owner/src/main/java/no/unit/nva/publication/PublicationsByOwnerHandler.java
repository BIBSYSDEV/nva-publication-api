package no.unit.nva.publication;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.Environment;
import no.unit.nva.GatewayResponse;
import no.unit.nva.PublicationHandler;
import no.unit.nva.model.PublicationSummary;
import no.unit.nva.service.PublicationService;
import no.unit.nva.service.impl.DynamoDBPublicationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import static no.unit.nva.Logger.log;
import static no.unit.nva.Logger.logError;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.OK;

public class PublicationsByOwnerHandler extends PublicationHandler {

    public static final String BODY = "/body";

    public static final String REQUEST_CONTEXT_AUTHORIZER_CLAIMS = "/requestContext/authorizer/claims/";
    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    public static final String CUSTOM_ORG_NUMBER = "custom:orgNumber";
    public static final String MISSING_CLAIM_IN_REQUEST_CONTEXT =
            "Missing claim in requestContext: ";

    private final PublicationService publicationService;

    public PublicationsByOwnerHandler() {
        this(createObjectMapper(), DynamoDBPublicationService.create(new Environment()),
                new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param objectMapper objectMapper
     * @param environment  environment
     */
    public PublicationsByOwnerHandler(ObjectMapper objectMapper, PublicationService publicationService,
                                      Environment environment) {
        super(objectMapper, environment);
        this.publicationService = publicationService;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        String owner;
        String publisherId;
        try {
            JsonNode event = objectMapper.readTree(input);
            owner = getClaimValueFromRequestContext(event, CUSTOM_FEIDE_ID);
            publisherId = getClaimValueFromRequestContext(event, CUSTOM_ORG_NUMBER);
        } catch (Exception e) {
            logError(e);
            writeErrorResponse(output, BAD_REQUEST, e);
            return;
        }

        log(String.format("Requested publications for owner with feideId=%s and publisher with orgNumber=%s",
                owner,
                publisherId));

        try {
            List<PublicationSummary> publicationsByOwner = publicationService.getPublicationsByOwner(
                    owner, publisherId, null);
            objectMapper.writeValue(output, new GatewayResponse<>(
                    objectMapper.writeValueAsString(
                            new PublicationsByOwnerResponse(publicationsByOwner)), headers(), OK.getStatusCode()));
        } catch (IOException e) {
            logError(e);
            writeErrorResponse(output, BAD_GATEWAY, e);
        } catch (Exception e) {
            logError(e);
            writeErrorResponse(output, INTERNAL_SERVER_ERROR, e);
        }
    }

    private String getClaimValueFromRequestContext(JsonNode event, String claimName) {
        return Optional.ofNullable(event.at(REQUEST_CONTEXT_AUTHORIZER_CLAIMS + claimName).textValue())
                .orElseThrow(() -> new IllegalArgumentException(MISSING_CLAIM_IN_REQUEST_CONTEXT + claimName));
    }
}
