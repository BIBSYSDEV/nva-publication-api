package no.unit.nva.publication.permissions.publication;

import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.role.Role;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class ContributorPermissionStrategyTest extends PublicationPermissionStrategyTest {

    @ParameterizedTest(name = "Should allow verified contributor {0} operation on published non-degree resources")
    @EnumSource(value = PublicationOperation.class, mode = Mode.INCLUDE,
        names = {"UPDATE", "UNPUBLISH", "PUBLISHING_REQUEST_CREATE", "SUPPORT_REQUEST_CREATE", "DOI_REQUEST_CREATE"})
    void shouldAllowVerifiedContributorOnPublishedNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var contributor = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(contributor, institution, cristinId, randomUri());
        var publication = createPublicationWithContributor(contributor, cristinId, Role.CREATOR,
                                                           randomUri(), randomUri());
        publication.setAssociatedArtifacts(new AssociatedArtifactList());

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);


        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny verified contributor {0} operation on non-degree resources")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE,
        names = {"UNPUBLISH", "UPDATE", "PUBLISHING_REQUEST_CREATE", "SUPPORT_REQUEST_CREATE", "DOI_REQUEST_CREATE"})
    void shouldDenyVerifiedContributorOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var contributor = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(contributor, institution, cristinId, randomUri());
        var publication = createPublicationWithContributor(contributor, cristinId, Role.CREATOR,
                                                           randomUri(), randomUri());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }


    @Test
    void shouldNotGivePermissionToUnpublishPublicationWithPublishedFilesWhenUserIsContributor()
        throws JsonProcessingException, UnauthorizedException {
        var contributorName = randomString();
        var contributorCristinId = randomUri();
        var contributorInstitutionId = randomUri();
        var topLevelCristinOrgId = randomUri();

        var requestInfo = createUserRequestInfo(contributorName,
                                                contributorInstitutionId,
                                                contributorCristinId,
                                                topLevelCristinOrgId);
        var publication = createPublicationWithContributor(contributorName,
                                                           contributorCristinId,
                                                           Role.CREATOR,
                                                           randomUri(),
                                                           topLevelCristinOrgId);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication,
                                           RequestUtil.createUserInstanceFromRequest(
                                               requestInfo,
                                               identityServiceClient))
                                   .allowsAction(UNPUBLISH));
    }
}
