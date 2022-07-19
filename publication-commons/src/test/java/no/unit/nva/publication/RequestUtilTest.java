package no.unit.nva.publication;

import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.exception.BadRequestException;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;

class RequestUtilTest {
    
    public static final String VALUE = "value";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    
    public static final String INJECT_NVA_USERNAME_CLAIM = "custom:nvaUsername";
    
    @Test
    void canGetIdentifierFromRequest() throws ApiGatewayException {
        SortableIdentifier uuid = SortableIdentifier.next();
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setPathParameters(Map.of(RequestUtil.PUBLICATION_IDENTIFIER, uuid.toString()));
        
        SortableIdentifier identifier = RequestUtil.getIdentifier(requestInfo);
        
        assertEquals(uuid, identifier);
    }
    
    @Test
    void getIdentifierOnInvalidRequestThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(BadRequestException.class, () -> RequestUtil.getIdentifier(requestInfo));
    }
    
    @Test
    void canGetOwnerFromRequest() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(INJECT_NVA_USERNAME_CLAIM, VALUE));
        
        String owner = RequestUtil.getOwner(requestInfo);
        
        assertEquals(VALUE, owner);
    }
    
    @Test
    void getOwnerThrowsUnauthorizedExceptionWhenOwnerCannotBeRetrieved() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(UnauthorizedException.class, () -> RequestUtil.getOwner(requestInfo));
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
        return dtoObjectMapper.readTree(dtoObjectMapper.writeValueAsString(map));
    }
}
