package no.unit.nva.publication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.publication.exception.InputException;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.RequestInfo;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RequestUtilTest {

    public static final String VALUE = "value";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";

    private ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper;

    @Test
    public void canGetIdentifierFromRequest() throws ApiGatewayException {
        UUID uuid = UUID.randomUUID();
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setPathParameters(Map.of(RequestUtil.IDENTIFIER, uuid.toString()));

        UUID identifier = new RequestUtil().getIdentifier(requestInfo);

        assertEquals(uuid, identifier);
    }

    @Test
    public void getIdentifierOnInvalidRequestThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(InputException.class, () -> RequestUtil.getIdentifier(requestInfo));
    }

    @Test
    public void canGetAuthorizationFromRequest() throws ApiGatewayException {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setHeaders(Map.of(HttpHeaders.AUTHORIZATION, VALUE));

        String authorization = RequestUtil.getAuthorization(requestInfo);

        assertEquals(VALUE, authorization);
    }

    @Test
    public void getAuthorizationOnInvalidRequestThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(InputException.class, () -> RequestUtil.getAuthorization(requestInfo));
    }

    @Test
    public void canGetOrgNumberFromRequest() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(RequestUtil.CUSTOM_ORG_NUMBER, VALUE));

        String orgNumber = RequestUtil.getOrgNumber(requestInfo);

        assertEquals(VALUE, orgNumber);
    }

    @Test
    public void getOrgNumberOnMissingNodeRequestThrowsException() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextWithMissingNode());

        assertThrows(InputException.class, () -> RequestUtil.getOrgNumber(requestInfo));

    }

    @Test
    public void getOrgNumberOnInvalidRequestThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(InputException.class, () -> RequestUtil.getOrgNumber(requestInfo));
    }


    @Test
    public void canGetOwnerFromRequest() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(RequestUtil.CUSTOM_FEIDE_ID, VALUE));

        String owner = RequestUtil.getOwner(requestInfo);

        assertEquals(VALUE, owner);
    }

    @Test
    public void getOwnerOnMissingNodeRequestThrowsException() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextWithMissingNode());

        assertThrows(InputException.class, () -> RequestUtil.getOwner(requestInfo));
    }

    @Test
    public void getOwnerOnInvalidRequestThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(InputException.class, () -> RequestUtil.getOwner(requestInfo));
    }

    private JsonNode getRequestContextWithMissingNode() throws JsonProcessingException {
        Map<String, Map<String, JsonNode>> map = Map.of(
                AUTHORIZER, Map.of(
                        CLAIMS, objectMapper.createObjectNode().nullNode()
                )
        );
        return objectMapper.readTree(objectMapper.writeValueAsString(map));
    }

    private JsonNode getRequestContextForClaim(String key, String value) throws JsonProcessingException {
        Map<String, Map<String, Map<String,String>>> map = Map.of(
            AUTHORIZER, Map.of(
                CLAIMS, Map.of(
                        key, value
                )
            )
        );
        return objectMapper.readTree(objectMapper.writeValueAsString(map));
    }

}
