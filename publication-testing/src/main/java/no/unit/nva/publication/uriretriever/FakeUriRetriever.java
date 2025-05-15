package no.unit.nva.publication.uriretriever;

import static java.util.Objects.nonNull;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import com.google.common.net.MediaType;
import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import no.unit.nva.publication.external.services.RawContentRetriever;

public final class FakeUriRetriever implements RawContentRetriever {

    private final List<HttpResponse<String>> responses;

    private FakeUriRetriever() {
        this.responses = new ArrayList<>();
    }

    public static FakeUriRetriever newInstance() {
        return new FakeUriRetriever();
    }

    /**
     * This method ensures that we only ever return the last registered response. It ignores media type and status code.
     *
     * @param response The response to return.
     */
    private void add(HttpResponse<String> response) {
        responses.stream().filter(i -> i.uri().equals(response.uri())).findFirst().ifPresent(responses::remove);
        responses.add(response);
    }

    @Override
    public Optional<String> getRawContent(URI uri,
                                          String mediaType) {
        var returnValue = responses.stream()
                   .filter(response -> matchUri(uri, mediaType, response))
                   .map(HttpResponse::body)
                   .findFirst();
        if (returnValue.isEmpty()) {
            System.out.println("(getRawContent fake) Failed to find matching response for " + uri);
        }
        return returnValue;
    }

    @SuppressWarnings({"PMD.UnusedAssignment", "PMD.UnusedFormalParameter"})
    private static boolean matchesMedia(String mediaType, HttpResponse<String> response) {
        // TODO: For now, we don't match anything. We should when we design the interface properly.

        return true;
    }

    private static boolean matchUri(URI uri,
                                    String mediaType,
                                    HttpResponse<String> response) {

        return response.uri().equals(uri) && matchesMedia(mediaType, response);
    }

    @Override
    public Optional<HttpResponse<String>> fetchResponse(URI uri,
                                                        String mediaType) {
        var returnValue = responses.stream()
                   .filter(response -> matchUri(uri, mediaType, response))
                   .findFirst();
        if (returnValue.isEmpty()) {
            System.out.println("(fetchResponse fake) Failed to find matching response for " + uri);
        }
        return returnValue;
    }

    public FakeUriRetriever registerResponse(URI uri,
                                             int statusCode,
                                             MediaType mediaType,
                                             String body) {
        if (nonNull(uri)) {
            add(new FakeHttpResponse(uri, statusCode, mediaType, body));
        }
        return this;
    }

    public record FakeHttpResponse(URI uri,
                                   int statusCode,
                                   MediaType mediaType,
                                   String body) implements HttpResponse<String> {

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(CONTENT_TYPE, List.of(mediaType.toString())), (a, b) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return null;
        }
    }
}
