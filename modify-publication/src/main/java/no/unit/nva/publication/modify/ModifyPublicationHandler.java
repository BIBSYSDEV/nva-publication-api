package no.unit.nva.publication.modify;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.api.UpdatePublicationRequest;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.modify.exception.PartialContentException;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static java.util.Objects.nonNull;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpHeaders.CONTENT_RANGE;

public class ModifyPublicationHandler extends ApiGatewayHandler<UpdatePublicationRequest, PublicationResponse> {

    private final PublicationService publicationService;

    /**
     * Default constructor for MainHandler.
     */
    @JacocoGenerated
    public ModifyPublicationHandler() {
        this(new DynamoDBPublicationService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                objectMapper,
                new Environment()),
            new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param publicationService publicationService
     * @param environment        environment
     */
    public ModifyPublicationHandler(PublicationService publicationService,
                                    Environment environment) {
        super(UpdatePublicationRequest.class, environment, LoggerFactory.getLogger(ModifyPublicationHandler.class));
        this.publicationService = publicationService;
    }

    @Override
    protected PublicationResponse processInput(UpdatePublicationRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        validateRequest(requestInfo);

        UUID identifier = RequestUtil.getIdentifier(requestInfo);
        Publication existingPublication = publicationService.getPublication(identifier);

        Publication publication = PublicationMapper.toExistingPublication(
            input,
            existingPublication
        );

        Publication updatedPublication = publicationService.updatePublication(identifier, publication);

        return PublicationMapper.convertValue(updatedPublication, PublicationResponse.class);
    }

    private void validateRequest(RequestInfo requestInfo) throws PartialContentException {
        if (nonNull(requestInfo.getHeader(CONTENT_RANGE))) {
            throw new PartialContentException();
        }
    }

    @Override
    protected Integer getSuccessStatusCode(UpdatePublicationRequest input, PublicationResponse output) {
        return HttpStatus.SC_OK;
    }
}