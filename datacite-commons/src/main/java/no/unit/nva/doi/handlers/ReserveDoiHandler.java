package no.unit.nva.doi.handlers;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.doi.model.ReserveDoiRequest;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class ReserveDoiHandler extends ApiGatewayHandler<Void, DoiResponse> {

    public static final String NOT_DRAFT_STATUS_ERROR_MESSAGE = "Operation is not allowed, publication is not a draft";
    public static final String DOI_ALREADY_EXISTS_ERROR_MESSAGE = "Operation is not allowed, publication already has "
                                                                  + "doi";
    public static final String UNSUPPORTED_ROLE_ERROR_MESSAGE = "Only owner can reserve a doi";
    public static final String BAD_RESPONSE_ERROR_MESSAGE = "Bad response from DataCite";
    protected static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String DOI = "doi";
    public static final String RESERVE = "reserve";
    private final ResourceService resourceService;
    private final HttpClient httpClient;
    private Publication publication;

    @JacocoGenerated
    public ReserveDoiHandler() {
        this(ResourceService.defaultService(), getDefaultHttpClient(),
             new Environment());
    }

    public ReserveDoiHandler(ResourceService resourceService,
                             HttpClient httpClient, Environment environment) {
        super(Void.class, environment);
        this.resourceService = resourceService;
        this.httpClient = httpClient;
    }

    public DoiResponse convertResponseToDoi(HttpResponse<String> response) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(response.body(), DoiResponse.class);
    }

    @Override
    protected DoiResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validateReserveDoiRequest(requestInfo);
        return attempt(this::generateDoi)
                   .orElseThrow(failure -> new BadGatewayException(BAD_RESPONSE_ERROR_MESSAGE));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, DoiResponse output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private static boolean userIsNotOwnerOfPublication(RequestInfo requestInfo, Publication publication)
        throws ApiGatewayException {
        return !RequestUtil.getOwner(requestInfo).equals(publication.getResourceOwner().getOwner());
    }

    private static boolean isNotADraft(Publication publication) {
        return !PublicationStatus.DRAFT.equals(publication.getStatus());
    }

    @JacocoGenerated
    private static HttpClient getDefaultHttpClient() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    }

    private DoiResponse generateDoi() {
        return attempt(this::constructUri)
                   .map(this::sendRequest)
                   .map(this::convertResponseToDoi)
                   .map(this::updatePublication)
                   .orElseThrow();
    }

    private DoiResponse updatePublication(DoiResponse doiResponse) {
        var updatedPublication = publication.copy()
                                     .withDoi(doiResponse.getDoi())
                                     .build();
        resourceService.updatePublication(updatedPublication);
        return doiResponse;
    }

    private HttpResponse<String> sendRequest(URI uri) throws IOException, InterruptedException {
        var requestBody = new ReserveDoiRequest(publication.getPublisher().getId());
        var request = HttpRequest.newBuilder()
                          .POST(BodyPublishers.ofString(JsonUtils.dtoObjectMapper.writeValueAsString(requestBody)))
                          .uri(uri)
                          .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI constructUri() {
        return UriWrapper.fromUri(environment.readEnv(API_HOST))
                   .addChild(RESERVE)
                   .addChild(DOI)
                   .addChild(publication.getIdentifier().toString())
                   .getUri();
    }

    private void validateReserveDoiRequest(RequestInfo requestInfo) throws ApiGatewayException {
        SortableIdentifier identifier = RequestUtil.getIdentifier(requestInfo);
        publication = resourceService.getPublicationByIdentifier(identifier);
        if (alreadyHasDoi(publication)) {
            throw new BadMethodException(DOI_ALREADY_EXISTS_ERROR_MESSAGE);
        }
        if (isNotADraft(publication)) {
            throw new BadMethodException(NOT_DRAFT_STATUS_ERROR_MESSAGE);
        }
        if (userIsNotOwnerOfPublication(requestInfo, publication)) {
            throw new UnauthorizedException(UNSUPPORTED_ROLE_ERROR_MESSAGE);
        }
    }

    private boolean alreadyHasDoi(Publication publication) {
        return nonNull(publication.getDoi());
    }
}
