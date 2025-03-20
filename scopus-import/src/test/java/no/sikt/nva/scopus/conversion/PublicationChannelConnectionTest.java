package no.sikt.nva.scopus.conversion;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.scopus.conversion.model.PublicationChannelResponse;
import no.sikt.nva.scopus.conversion.model.PublicationChannelResponse.PublicationChannelHit;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicationChannelConnectionTest {

    private PublicationChannelConnection publicationChannelConnection;
    private AuthorizedBackendUriRetriever uriRetriever;

    @BeforeEach
    void setup() {
        uriRetriever = mock(AuthorizedBackendUriRetriever.class);
        publicationChannelConnection = new PublicationChannelConnection(uriRetriever);
    }

    @Test
    void shouldDoSingleApiCallWhenSearchingForChannelAndReceivingResponseWithSingleHitOnFirstApiCall() {
        when(uriRetriever.fetchResponse(any(), any())).thenReturn(
            Optional.of(FakeHttpResponse.create(responseWithSingleHit(), HTTP_OK)));

        publicationChannelConnection.fetchSerialPublication(randomString(), randomString(), randomString(),
                                                            randomInteger());

        verify(uriRetriever, times(1)).fetchResponse(any(), any());
    }

    @Test
    void shouldLogWhenChannelRegisterApiIsFailing() {
        var request = HttpRequest.newBuilder().GET().uri(randomUri()).build();
        var response = FakeHttpResponse.create(randomString(), HTTP_BAD_GATEWAY);
        when(uriRetriever.fetchResponse(any(), any())).thenReturn(
            Optional.of(FakeHttpResponse.create(request, response)));

        var appender = LogUtils.getTestingAppender(PublicationChannelConnection.class);
        publicationChannelConnection.fetchSerialPublication(randomString(), randomString(), randomString(),
                                                            randomInteger());

        assertTrue(appender.getMessages().contains("Publication channels API responded with 502"));
    }

    private static String responseWithSingleHit() {
        return new PublicationChannelResponse(1, List.of(new PublicationChannelHit(randomUri()))).toJsonString();
    }
}