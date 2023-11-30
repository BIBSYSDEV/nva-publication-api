package no.sikt.nva.scopus.conversion;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.http.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NvaCustomerConnectionTest {

    private AuthorizedBackendUriRetriever uriRetriever;
    private NvaCustomerConnection nvaCustomerConnection;

    @BeforeEach
    void setUp() {
        uriRetriever = mock(AuthorizedBackendUriRetriever.class);
        nvaCustomerConnection = new NvaCustomerConnection(uriRetriever);
    }

    @Test
    void shouldReturnTrueWhenFetchingCustomerByCristinIdReturnsOk() {
        mockResponseWithStatusCode(200);

        assertTrue(nvaCustomerConnection.isNvaCustomer(cristinOrgWithId(randomUri())));
    }

    @Test
    void shouldReturnFalseWhenFetchingCustomerByCristinIdReturnsNotOk() {
        mockResponseWithStatusCode(502);

        assertFalse(nvaCustomerConnection.isNvaCustomer(cristinOrgWithId(randomUri())));
    }

    @Test
    void shouldReturnFalseWhenCristinOrgIdIsNull() {
        assertFalse(nvaCustomerConnection.isNvaCustomer(cristinOrgWithId(null)));
    }

    @Test
    void shouldReturnFalseWhenEmptyResponseFromCustomerApi() {
        when(uriRetriever.fetchResponse(any(), any())).thenReturn(Optional.empty());

        assertFalse(nvaCustomerConnection.isNvaCustomer(cristinOrgWithId(randomUri())));
    }

    private static CristinOrganization cristinOrgWithId(URI id) {
        return new CristinOrganization(id, null, null, null, null, null);
    }

    private void mockResponseWithStatusCode(int statusCode) {
        when(uriRetriever.fetchResponse(any(), any())).thenReturn(
            Optional.of(FakeHttpResponse.create(null, statusCode)));
    }
}
