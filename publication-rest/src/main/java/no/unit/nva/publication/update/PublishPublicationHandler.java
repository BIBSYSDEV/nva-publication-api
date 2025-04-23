package no.unit.nva.publication.update;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.PublishingService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class PublishPublicationHandler extends ApiGatewayHandler<Void, Void> {

    private final PublishingService publishingService;

    @JacocoGenerated
    public PublishPublicationHandler() {
        this(PublishingService.defaultService(), new Environment());
    }

    public PublishPublicationHandler(PublishingService publishingService, Environment environment) {
        super(Void.class, environment);
        this.publishingService = publishingService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //No input to validate
    }

    @Override
    protected Void processInput(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var resourceIdentifier = RequestUtil.getIdentifier(requestInfo);
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        try {
            publishingService.publishResource(resourceIdentifier, userInstance);
        } catch (Exception e) {
            handleException(e);
        }

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HTTP_ACCEPTED;
    }

    private void handleException(Exception exception) throws ApiGatewayException {
        if (exception instanceof IllegalStateException) {
            throw new BadRequestException(exception.getMessage());
        } else if (exception instanceof ApiGatewayException apiGatewayException) {
            throw apiGatewayException;
        } else {
            throw new BadGatewayException(exception.getMessage());
        }
    }
}
