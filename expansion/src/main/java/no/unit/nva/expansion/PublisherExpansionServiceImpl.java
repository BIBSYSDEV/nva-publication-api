package no.unit.nva.expansion;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.net.http.HttpResponse;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedPublisher;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublisherExpansionServiceImpl implements PublisherExpansionService {

    private static final Logger logger = LoggerFactory.getLogger(PublisherExpansionServiceImpl.class);

    private final RawContentRetriever rawContentRetriever;

    public PublisherExpansionServiceImpl(RawContentRetriever rawContentRetriever) {
        this.rawContentRetriever = rawContentRetriever;
    }

    @Override
    public ExpandedPublisher createExpandedPublisher(PublishingHouse publishingHouse) {
        if (publishingHouse instanceof Publisher publisher) {
            return new ExpandedPublisher(publisher.getId(), expandName(publisher));
        }
        if (publishingHouse instanceof UnconfirmedPublisher publisher) {
            return new ExpandedPublisher(null, publisher.getName());
        }
        return null;
    }

    private String expandName(Publisher publisher) {
        if (isNull(publisher.getId())) {
            return null;
        }
        return rawContentRetriever.fetchResponse(publisher.getId(), MediaType.JSON_UTF_8.toString())
                   .map(this::extractNameFromResponse)
                   .orElse(null);
    }

    private String extractNameFromResponse(HttpResponse<String> response) {
        return response.statusCode() == HTTP_OK
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

    private String ignoreFailure(Failure<String> fail) {
        logger.error("Failed to parse channel registry response: {}", fail);
        return null;
    }
}
