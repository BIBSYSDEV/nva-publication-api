package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.testing.associatedartifacts.PublishedFileGenerator;
import no.unit.nva.model.testing.associatedartifacts.util.RightsRetentionStrategyGenerator;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class NonDegreePermissionStrategyTest extends PublicationPermissionStrategyTest {

    @ParameterizedTest(name = "Should deny Curator {0} operation on degree resources belonging to the institution")
    @EnumSource(value = PublicationOperation.class)
    void shouldDenyCuratorOnDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();
        var topLevelCristinOrgId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRights(), cristinId,
                                                topLevelCristinOrgId);
        var publication = createDegreePhd(resourceOwner, institution, topLevelCristinOrgId);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance, uriRetriever)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator {0} operation on degree resources belonging to the institution "
                              + "with MANAGE_DEGREE access rights")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE", "TERMINATE",
        "TICKET_PUBLISH"})
    void shouldAllowCuratorOnDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createDegreePhd(resourceOwner, institution, randomUri()).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();
        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRightsWithDegree(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance, uriRetriever)
                                  .allowsAction(operation));
    }

    @Test
    void shouldDenyCuratorWithMissingEmbargoAccess()
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createDegreePhd(resourceOwner, institution, randomUri()).copy()
                              .withStatus(PUBLISHED)
                              .withAssociatedArtifacts(List.of(randomFileWithEmbargo()))
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRightsWithDegree(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                  .create(publication, userInstance, uriRetriever)
                                  .allowsAction(PublicationOperation.UPDATE));
    }

    @Test
    void shouldAllowCuratorWithEmbargoAccess()
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createDegreePhd(resourceOwner, institution, randomUri()).copy()
                              .withStatus(PUBLISHED)
                              .withAssociatedArtifacts(List.of(randomFileWithEmbargo(), PublishedFileGenerator.random()))
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRightsWithEmbargoDegree(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                   .create(publication, userInstance, uriRetriever)
                                   .allowsAction(PublicationOperation.UPDATE));
    }

    @Test
    void shouldDenyDegreeCuratorEmbargoAccess()
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createDegreePhd(resourceOwner, institution, randomUri()).copy()
                              .withStatus(PUBLISHED)
                              .withAssociatedArtifacts(List.of(randomFileWithEmbargo(), PublishedFileGenerator.random()))
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRightsWithDegree(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                  .create(publication, userInstance, uriRetriever)
                                  .allowsAction(PublicationOperation.UPDATE));
    }

    public static PublishedFile randomFileWithEmbargo() {
        return new PublishedFile(UUID.randomUUID(), RandomDataGenerator.randomString(),
                                 RandomDataGenerator.randomString(), RandomDataGenerator.randomInteger().longValue(),
                                 RandomDataGenerator.randomUri(), false, true, Instant.now().plusSeconds(60 * 60 * 24),
                                 RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(),
                                 RandomDataGenerator.randomString(), RandomDataGenerator.randomInstant(),
                                 new UploadDetails(new Username(RandomDataGenerator.randomString()),
                                                   RandomDataGenerator.randomInstant()));
    }
}
