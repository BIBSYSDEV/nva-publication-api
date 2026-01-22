package no.unit.nva.expansion;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.net.http.HttpResponse;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedJournal;
import no.unit.nva.model.contexttypes.Journal;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalExpansionServiceImpl implements JournalExpansionService {

    private final RawContentRetriever rawContentRetriever;
    private static final Logger logger = LoggerFactory.getLogger(JournalExpansionServiceImpl.class);


    public JournalExpansionServiceImpl(RawContentRetriever rawContentRetriever) {
        this.rawContentRetriever = rawContentRetriever;
    }

    @Override
    public ExpandedJournal expandJournal(Journal journal) {
        return nonNull(journal) && nonNull(journal.getId())
                   ? new ExpandedJournal(journal.getId(), expandJournalName(journal))
                   : null;
    }

    private String expandJournalName(Journal journal) {
        return rawContentRetriever.fetchResponse(journal.getId(), MediaType.JSON_UTF_8.toString())
                   .map(this::extractNameFromResponse)
                   .orElse(null);
    }

    private String extractNameFromResponse(HttpResponse<String> response) {
        return response.statusCode() == HTTP_OK
                   ? extractNameFromResponseBody(response.body())
                   : logFailedResponse(response);
    }

    private String extractNameFromResponseBody(String body) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readTree(body))
                   .map(bodyJson -> bodyJson.get("name").asText())
                   .orElse(this::ignoreFailure);
    }

    private String logFailedResponse(HttpResponse<String> response) {
        logger.error("Not Ok response from channel registry: {}", response);
        return null;
    }

    private String ignoreFailure(Failure<String> fail) {
        logger.error("Failed to parse channel registry response: {}", fail);
        return null;
    }
}
