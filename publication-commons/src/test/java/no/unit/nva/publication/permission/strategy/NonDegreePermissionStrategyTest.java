package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.testing.associatedartifacts.PublishedFileGenerator;
import no.unit.nva.model.testing.associatedartifacts.util.RightsRetentionStrategyGenerator;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NonDegreePermissionStrategyTest extends PublicationPermissionStrategyTest {

    private static Stream<Arguments> argumentsForDenyingCuratorFromPerformingOperationsOnProtectedDegreeResources() {
        return Stream.of(
            Arguments.of(PublicationOperation.UPDATE, DegreeLicentiate.class),
            Arguments.of(PublicationOperation.UPDATE, DegreeBachelor.class),
            Arguments.of(PublicationOperation.UPDATE, DegreeMaster.class),
            Arguments.of(PublicationOperation.UPDATE, DegreePhd.class),
            Arguments.of(PublicationOperation.DELETE, DegreeLicentiate.class),
            Arguments.of(PublicationOperation.DELETE, DegreeBachelor.class),
            Arguments.of(PublicationOperation.DELETE, DegreeMaster.class),
            Arguments.of(PublicationOperation.DELETE, DegreePhd.class),
            Arguments.of(PublicationOperation.UNPUBLISH, DegreeLicentiate.class),
            Arguments.of(PublicationOperation.UNPUBLISH, DegreeBachelor.class),
            Arguments.of(PublicationOperation.UNPUBLISH, DegreeMaster.class),
            Arguments.of(PublicationOperation.UNPUBLISH, DegreePhd.class),
            Arguments.of(PublicationOperation.TERMINATE, DegreeLicentiate.class),
            Arguments.of(PublicationOperation.TERMINATE, DegreeBachelor.class),
            Arguments.of(PublicationOperation.TERMINATE, DegreeMaster.class),
            Arguments.of(PublicationOperation.TERMINATE, DegreePhd.class),
            Arguments.of(PublicationOperation.TICKET_PUBLISH, DegreeLicentiate.class),
            Arguments.of(PublicationOperation.TICKET_PUBLISH, DegreeBachelor.class),
            Arguments.of(PublicationOperation.TICKET_PUBLISH, DegreeMaster.class),
            Arguments.of(PublicationOperation.TICKET_PUBLISH, DegreePhd.class)
        );
    }

    private static Stream<Arguments> argumentsForAllowingThesisCuratorPerformingOperationsOnProtectedDegreeResources() {
        return Stream.of(
            Arguments.of(PublicationOperation.UPDATE, DegreeLicentiate.class),
            Arguments.of(PublicationOperation.UPDATE, DegreeBachelor.class),
            Arguments.of(PublicationOperation.UPDATE, DegreeMaster.class),
            Arguments.of(PublicationOperation.UPDATE, DegreePhd.class),
            Arguments.of(PublicationOperation.UNPUBLISH, DegreeLicentiate.class),
            Arguments.of(PublicationOperation.UNPUBLISH, DegreeBachelor.class),
            Arguments.of(PublicationOperation.UNPUBLISH, DegreeMaster.class),
            Arguments.of(PublicationOperation.UNPUBLISH, DegreePhd.class)
        );
    }

    @ParameterizedTest(name = "Should deny Curator {0} operation on instance type {1} belonging to the institution")
    @MethodSource("argumentsForDenyingCuratorFromPerformingOperationsOnProtectedDegreeResources")
    void shouldDenyCuratorOnDegree(PublicationOperation operation, Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();
        var topLevelCristinOrgId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForCurator(), cristinId,
                                                topLevelCristinOrgId);
        var publication = createPublication(degreeInstanceClass, resourceOwner, institution, topLevelCristinOrgId);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(
        name = "Should allow Thesis curator {0} operation on instance type {1} belonging to the institution"
    )
    @MethodSource("argumentsForAllowingThesisCuratorPerformingOperationsOnProtectedDegreeResources")
    void shouldAllowThesisCuratorOnDegree(PublicationOperation operation, Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createPublication(degreeInstanceTypeClass, resourceOwner, institution, randomUri()).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();
        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForThesisCurator(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @Test
    void shouldDenyNonEmbargoThesisCuratorOnDegreeWithEmbargo()
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createDegreePhd(resourceOwner, institution, randomUri()).copy()
                              .withStatus(PUBLISHED)
                              .withAssociatedArtifacts(
                                  List.of(randomFileWithEmbargo(), PublishedFileGenerator.random()))
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForThesisCurator(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(PublicationOperation.UPDATE));
    }

    @Test
    void shouldAllowEmbargoThesisCuratorOnDegreeWithEmbargo()
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createDegreePhd(resourceOwner, institution, randomUri()).copy()
                              .withStatus(PUBLISHED)
                              .withAssociatedArtifacts(
                                  List.of(randomFileWithEmbargo(), PublishedFileGenerator.random()))
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForEmbargoThesisCurator(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
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
