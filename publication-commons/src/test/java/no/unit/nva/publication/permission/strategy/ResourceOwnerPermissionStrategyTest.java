package no.unit.nva.publication.permission.strategy;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceOwnerPermissionStrategyTest extends PublicationPermissionStrategyTest {

    //region Non-degree publications
    @ParameterizedTest(name = "Should allow ResourceOwner {0} operation on own published non-degree resource")
    @EnumSource(value = PublicationOperation.class, mode = Mode.INCLUDE, names = {"UPDATE", "UNPUBLISH",
        "DOI_REQUEST_CREATE", "PUBLISHING_REQUEST_CREATE", "SUPPORT_REQUEST_CREATE"})
    void shouldAllowResourceOwnerOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(resourceOwner, institution, cristinId, randomUri());
        var publication = createNonDegreePublication(resourceOwner, institution);
        publication.setAssociatedArtifacts(new AssociatedArtifactList());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny ResourceOwner {0} operation on own published non-degree resource")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"UPDATE", "UNPUBLISH", "DOI_REQUEST_CREATE",
        "PUBLISHING_REQUEST_CREATE", "SUPPORT_REQUEST_CREATE"})
    void shouldDenyResourceOwnerOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(resourceOwner, institution, cristinId, randomUri());
        var publication = createNonDegreePublication(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny ResourceOwner unpublish operation on own published non-degree resource "
        + "when files are approved ({0})")
    @MethodSource("filesWithApprovedStatus")
    void shouldDenyResourceOwnerUnpublishWhenFilesAreApproved(List<AssociatedArtifact> fileList)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(resourceOwner, institution, cristinId, randomUri());
        var publication =
            createNonDegreePublication(resourceOwner, institution).copy()
                .withAssociatedArtifacts(fileList)
                .build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(PublicationOperation.UNPUBLISH));
    }

    @ParameterizedTest(name = "Should allow ResourceOwner unpublish operation on own published non-degree resource "
                              + "when files are not approved ({0})")
    @MethodSource("filesWithNotApprovedStatus")
    void shouldAllowResourceOwnerUnpublishWhenNoFilesOrUnapproved(List<AssociatedArtifact> fileList)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(resourceOwner, institution, cristinId, randomUri());
        var publication =
            createNonDegreePublication(resourceOwner, institution).copy()
                .withAssociatedArtifacts(fileList)
                .build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(PublicationOperation.UNPUBLISH));
    }
    //endregion

    //region Degree publications
    @ParameterizedTest(name = "Should deny ResourceOwner {0} operation on own published degree resource")
    @EnumSource(value = PublicationOperation.class)
    void shoulDenyResourceOwnerOnDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(resourceOwner, institution, cristinId, randomUri());
        var publication = createDegreePhd(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    //endregion

    public static Stream<Arguments> filesWithApprovedStatus() {
        return Stream.of(
            arguments(named("UnpublishableFile", List.of(File.builder()
                                                 .withName(randomString())
                                                 .withIdentifier(randomUUID())
                                                 .withLicense(PublicationGenerator.randomUri())
                                                 .withAdministrativeAgreement(true)
                                                 .buildUnpublishableFile()))),
            arguments(named("PublishedFile", List.of(File.builder()
                                                  .withIdentifier(randomUUID())
                                                  .withName(randomString())
                                                  .withLicense(PublicationGenerator.randomUri())
                                                  .buildPublishedFile())))
        );
    }

    public static Stream<Arguments> filesWithNotApprovedStatus() {
        return Stream.of(
            arguments(named("UnpublishedFile", List.of(File.builder()
                                                           .withName(randomString())
                                                           .withIdentifier(randomUUID())
                                                           .withLicense(PublicationGenerator.randomUri())
                                                           .buildUnpublishedFile()))),
            arguments(named("Empty list", List.of())),
            arguments(named("NullAssociatedArtifact", List.of(new NullAssociatedArtifact()))),
            arguments(named("AssociatedLink", List.of(new AssociatedLink(randomUri(), randomString(),
                                                                                 randomString())))
            )
        );
    }
}
