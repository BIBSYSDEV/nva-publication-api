package no.unit.nva.publication.testing.http;

import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import javax.net.ssl.SSLSession;

public final class FakeHttpResponse<T> implements HttpResponse<T> {
    
    private final HttpRequest inputRequest;
    private final T responseBody;
    private final int status;
    
    private FakeHttpResponse(HttpRequest request, T responseBody, int statusCode) {
        this.inputRequest = request;
        this.responseBody = responseBody;
        this.status = statusCode;
    }
    
    public static <T> FakeHttpResponse<T> create(T responseBody, int statusCode) {
        return new FakeHttpResponse<>(null, responseBody, statusCode);
    }
    
    public static <T> FakeHttpResponse<T> create(HttpRequest request, FakeHttpResponse<T> response) {
        return new FakeHttpResponse<>(request, response.responseBody, response.status);
    }
    
    @Override
    public int statusCode() {
        return status;
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
