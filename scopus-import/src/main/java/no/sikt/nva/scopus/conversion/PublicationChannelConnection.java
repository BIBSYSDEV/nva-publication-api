package no.sikt.nva.scopus.conversion;

import static no.sikt.nva.scopus.conversion.PublicationChannel.PUBLISHER;
import static no.sikt.nva.scopus.conversion.PublicationChannel.SERIAL_PUBLICATION;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import no.sikt.nva.scopus.conversion.model.PublicationChannelResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class PublicationChannelConnection {

    public static final String PUBLICATION_CHANNELS_V2 = "publication-channels-v2";
    public static final String QUERY = "query";
    public static final String YEAR = "year";
    public static final String CONTENT_TYPE = "application/json";
    public static final int SINGLE_ITEM = 1;
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    private final AuthorizedBackendUriRetriever uriRetriever;

    public PublicationChannelConnection(AuthorizedBackendUriRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public Optional<URI> fetchSerialPublication(String printIssn, String electronicIssn, String sourceTitle,
                                                Integer publicationYear) {
        var uriStream = Stream.of(printIssn, electronicIssn, sourceTitle)
                            .map(item -> constructSearchUri(SERIAL_PUBLICATION, item, publicationYear));
        return fetchPublicationChannelId(uriStream);
    }

    public Optional<URI> fetchPublisher(String publisherName, Integer publicationYer) {
        var uriToRetrieve = constructSearchUri(PUBLISHER, publisherName, publicationYer);
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
        return uriStream.map(uri -> uriRetriever.getRawContent(uri, CONTENT_TYPE))
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .map(PublicationChannelConnection::toPublicationChannelResponse)
                   .filter(Objects::nonNull)
                   .filter(PublicationChannelConnection::containsSingleResult)
                   .map(PublicationChannelConnection::getId)
                   .findFirst();
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
