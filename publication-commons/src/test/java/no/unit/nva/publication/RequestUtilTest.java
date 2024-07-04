package no.unit.nva.publication;

import static java.util.UUID.randomUUID;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.publication.RequestUtil.FILE_IDENTIFIER;
import static no.unit.nva.publication.RequestUtil.IMPORT_CANDIDATE_IDENTIFIER;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.RequestUtil.createUserInstanceFromRequest;
import static no.unit.nva.publication.RequestUtil.getFileIdentifier;
import static no.unit.nva.publication.RequestUtil.getIdentifier;
import static no.unit.nva.publication.RequestUtil.getImportCandidateIdentifier;
import static no.unit.nva.publication.RequestUtil.getOwner;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RequestUtilTest {
    
    public static final String VALUE = "value";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    
    public static final String INJECT_NVA_USERNAME_CLAIM = "custom:nvaUsername";
    public static final String INJECT_ISSUER_CLAIM = "iss";
    public static final String INJECT_CLIENT_ID_CLAIM = "client_id";
    private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");


    @ParameterizedTest
    @MethodSource("provideIdentifiersForTesting")
    void shouldReturnCorrectIdentifierWhenIdentifierIsSet(String pathParameterKey,
                                                          Function<RequestInfo, SortableIdentifier> identifierGetter) {
        var uuid = SortableIdentifier.next();
        var requestInfo = new RequestInfo();
        requestInfo.setPathParameters(Map.of(pathParameterKey, uuid.toString()));

        var identifier = identifierGetter.apply(requestInfo);

        assertEquals(uuid, identifier);
    }

    private static Stream<Arguments> provideIdentifiersForTesting() {
        return Stream.of(
            Arguments.of(PUBLICATION_IDENTIFIER,
                         (Function<RequestInfo, SortableIdentifier>) req -> attempt(() -> getIdentifier(req))
                                                                                .orElseThrow()),
            Arguments.of(IMPORT_CANDIDATE_IDENTIFIER,
                         (Function<RequestInfo, SortableIdentifier>) req ->
                                                                         attempt(() ->
                                                                                     getImportCandidateIdentifier(req))
                                                                             .orElseThrow())
        );
    }

    @Test
    void canGetFileIdentifierFromRequest() throws ApiGatewayException {
        var uuid = randomUUID();
        var requestInfo = new RequestInfo();
        requestInfo.setPathParameters(Map.of(FILE_IDENTIFIER, uuid.toString()));

        var identifier = getFileIdentifier(requestInfo);

        assertEquals(uuid, identifier);
    }
    
    @Test
    void getIdentifierOnInvalidRequestThrowsException() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(BadRequestException.class, () -> getIdentifier(requestInfo));
        assertThrows(BadRequestException.class, () -> getImportCandidateIdentifier(requestInfo));
        assertThrows(BadRequestException.class, () -> getFileIdentifier(requestInfo));
    }
    
    @Test
    void canGetOwnerFromRequest() throws Exception {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(INJECT_NVA_USERNAME_CLAIM, VALUE));
        
        String owner = getOwner(requestInfo);
        
        assertEquals(VALUE, owner);
    }
    
    @Test
    void getOwnerThrowsUnauthorizedExceptionWhenOwnerCannotBeRetrieved() {
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(UnauthorizedException.class, () -> getOwner(requestInfo));
    }

    @Test
    void createExternalUserInstanceReturnsNonNullValue()
        throws NotFoundException, JsonProcessingException, UnauthorizedException {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(Map.of(
            INJECT_ISSUER_CLAIM, EXTERNAL_ISSUER,
            INJECT_CLIENT_ID_CLAIM, "clientId"
        )));

        var getExternalClientResponse = mock(GetExternalClientResponse.class);
        var identityServiceClient = mock(IdentityServiceClient.class);
        when(identityServiceClient.getExternalClient(any())).thenReturn(getExternalClientResponse);

        var userInstance = createUserInstanceFromRequest(requestInfo, identityServiceClient);
        assertNotNull(userInstance);
    }

    @Test
    void createExternalUserInstanceThrowsUnauthorizedWhenClientIdIsMissing()
        throws NotFoundException, JsonProcessingException {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(Map.of(
            INJECT_ISSUER_CLAIM, EXTERNAL_ISSUER
        )));

        var getExternalClientResponse = mock(GetExternalClientResponse.class);
        var identityServiceClient = mock(IdentityServiceClient.class);
        when(identityServiceClient.getExternalClient(any())).thenReturn(getExternalClientResponse);

        assertThrows(UnauthorizedException.class, () -> createUserInstanceFromRequest(requestInfo,
                                                                                      identityServiceClient));
    }

    @Test
    void createInternalUserInstanceReturnsValidData() throws ApiGatewayException {

        var username = RandomDataGenerator.randomString();
        var customer = RandomDataGenerator.randomUri();

        RequestInfo requestInfo = mock(RequestInfo.class);
        when(requestInfo.getCurrentCustomer()).thenReturn(customer);
        when(requestInfo.getUserName()).thenReturn(username);

        var identityServiceClient = mock(IdentityServiceClient.class);

        var userInstance = createUserInstanceFromRequest(requestInfo, identityServiceClient);
        assertEquals(username, userInstance.getUsername());
        assertEquals(customer, userInstance.getCustomerId());
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
