package no.unit.nva.publication.modify;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.api.UpdatePublicationRequest;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class ModifyPublicationHandler extends ApiGatewayHandler<UpdatePublicationRequest, PublicationResponse> {
    
    public static final String IDENTIFIER_MISMATCH_ERROR_MESSAGE = "Identifiers in path and in body, do not match";
    private final ResourceService resourceService;
    
    /**
     * Default constructor for MainHandler.
     */
    @JacocoGenerated
    public ModifyPublicationHandler() {
        this(new ResourceService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                Clock.systemDefaultZone()),
            new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param resourceService publicationService
     * @param environment     environment
     */
    public ModifyPublicationHandler(ResourceService resourceService,
                                    Environment environment) {
        super(UpdatePublicationRequest.class, environment);
        this.resourceService = resourceService;
    }

    @Override
    protected PublicationResponse processInput(UpdatePublicationRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
    
        SortableIdentifier identifierInPath = RequestUtil.getIdentifier(requestInfo);
        UserInstance userInstance = RequestUtil.extractUserInstance(requestInfo);
        validateRequest(identifierInPath, input);
        Publication existingPublication = resourceService.getPublication(userInstance, identifierInPath);
    
        Publication publication = PublicationMapper.toExistingPublication(
            input,
            existingPublication
        );
    
        Publication updatedPublication = resourceService.updatePublication(publication);
    
        return PublicationMapper.convertValue(updatedPublication, PublicationResponse.class);
    }
    
    private void validateRequest(SortableIdentifier identifierInPath, UpdatePublicationRequest input)
        throws BadRequestException {
        if (identifiersDoNotMatch(identifierInPath, input)) {
            throw new BadRequestException(IDENTIFIER_MISMATCH_ERROR_MESSAGE);
        }
    }
    
    private boolean identifiersDoNotMatch(SortableIdentifier identifierInPath,
                                          UpdatePublicationRequest input) {
        return !identifierInPath.equals(input.getIdentifier());
    }
    
    @Override
    protected Integer getSuccessStatusCode(UpdatePublicationRequest input, PublicationResponse output) {
        return HttpStatus.SC_OK;
    }
}