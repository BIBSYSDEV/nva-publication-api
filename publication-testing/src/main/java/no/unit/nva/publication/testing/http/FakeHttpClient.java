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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class FakeHttpClient<T> extends HttpClient {
    
    private final List<FakeHttpResponse<T>> responseBodies;
    private final AtomicInteger callCounter;
    
    @SafeVarargs
    public FakeHttpClient(FakeHttpResponse<T>... responses) {
        super();
        this.responseBodies = new ArrayList<>();
        responseBodies.addAll(Arrays.asList(responses));
        this.callCounter = new AtomicInteger(0);
    }
    
    public FakeHttpClient<T> addResponse(FakeHttpResponse<T> response) {
        responseBodies.add(response);
        return this;
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
    
    @SuppressWarnings("unchecked")
    @Override
    public <S> HttpResponse<S> send(HttpRequest request, BodyHandler<S> responseBodyHandler) {
        return FakeHttpResponse.create(request, (FakeHttpResponse<S>) nextResponse());
    }
    
    @Override
    public <S> CompletableFuture<HttpResponse<S>> sendAsync(HttpRequest request, BodyHandler<S> responseBodyHandler) {
        return null;
    }
    
    @Override
    public <S> CompletableFuture<HttpResponse<S>> sendAsync(HttpRequest request, BodyHandler<S> responseBodyHandler,
                                                            PushPromiseHandler<S> pushPromiseHandler) {
        return null;
    }
    
    private FakeHttpResponse<T> nextResponse() {
        int nextResponseIndex = Math.min(callCounter.get(), responseBodies.size() - 1);
        callCounter.incrementAndGet();
        return responseBodies.get(nextResponseIndex);
    }
}
