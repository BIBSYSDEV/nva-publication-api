package no.unit.nva.expansion;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import java.net.http.HttpResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedJournal;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.publication.external.services.RawContentRetriever;
import nva.commons.core.attempt.Failure;
import org.apache.http.HttpStatus;
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
        return rawContentRetriever.fetchResponse(journal.getId(), APPLICATION_JSON.getMimeType())
                   .map(this::extractNameFromResponse)
                   .orElse(null);
    }

    private String extractNameFromResponse(HttpResponse<String> response) {
        return response.statusCode() == HttpStatus.SC_OK
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
