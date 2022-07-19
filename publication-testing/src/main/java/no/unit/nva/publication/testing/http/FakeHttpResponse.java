package no.unit.nva.publication.testing.http;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import javax.net.ssl.SSLSession;

public class FakeHttpResponse<T> implements HttpResponse<T> {
    
    private final HttpRequest inputRequest;
    private final T responseBody;
    
    public FakeHttpResponse(HttpRequest request, T responseBody) {
        this.inputRequest = request;
        this.responseBody = responseBody;
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
    
    @Override
    public T body() {
        return responseBody;
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
