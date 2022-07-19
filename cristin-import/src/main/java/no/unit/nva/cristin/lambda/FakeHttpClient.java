package no.unit.nva.cristin.lambda;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import nva.commons.core.JacocoGenerated;

/**
 * This Fake client is returning a hardcoded Institution ID when we are importing entries from Cristin. The reason for
 * that is that entries imported from Cristin have a hardcoded owner (someone@unit.no) for now and there is no point in
 * sending millions of queries to Person proxy to get an empty answer back.
 */
@JacocoGenerated
public class FakeHttpClient extends HttpClient {
    
    @JacocoGenerated
    @Override
    public Optional<CookieHandler> cookieHandler() {
        return Optional.empty();
    }
    
    @JacocoGenerated
    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.empty();
    }
    
    @JacocoGenerated
    @Override
    public Redirect followRedirects() {
        return null;
    }
    
    @JacocoGenerated
    @Override
    public Optional<ProxySelector> proxy() {
        return Optional.empty();
    }
    
    @JacocoGenerated
    @Override
    public SSLContext sslContext() {
        return null;
    }
    
    @JacocoGenerated
    @Override
    public SSLParameters sslParameters() {
        return null;
    }
    
    @JacocoGenerated
    @Override
    public Optional<Authenticator> authenticator() {
        return Optional.empty();
    }
    
    @JacocoGenerated
    @Override
    public Version version() {
        return null;
    }
    
    @JacocoGenerated
    @Override
    public Optional<Executor> executor() {
        return Optional.empty();
    }
    
    @SuppressWarnings("unchecked")
    @JacocoGenerated
    @Override
    public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler) {
        return new FakeHttpResponse(request);
    }
    
    @JacocoGenerated
    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> responseBodyHandler) {
        return null;
    }
    
    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> responseBodyHandler,
                                                            PushPromiseHandler<T> pushPromiseHandler) {
        return null;
    }
}
