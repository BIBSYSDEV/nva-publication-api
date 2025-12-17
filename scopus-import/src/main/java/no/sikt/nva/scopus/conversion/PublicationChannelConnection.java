package no.sikt.nva.scopus.conversion;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.scopus.conversion.PublicationChannel.PUBLISHER;
import static no.sikt.nva.scopus.conversion.PublicationChannel.SERIAL_PUBLICATION;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import no.sikt.nva.scopus.conversion.model.PublicationChannelResponse;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationChannelConnection {

    public static final String PUBLICATION_CHANNELS_V2 = "publication-channels-v2";
    public static final String QUERY = "query";
    public static final String YEAR = "year";
    public static final String CONTENT_TYPE = "application/json";
    public static final int SINGLE_ITEM = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicationChannelConnection.class);
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    private final AuthorizedBackendUriRetriever uriRetriever;

    public PublicationChannelConnection(AuthorizedBackendUriRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public Optional<URI> fetchSerialPublication(String printIssn, String electronicIssn, String sourceTitle,
                                                Integer publicationYear) {
        LOGGER.info("Searching for serial publication with printIssn={}, electronicIssn={}, sourceTitle={}, year={}",
                    printIssn, electronicIssn, sourceTitle, publicationYear);
        var uriStream =
                Stream.of(printIssn, electronicIssn, sourceTitle)
                        .filter(Objects::nonNull)
                        .map(item -> constructSearchUri(SERIAL_PUBLICATION, item, publicationYear));
        return fetchPublicationChannelId(uriStream);
    }

    public Optional<URI> fetchPublisher(String publisherName, Integer publicationYear) {
        LOGGER.info("Searching for publisher with publisherName={}, year={}", publisherName, publicationYear);
        var uriToRetrieve = constructSearchUri(PUBLISHER, publisherName, publicationYear);
        return fetchPublicationChannelId(Stream.of(uriToRetrieve));
    }

    private static URI getId(PublicationChannelResponse results) {
        return results.getHits().getFirst().getId();
    }

    private static boolean containsSingleResult(PublicationChannelResponse response) {
        return response.getTotalHits() == SINGLE_ITEM;
    }

    private static PublicationChannelResponse toPublicationChannelResponse(String response) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, PublicationChannelResponse.class)).orElse(
            failure -> null);
    }

    private Optional<URI> fetchPublicationChannelId(Stream<URI> uriStream) {
        return uriStream.map(uri -> uriRetriever.fetchResponse(uri, CONTENT_TYPE))
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .map(this::handleResponse)
                   .map(PublicationChannelConnection::toPublicationChannelResponse)
                   .filter(Objects::nonNull)
                   .filter(PublicationChannelConnection::containsSingleResult)
                   .map(PublicationChannelConnection::getId)
                   .findFirst();
    }

    private String handleResponse(HttpResponse<String> response) {
        if (HTTP_OK != response.statusCode()) {
            LOGGER.error("Publication channels API responded with {} when searching for {}", response.statusCode(),
                         response.request().uri());
        }
        return response.body();
    }

    private URI constructSearchUri(PublicationChannel publicationChannel, String searchTerm, Integer year) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION_CHANNELS_V2)
                   .addChild(publicationChannel.getValue())
                   .addQueryParameter(QUERY, searchTerm)
                   .addQueryParameter(YEAR, String.valueOf(year))
                   .getUri();
    }
}
