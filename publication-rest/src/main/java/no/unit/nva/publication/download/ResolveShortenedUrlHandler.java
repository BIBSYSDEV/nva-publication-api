package no.unit.nva.publication.download;

import static org.apache.http.HttpHeaders.LOCATION;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;
import no.unit.nva.publication.services.UriResolver;
import no.unit.nva.publication.services.UriResolverImpl;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolveShortenedUrlHandler extends ApiGatewayHandler<Void, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResolveShortenedUrlHandler.class);
    private final UriResolver uriResolver;

    @JacocoGenerated
    public ResolveShortenedUrlHandler() {
        this(new Environment(), UriResolverImpl.createDefault(), HttpClient.newHttpClient());
    }

    public ResolveShortenedUrlHandler(Environment environment, UriResolver uriResolver, HttpClient httpClient) {
        super(Void.class, environment, httpClient);
        this.uriResolver = uriResolver;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Void processInput(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        LOGGER.info(requestInfo.getRequestUri().toString());
        var shortenedUri = requestInfo.getRequestUri();
        var longUri = uriResolver.resolve(shortenedUri);
        addAdditionalHeaders(() -> addLocationHeader(longUri));
        return null;
    }

    private Map<String, String> addLocationHeader(URI longUri) {
        Map<String, String> headers = new HashMap<>();
        headers.put(LOCATION, longUri.toString());
        return headers;
    }

    @JacocoGenerated
    @Override
    protected Integer getSuccessStatusCode(Void unused, Void o) {
        return 301;
    }
}
