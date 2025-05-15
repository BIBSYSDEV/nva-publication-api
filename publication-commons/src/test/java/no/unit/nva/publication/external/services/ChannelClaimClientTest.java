package no.unit.nva.publication.external.services;

import static no.unit.nva.model.testing.RandomUtils.randomBackendUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Optional;
import no.unit.nva.publication.external.services.ChannelClaimDto.ChannelClaim;
import no.unit.nva.publication.external.services.ChannelClaimDto.ChannelClaim.ChannelConstraint;
import no.unit.nva.publication.external.services.ChannelClaimDto.CustomerSummaryDto;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChannelClaimClientTest {

    protected static final String CONTENT_TYPE = "application/json";
    private ChannelClaimClient channelClaimClient;
    private UriRetriever uriRetriever;

    @BeforeEach
    void setUp() {
        this.uriRetriever = mock(UriRetriever.class);
        this.channelClaimClient = ChannelClaimClient.create(uriRetriever);
    }

    @Test
    void shouldReturnChannelClaimByIdWhenRequested() throws NotFoundException {
        var channelClaim = randomBackendUri("customer/channel-claim");
        var expectedChannelClaim = channelClaimWithId(channelClaim);

        var response = FakeHttpResponse.create(expectedChannelClaim.toJsonString(), 200);
        when(uriRetriever.fetchResponse(channelClaim, CONTENT_TYPE)).thenReturn(Optional.of(response));

        var actual = channelClaimClient.fetchChannelClaim(channelClaim);

        assertEquals(expectedChannelClaim, actual);
    }

    @Test
    void shouldThrowNotFoundWhenIdentityServiceRespondsWithNoFoundWhenFetchingChannelClaim() {
        var channelClaim = randomBackendUri("customer/channel-claim");

        var response = FakeHttpResponse.create(randomString(), 404);
        when(uriRetriever.fetchResponse(channelClaim, CONTENT_TYPE)).thenReturn(Optional.of(response));
        assertThrows(NotFoundException.class, () -> channelClaimClient.fetchChannelClaim(channelClaim));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenUnhandledExceptionWhenFetchingChannelClaim() {
        var channelClaim = randomUri();
        var request = HttpRequest.newBuilder().GET().uri(channelClaim).build();

        var response = FakeHttpResponse.create(randomString(), 502);
        when(uriRetriever.fetchResponse(channelClaim, CONTENT_TYPE)).thenReturn(Optional.of(response));
        assertThrows(RuntimeException.class, () -> channelClaimClient.fetchChannelClaim(channelClaim));
    }

    private ChannelClaimDto channelClaimWithId(URI channelClaim) {
        return new ChannelClaimDto(channelClaim, new CustomerSummaryDto(randomUri(), randomUri()),
                                   new ChannelClaim(channelClaim, new ChannelConstraint(randomString(), randomString(),
                                                                                        List.of(randomString(),
                                                                                                randomString()))));
    }
}