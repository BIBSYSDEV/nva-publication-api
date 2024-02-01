package no.unit.nva.publication.ticket.utils;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RequestUtilsTest {

    @Test
    void shouldReturnFalseWhenCheckingAuthorizationForNullTicket() throws UnauthorizedException {
        Assertions.assertFalse(RequestUtils.fromRequestInfo(mockedRequestInfo()).isAuthorizedToManage(null));
    }

    @Test
    void shouldThrowIllegalArgumentWhenExtractingMissingPathParamAsIdentifier() {
        assertThrows(IllegalArgumentException.class,
                     () -> RequestUtils.fromRequestInfo(mockedRequestInfo()).pathParameterAsIdentifier(randomString()));
    }

    private static RequestInfo mockedRequestInfo() throws UnauthorizedException {
        var requestInfo = mock(RequestInfo.class);
        when(requestInfo.getCurrentCustomer()).thenReturn(randomUri());
        when(requestInfo.getUserName()).thenReturn(randomString());
        when(requestInfo.getPathParameters()).thenReturn(Map.of());
        when(requestInfo.getAccessRights()).thenReturn(List.of());
        return requestInfo;
    }
}
