package no.unit.nva.cristin.lambda;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;

@JacocoGenerated
public class FakeHttpResponse<T> implements HttpResponse<T> {
    
    private final HttpRequest inputRequest;
    
    public FakeHttpResponse(HttpRequest request) {
        this.inputRequest = request;
    }
    
    @Override
    public int statusCode() {
        return HttpURLConnection.HTTP_OK;
    }
    
    @Override
    public HttpRequest request() {
        return this.inputRequest;
    }
    
    @Override
    public Optional<HttpResponse<T>> previousResponse() {
        return Optional.empty();
    }
    
    @Override
    public HttpHeaders headers() {
        return null;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public T body() {
        return (T) IoUtils.stringFromResources(Path.of("fake_person_api_response.json"));
    }
    
    @Override
    public Optional<SSLSession> sslSession() {
        return Optional.empty();
    }
    
    @Override
    public URI uri() {
        return null;
    }
    
    @Override
    public Version version() {
        return null;
    }
}
