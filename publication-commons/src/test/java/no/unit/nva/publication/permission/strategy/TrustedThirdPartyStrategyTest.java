package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class TrustedThirdPartyStrategyTest extends PublicationPermissionStrategyTest {

    @ParameterizedTest(name = "Should allow trusted third party {0} operation on non-degree resources")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE"})
    void shouldAllowTrustedThirdPartyOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var requestInfo = createThirdPartyRequestInfo(getEditorAccessRightsWithDegree());
        var publication =
            createNonDegreePublication(randomString(), EXTERNAL_CLIENT_CUSTOMER_URI).copy()
                .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                .build();
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance, uriRetriever)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny trusted third party {0} operation on non-degree resources")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"UNPUBLISH", "UPDATE",
        "TICKET_PUBLISH", "TERMINATE"})
    void shouldDenyTrustedThirdPartyOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var requestInfo = createThirdPartyRequestInfo(getEditorAccessRightsWithDegree());
        var publication = createNonDegreePublication(randomString(), EXTERNAL_CLIENT_CUSTOMER_URI);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance, uriRetriever)
                                   .allowsAction(operation));
    }

    @Test
    void shouldDenyTrustedClientEditPublicationWithMissingPublisher()
        throws JsonProcessingException, UnauthorizedException {
        var publication = createNonDegreePublication(randomString(), randomUri());
        publication.setPublisher(null);
        var requestInfo = createThirdPartyRequestInfo(getEditorAccessRightsWithDegree());

        Assertions.assertFalse(
            PublicationPermissionStrategy.create(publication, RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient), uriRetriever)
                .allowsAction(UPDATE));
    }


    @Test
    void shouldAllowTrustedClientEditPublicationOnDegree()
        throws JsonProcessingException, UnauthorizedException {
        var publication = createDegreePhd(randomString(), EXTERNAL_CLIENT_CUSTOMER_URI);
        var requestInfo = createThirdPartyRequestInfo(new ArrayList<>());

        Assertions.assertTrue(
            PublicationPermissionStrategy.create(publication, RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient), uriRetriever)
                .allowsAction(UPDATE));
    }
}
