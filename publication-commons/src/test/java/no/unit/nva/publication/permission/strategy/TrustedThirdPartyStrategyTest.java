package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.model.PublicationOperation.DELETE;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class TrustedThirdPartyStrategyTest extends PublicationPermissionStrategyTest {

    @ParameterizedTest(name = "Should allow trusted third party {0} operation on non-degree resources")
    @EnumSource(value = PublicationOperation.class, mode = Mode.INCLUDE, names = {"UPDATE", "TERMINATE"})
    void shouldAllowTrustedThirdPartyOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var requestInfo = createThirdPartyRequestInfo();
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var publication =
            createNonDegreePublication(randomString(), EXTERNAL_CLIENT_CUSTOMER_URI).copy()
                .withResourceOwner(new ResourceOwner(new Username(userInstance.getUsername()),
                                                     userInstance.getTopLevelOrgCristinId()))
                .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                .build();

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny trusted third party {0} operation on non-degree resources")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"UNPUBLISH", "UPDATE", "TERMINATE",
        "READ_HIDDEN_FILES"})
    void shouldDenyTrustedThirdPartyOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var requestInfo = createThirdPartyRequestInfo();
        var publication = createNonDegreePublication(randomString(), EXTERNAL_CLIENT_CUSTOMER_URI);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    @Test
    void shouldAllowTrustedThirdPartyDeleteOnDraftNonDegree()
        throws JsonProcessingException, UnauthorizedException {

        var requestInfo = createThirdPartyRequestInfo();
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var publication =
            createNonDegreePublication(randomString(), EXTERNAL_CLIENT_CUSTOMER_URI,
                                       userInstance.getTopLevelOrgCristinId()).copy()
                .withStatus(DRAFT)
                .build();

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(DELETE));
    }

    @Test
    void shouldDenyTrustedThirdPartyWithMissingTopLevelOrgCristinId() {

        var userInstance = mock(UserInstance.class);
        when(userInstance.isExternalClient()).thenReturn(true);

        var publication =
            createNonDegreePublication(randomString(), EXTERNAL_CLIENT_CUSTOMER_URI,
                                       randomUri()).copy()
                .withStatus(UNPUBLISHED)
                .build();

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(DELETE));
    }

    @Test
    void shouldDenyTrustedClientEditPublicationWithMissingPublisher()
        throws JsonProcessingException, UnauthorizedException {
        var publication = createNonDegreePublication(randomString(), randomUri());
        publication.setPublisher(null);
        var requestInfo = createThirdPartyRequestInfo();

        Assertions.assertFalse(
            PublicationPermissionStrategy.create(publication, RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient))
                .allowsAction(UPDATE));
    }

    @Test
    void shouldAllowTrustedClientEditPublicationOnDegreeThatItPublished()
        throws JsonProcessingException, UnauthorizedException {
        var requestInfo = createThirdPartyRequestInfo();
        var userInstanse = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var publication = createDegreePhd(randomString(), EXTERNAL_CLIENT_CUSTOMER_URI,
                                          userInstanse.getTopLevelOrgCristinId());

        Assertions.assertTrue(
            PublicationPermissionStrategy.create(publication, userInstanse)
                .allowsAction(UPDATE));
    }

    @Test
    void shouldDenyTrustedClientEditPublicationOnDegreeFromAnotherPublisher()
        throws JsonProcessingException, UnauthorizedException {
        var requestInfo = createThirdPartyRequestInfo();
        var userInstanse = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var publisher = randomUri();
        var publication = createDegreePhd(randomString(), publisher, userInstanse.getTopLevelOrgCristinId());

        Assertions.assertFalse(
            PublicationPermissionStrategy.create(publication, userInstanse)
                .allowsAction(UPDATE));
    }
}
