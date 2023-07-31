package no.sikt.nva.scopus.conversion;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;
import no.sikt.nva.scopus.conversion.model.PublicationChannelResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class PublicationChannelConnection {

    public static final String PUBLICATION_CHANNELS = "publication-channels";
    public static final String JOURNAL = "journal";
    public static final String QUERY = "query";
    public static final String YEAR = "year";
    public static final String CONTENT_TYPE = "application/json";
    public static final int SINGLE_ITEM = 1;
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    private final AuthorizedBackendUriRetriever uriRetriever;

    public PublicationChannelConnection(AuthorizedBackendUriRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public Optional<URI> fetchJournal(String printIssn, String electronicIssn, String sourceTitle,
                                      Integer publicationYear) {
        return attempt(() -> Stream.of(printIssn, electronicIssn, sourceTitle)
                                 .map(item -> constructSearchJournalUri(item, publicationYear))
                                 .map(uri -> uriRetriever.getRawContent(uri, CONTENT_TYPE))
                                 .filter(Optional::isPresent)
                                 .map(Optional::get)
                                 .map(PublicationChannelConnection::toPublicationChannelResults)
                                 .filter(PublicationChannelConnection::containsSingleResult)
                                 .map(PublicationChannelConnection::getId)
                                 .findFirst()).orElse(failure -> Optional.<URI>empty());
    }

    private static URI getId(PublicationChannelResponse... results) {
        return results[0].getId();
    }

    private static boolean containsSingleResult(PublicationChannelResponse... results) {
        return results.length == SINGLE_ITEM;
    }

    private static PublicationChannelResponse[] toPublicationChannelResults(String response) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, PublicationChannelResponse[].class)).orElse(
            failure -> null);
    }

    private URI constructSearchJournalUri(String searchTerm, Integer year) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION_CHANNELS)
                   .addChild(JOURNAL)
                   .addQueryParameter(QUERY, searchTerm)
                   .addQueryParameter(YEAR, String.valueOf(year))
                   .getUri();
    }
}
