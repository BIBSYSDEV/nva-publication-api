package no.unit.nva.publication.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchPublicationMainTitleHandler extends ApiGatewayHandler<Void, Map<String, String>> {

    public static final String PUBLICATION_NOT_FOUND_MESSAGE = "Publication not found";
    public static final String INVALID_ID_FORMAT_MESSAGE = "Invalid publication ID format";
    public static final String INTERNAL_ERROR_MESSAGE = "Internal server error";
    
    private final ResourceService resourceService;

    @JacocoGenerated
    public FetchPublicationMainTitleHandler() {
        this(ResourceService.defaultService(), new Environment());
    }

    public FetchPublicationMainTitleHandler(ResourceService resourceService, Environment environment) {
        super(Void.class, environment);
        this.resourceService = resourceService;
    }

    @Override
    protected Map<String, String> processInput(Void input, RequestInfo requestInfo, Context context) 
            throws ApiGatewayException {
        
        var identifier = getAndValidateIdentifier(requestInfo);
        var resource = fetchResource(identifier);
        
        return createResponse(identifier, resource);
    }

    private SortableIdentifier getAndValidateIdentifier(RequestInfo requestInfo) throws BadRequestException {
        try {
            return RequestUtil.getIdentifier(requestInfo);
        } catch (Exception e) {
            throw new BadRequestException(INVALID_ID_FORMAT_MESSAGE);
        }
    }

    private Resource fetchResource(SortableIdentifier identifier) throws NotFoundException {
        try {
            return Resource.resourceQueryObject(identifier)
                    .fetch(resourceService)
                    .orElseThrow(() -> new NotFoundException(PUBLICATION_NOT_FOUND_MESSAGE));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(INTERNAL_ERROR_MESSAGE, e);
        }
    }

    private Map<String, String> createResponse(SortableIdentifier identifier, Resource resource) {
        var publication = resource.toPublication();
        var mainTitle = publication.getEntityDescription() != null 
                ? publication.getEntityDescription().getMainTitle() 
                : null;
        
        return Map.of(
                "id", identifier.toString(),
                "mainTitle", mainTitle != null ? mainTitle : ""
        );
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Map<String, String> output) {
        return HttpURLConnection.HTTP_OK;
    }
}