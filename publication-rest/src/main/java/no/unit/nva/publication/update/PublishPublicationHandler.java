package no.unit.nva.publication.update;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.validation.ValidationException;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.PublishingService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ValidationError;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class PublishPublicationHandler extends ApiGatewayHandler<Void, Void> {

  private final PublishingService publishingService;
  private final IdentityServiceClient identityServiceClient;

  @JacocoGenerated
  public PublishPublicationHandler() {
    this(PublishingService.defaultService(), IdentityServiceClient.prepare(), new Environment());
  }

  public PublishPublicationHandler(
      PublishingService publishingService,
      IdentityServiceClient identityServiceClient,
      Environment environment) {
    super(Void.class, environment);
    this.publishingService = publishingService;
    this.identityServiceClient = identityServiceClient;
  }

  @Override
  protected void validateRequest(Void unused, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    // No input to validate
  }

  @Override
  protected Void processInput(Void unused, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var resourceIdentifier = RequestUtil.getIdentifier(requestInfo);
    var userInstance =
        RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
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
    if (exception instanceof ValidationException validation) {
      throw new BadRequestException(validation.getMessage(), toApiErrors(validation));
    }
    if (exception instanceof ApiGatewayException apiGatewayException) {
      throw apiGatewayException;
    }
    throw new BadGatewayException(exception.getMessage());
  }

  private static List<ValidationError> toApiErrors(ValidationException validation) {
    return validation.getErrors().stream()
        .map(error -> new ValidationError(error.detail(), error.pointer()))
        .toList();
  }
}
