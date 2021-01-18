package no.unit.nva.publication.modify;

import static nva.commons.core.JsonUtils.objectMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.api.UpdatePublicationRequest;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

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

        SortableIdentifier identifier = RequestUtil.getIdentifier(requestInfo);
        Publication existingPublication = publicationService.getPublication(identifier);

        Publication publication = PublicationMapper.toExistingPublication(
            input,
            existingPublication
        );

        Publication updatedPublication = publicationService.updatePublication(identifier, publication);

        return PublicationMapper.convertValue(updatedPublication, PublicationResponse.class);
    }

    @Override
    protected Integer getSuccessStatusCode(UpdatePublicationRequest input, PublicationResponse output) {
        return HttpStatus.SC_OK;
    }
}