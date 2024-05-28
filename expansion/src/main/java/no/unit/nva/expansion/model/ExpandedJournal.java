package no.unit.nva.expansion.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.utils.RdfUtils.APPLICATION_JSON;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.publication.external.services.RawContentRetriever;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(ExpandedJournal.TYPE)
public class ExpandedJournal {

    public static final String TYPE = "Journal";
    private static final Logger logger = LoggerFactory.getLogger(ExpandedJournal.class);
    public URI id;

    public String name;

    @JacocoGenerated
    public ExpandedJournal() {

    }

    public ExpandedJournal(URI id, String name) {
        this.id = id;
        this.name = name;
    }

    public ExpandedJournal(Journal journal, RawContentRetriever rawContentRetriever) {
        this.id = journal.getId();
        this.name = expandJournalName(journal, rawContentRetriever).orElse(null);
    }

    @JacocoGenerated
    public URI getId() {
        return id;
    }

    @JacocoGenerated
    public String getName() {
        return name;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExpandedJournal that = (ExpandedJournal) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    private Optional<String> expandJournalName(Journal journal,
                                               RawContentRetriever rawContentRetriever) {
        return nonNull(journal) && nonNull(journal.getId())
                   ? expandJournalId(journal, rawContentRetriever)
                   : Optional.empty();
    }

    private Optional<String> expandJournalId(Journal journal, RawContentRetriever rawContentRetriever) {
        return rawContentRetriever.fetchResponse(journal.getId(), APPLICATION_JSON)
                   .map(this::extractNameFromResponse);
    }

    private String extractNameFromResponse(HttpResponse<String> response) {
        return response.statusCode() == HttpStatus.SC_OK
                   ? extractNameFromResponseBody(response.body())
                   : logFailedResponse(response);
    }

    private String logFailedResponse(HttpResponse<String> response) {
        logger.error("Not Ok response from channel registry: {}", response);
        return null;
    }

    private String extractNameFromResponseBody(String body) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readTree(body))
                   .map(bodyJson -> bodyJson.get("name").asText())
                   .orElse(this::ignoreFailure);
    }

    @JacocoGenerated
    private String ignoreFailure(Failure<String> fail) {
        logger.error("Failed to parse channel registry response: {}", fail);
        return null;
    }
}
