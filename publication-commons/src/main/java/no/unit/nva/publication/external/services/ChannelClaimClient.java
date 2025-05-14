package no.unit.nva.publication.external.services;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Failure;
import software.amazon.awssdk.http.HttpStatusCode;

public final class ChannelClaimClient {

    private final RawContentRetriever uriRetriever;

    private ChannelClaimClient(RawContentRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public static ChannelClaimClient create(RawContentRetriever uriRetriever) {
        return new ChannelClaimClient(uriRetriever);
    }

    public ChannelClaimDto fetchChannelClaim(URI channelClaimId) throws NotFoundException {
        return attempt(() -> uriRetriever.fetchResponse(channelClaimId, "application/json")).map(Optional::get)
                   .map(this::validateResponse)
                   .map(ChannelClaimClient::toChannelClaimDto)
                   .orElseThrow(this::handleFailure);
    }

    private static ChannelClaimDto toChannelClaimDto(HttpResponse<String> response) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(response.body(), ChannelClaimDto.class);
    }

    private HttpResponse<String> validateResponse(HttpResponse<String> response) throws NotFoundException {
        if (response.statusCode() == HttpStatusCode.NOT_FOUND) {
            throw new NotFoundException("Client not found");
        }

        if (response.statusCode() != HttpStatusCode.OK) {
            throw new IllegalStateException("Received " + response.statusCode() + " from identity service");
        }
        return response;
    }

    private NotFoundException handleFailure(Failure<?> responseFailure) {
        var exception = responseFailure.getException();
        if (exception instanceof NotFoundException notFoundException) {
            return notFoundException;
        }

        throw new RuntimeException("Something went wrong!" + exception.getMessage());
    }
}
