package no.unit.nva.publication;

import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.Test;

public class RequestUtilTest {
    
    public static final String VALUE = "value";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String SOME_USER = "some@user";
    public static final String SOME_ORG = "https://some.org.example.org";
    
    @Test
    public void canGetIdentifierFromRequest() throws ApiGatewayException {
        SortableIdentifier uuid = SortableIdentifier.next();
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setPathParameters(Map.of(RequestUtil.IDENTIFIER, uuid.toString()));
        
        SortableIdentifier identifier = RequestUtil.getIdentifier(requestInfo);
        
        assertEquals(uuid, identifier);
    }
    
    @Test
    public void getIdentifierOnInvalidRequestThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(BadRequestException.class, () -> RequestUtil.getIdentifier(requestInfo));
    }
    
    @Test
    public void canGetCustomerIdFromRequest() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(RequestUtil.CUSTOM_CUSTOMER_ID, VALUE));
        
        URI customerId = RequestUtil.getCustomerId(requestInfo);
        
        assertEquals(URI.create(VALUE), customerId);
    }
    
    @Test
    public void getCustomerIdOnMissingNodeRequestThrowsException() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextWithMissingNode());
        
        assertThrows(BadRequestException.class, () -> RequestUtil.getCustomerId(requestInfo));
    }
    
    @Test
    public void getCustomerIdOnInvalidRequestThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(BadRequestException.class, () -> RequestUtil.getCustomerId(requestInfo));
    }
    
    @Test
    public void canGetOwnerFromRequest() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(RequestUtil.CUSTOM_FEIDE_ID, VALUE));
        
        String owner = RequestUtil.getOwner(requestInfo);
        
        assertEquals(VALUE, owner);
    }
    
    @Test
    public void extractUserInstanceReturnsUserInstanceWithOwnerIdentifierAndOrgId() throws JsonProcessingException {
        RequestInfo requestInfo = new RequestInfo();
        var claims = Map.of(
            RequestUtil.CUSTOM_CUSTOMER_ID, SOME_ORG,
            RequestUtil.CUSTOM_FEIDE_ID, SOME_USER
        );
        requestInfo.setRequestContext(getRequestContextForClaim(claims));
        UserInstance userInstance = RequestUtil.extractUserInstance(requestInfo);
        assertThat(userInstance.getUserIdentifier(), is(equalTo(SOME_USER)));
        assertThat(userInstance.getOrganizationUri(), is(equalTo(URI.create(SOME_ORG))));
    }
    
    @Test
    public void getOwnerOnMissingNodeRequestThrowsException() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextWithMissingNode());
        
        assertThrows(BadRequestException.class, () -> RequestUtil.getOwner(requestInfo));
    }
    
    @Test
    public void getOwnerOnInvalidRequestThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(BadRequestException.class, () -> RequestUtil.getOwner(requestInfo));
    }
    
    @Test
    public void getPageSizeRequestInvalidRangeThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        
        Map<String, String> queryParameters = Map.of(RequestUtil.PAGESIZE, "-1");
        requestInfo.setQueryParameters(queryParameters);
        
        assertThrows(BadRequestException.class, () -> RequestUtil.getPageSize(requestInfo));
    }
    
    @Test
    public void getPageSizeRequestInvalidValueThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        
        Map<String, String> queryParameters = Map.of(RequestUtil.PAGESIZE, "-abc");
        requestInfo.setQueryParameters(queryParameters);
        
        assertThrows(BadRequestException.class, () -> RequestUtil.getPageSize(requestInfo));
    }
    
    @Test
    public void getPageSizeRequestEmptyValueReturnsDefault() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        
        Map<String, String> queryParameters = Map.of(RequestUtil.PAGESIZE, "");
        requestInfo.setQueryParameters(queryParameters);
        
        assertEquals(RequestUtil.DEFAULT_PAGESIZE, RequestUtil.getPageSize(requestInfo));
    }
    
    @Test
    public void getPageSizeRequestOKValue() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        
        Map<String, String> queryParameters = Map.of(RequestUtil.PAGESIZE, "3");
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
        return getRequestContextForClaim(Map.of(key, value));
    }
    
    private JsonNode getRequestContextForClaim(Map<String, String> claimKeyValuePairs) throws JsonProcessingException {
        Map<String, Map<String, Map<String, String>>> map = Map.of(
            AUTHORIZER, Map.of(
                CLAIMS, claimKeyValuePairs
            )
        );
        return objectMapper.readTree(objectMapper.writeValueAsString(map));
    }
}
