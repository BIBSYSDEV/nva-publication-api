package no.unit.nva.publication.permissions.publication;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class BackendClientStrategyTest extends PublicationPermissionStrategyTest {
    @ParameterizedTest(name = "Should allow backend client {0} operation")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE)
    void shouldAllowBackendClientEverything(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var requestInfo = createBackendRequestInfo();
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var publication =
            createPublication(randomString(), EXTERNAL_CLIENT_CUSTOMER_URI, randomUri()).copy()
                .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                .build();

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }
}
