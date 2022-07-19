package no.unit.nva.publication.testing.http;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class FakeHttpClient<T> extends HttpClient {
    
    private final List<T> responseBodies;
    private final AtomicInteger callCounter;
    
    public FakeHttpClient(T... responseBody) {
        super();
        this.responseBodies = Arrays.asList(responseBody);
        this.callCounter = new AtomicInteger(0);
    }
    
    public AtomicInteger getCallCounter() {
        return callCounter;
    }
    
    @Override
    public Optional<CookieHandler> cookieHandler() {
        return Optional.empty();
    }
    
    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.empty();
    }
    
    @Override
    public Redirect followRedirects() {
        return null;
    }
    
    @Override
    public Optional<ProxySelector> proxy() {
        return Optional.empty();
    }
    
    @Override
    public SSLContext sslContext() {
        return null;
    }
    
    @Override
    public SSLParameters sslParameters() {
        return null;
    }
    
    @Override
    public Optional<Authenticator> authenticator() {
        return Optional.empty();
    }
    
    @Override
    public Version version() {
        return null;
    }
    
    @Override
    public Optional<Executor> executor() {
        return Optional.empty();
    }
    
    @Override
    public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler) {
        var responseBody = nextResponse();
        return new FakeHttpResponse(request, responseBody);
    }
    
    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> responseBodyHandler) {
        return null;
    }
    
    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> responseBodyHandler,
                                                            PushPromiseHandler<T> pushPromiseHandler) {
        return null;
    }
    
    private T nextResponse() {
        int nextResponseIndex = Math.min(callCounter.get(), responseBodies.size() - 1);
        callCounter.incrementAndGet();
        return responseBodies.get(nextResponseIndex);
    }
}
