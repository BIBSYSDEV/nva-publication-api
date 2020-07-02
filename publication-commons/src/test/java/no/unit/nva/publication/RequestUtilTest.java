package no.unit.nva.publication;

import static nva.commons.utils.JsonUtils.objectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.publication.exception.InputException;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.RequestInfo;
import org.junit.jupiter.api.Test;

public class RequestUtilTest {

    public static final String VALUE = "value";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";

    @Test
    public void canGetIdentifierFromRequest() throws ApiGatewayException {
        UUID uuid = UUID.randomUUID();
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setPathParameters(Map.of(RequestUtil.IDENTIFIER, uuid.toString()));

        UUID identifier = RequestUtil.getIdentifier(requestInfo);

        assertEquals(uuid, identifier);
    }

    @Test
    public void getIdentifierOnInvalidRequestThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(InputException.class, () -> RequestUtil.getIdentifier(requestInfo));
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

    @Test
    public void getPageSizeRequestInvalidRangeThrowsException() throws Exception {
        RequestInfo requestInfo = new RequestInfo();

        Map<String, String> queryParameters = Map.of(RequestUtil.PAGESIZE,"-1");
        requestInfo.setQueryParameters(queryParameters);

        assertThrows(InputException.class, () -> RequestUtil.getPageSize(requestInfo));
    }

    @Test
    public void getPageSizeRequestInvalidValueThrowsException() throws Exception {
        RequestInfo requestInfo = new RequestInfo();

        Map<String, String> queryParameters = Map.of(RequestUtil.PAGESIZE,"-abc");
        requestInfo.setQueryParameters(queryParameters);

        assertThrows(InputException.class, () -> RequestUtil.getPageSize(requestInfo));
    }

    @Test
    public void getPageSizeRequestEmptyValueReturnsDefault() throws Exception {
        RequestInfo requestInfo = new RequestInfo();

        Map<String, String> queryParameters = Map.of(RequestUtil.PAGESIZE,"");
        requestInfo.setQueryParameters(queryParameters);

        assertEquals(RequestUtil.DEFAULT_PAGESIZE, RequestUtil.getPageSize(requestInfo));
    }

    @Test
    public void getPageSizeRequestOKValue() throws Exception {
        RequestInfo requestInfo = new RequestInfo();

        Map<String, String> queryParameters = Map.of(RequestUtil.PAGESIZE,"3");
        requestInfo.setQueryParameters(queryParameters);

        assertEquals(3, RequestUtil.getPageSize(requestInfo));
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
        Map<String, Map<String, Map<String, String>>> map = Map.of(
            AUTHORIZER, Map.of(
                CLAIMS, Map.of(
                    key, value
                )
            )
        );
        return objectMapper.readTree(objectMapper.writeValueAsString(map));
    }
}
