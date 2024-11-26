package no.unit.nva.publication.rightsretention;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.file.PublisherVersion.ACCEPTED_VERSION;
import static no.unit.nva.model.associatedartifacts.file.PublisherVersion.PUBLISHED_VERSION;
import static no.unit.nva.publication.rightsretention.RightsRetentionsApplier.rrsApplierForUpdatedPublication;
import static no.unit.nva.publication.rightsretention.RightsRetentionsValueFinder.ILLEGAL_RIGHTS_RETENTION_STRATEGY_ON_FILE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.FunderRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.OverriddenRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAbstracts;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.commons.customer.CustomerApiRightsRetention;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class RightsRetentionsApplierTest {

    public static Stream<Arguments> publicationTypeAndForceNull() {
        return Stream.of(Arguments.of(AcademicArticle.class, false),
                         Arguments.of(BookAbstracts.class, true),
                         Arguments.of(DegreeBachelor.class, true)
        );
    }

    public static Stream<Arguments> rrsConfigIsValid() {
        return Stream.of(Arguments.of(OverriddenRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY,
                                                                               ""), true),
                         Arguments.of(OverriddenRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY, ""), false),
                         Arguments.of(OverriddenRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY, ""),
                                      false),
                         Arguments.of(FunderRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY),
                                      true),
                         Arguments.of(FunderRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY), false),
                         Arguments.of(FunderRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY), true),
                         Arguments.of(NullRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY), false),
                         Arguments.of(NullRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY), false),
                         Arguments.of(NullRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY), true),
                         Arguments.of(CustomerRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY),
                                      true),
                         Arguments.of(CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY), true),
                         Arguments.of(CustomerRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY), false)

        );
    }

    @ParameterizedTest
    @MethodSource("publicationTypeAndForceNull")
    void shouldForceNullRightsRetentionIfNotAcademicArticle(
        Class<? extends PublicationInstance<?>> publicationType,
        boolean forceNull) throws BadRequestException, UnauthorizedException {

        var publication = PublicationGenerator.randomPublication(
            PublicationInstanceBuilder.randomPublicationInstance(publicationType).getClass());
        var file = createPendingOpenFileWithAcceptedVersionAndRrs(CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY));
        addFilesToPublication(publication, file);
        var applier = RightsRetentionsApplier.rrsApplierForNewPublication(publication, getServerConfiguredRrs(
                                                                              RIGHTS_RETENTION_STRATEGY),
                                                                          randomString());
        applier.handle();

        assertThat(file.getRightsRetentionStrategy() instanceof NullRightsRetentionStrategy, is(equalTo(forceNull)));
    }

    @ParameterizedTest
    @MethodSource("rrsConfigIsValid")
    void shouldThrowBadRequestWhenFileHasInvalidRrs(RightsRetentionStrategy rrs, boolean isValid) {
        var publication = PublicationGenerator.randomPublication(AcademicArticle.class);
        var file = createPendingOpenFileWithAcceptedVersionAndRrs(rrs);
        addFilesToPublication(publication, file);
        var applier = RightsRetentionsApplier.rrsApplierForNewPublication(publication, getServerConfiguredRrs(
                                                                              rrs.getConfiguredType()),
                                                                          randomString());

        if (isValid) {
            assertDoesNotThrow(applier::handle);
        } else {
            var e = assertThrows(BadRequestException.class, applier::handle);
            assertThat(e.getMessage(), containsString(ILLEGAL_RIGHTS_RETENTION_STRATEGY_ON_FILE));
            assertThat(e.getMessage(), containsString(file.getIdentifier().toString()));
        }
    }

    @Test
    void shouldNotResetRrsWhenFilesMetadataIsChanged() throws BadRequestException, UnauthorizedException {
        var originalPublication = PublicationGenerator.randomPublication(AcademicArticle.class);
        var overriddenBy = randomString();
        var originalRrs = OverriddenRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY, overriddenBy);
        var fileId = UUID.randomUUID();
        var originalFile = createPendingOpenFileWithAcceptedVersionAndRrs(fileId, originalRrs);
        var updatedFile = createPendingOpenFileWithAcceptedVersionAndRrs(fileId, originalRrs);

        var updatedPublication = originalPublication.copy()
                                     .withAssociatedArtifacts(new AssociatedArtifactList(updatedFile))
                                     .build();

        addFilesToPublication(originalPublication, originalFile);
        var isCurator = false;
        var permissionStrategy = new FakePublicationPermissionStrategy(isCurator);
        var applier = rrsApplierForUpdatedPublication(originalPublication, updatedPublication,
                                                      getServerConfiguredRrs(
                                                          NULL_RIGHTS_RETENTION_STRATEGY),
                                                      randomString(), permissionStrategy);
        applier.handle();

        assertThat(updatedFile.getRightsRetentionStrategy(), is(equalTo(originalRrs)));
    }

    @Test
    void shouldNotChangeRrsIfClientSetsNewStrategy() throws BadRequestException, UnauthorizedException {
        var originalPublication = PublicationGenerator.randomPublication(AcademicArticle.class);
        var overriddenBy = randomString();
        var originalRrs = OverriddenRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY, overriddenBy);
        var updatedRrs = OverriddenRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY, overriddenBy);
        var fileId = UUID.randomUUID();
        var originalFile = createPendingOpenFileWithAcceptedVersionAndRrs(fileId, originalRrs);
        var updatedFile = createPendingOpenFileWithAcceptedVersionAndRrs(fileId, updatedRrs);

        var updatedPublication = originalPublication.copy()
                                     .withAssociatedArtifacts(new AssociatedArtifactList(updatedFile))
                                     .build();

        addFilesToPublication(originalPublication, originalFile);
        var isCurator = false;
        var permissionStrategy = new FakePublicationPermissionStrategy(isCurator);
        var applier = rrsApplierForUpdatedPublication(originalPublication, updatedPublication,
                                                      getServerConfiguredRrs(
                                                          NULL_RIGHTS_RETENTION_STRATEGY),
                                                      randomString(),
                                                      permissionStrategy);
        applier.handle();

        assertThat(updatedFile.getRightsRetentionStrategy(), is(equalTo(originalRrs)));
    }

    @ParameterizedTest
    @EnumSource(names = {
        "NULL_RIGHTS_RETENTION_STRATEGY",
        "RIGHTS_RETENTION_STRATEGY",
        "OVERRIDABLE_RIGHTS_RETENTION_STRATEGY"
    })
    void shouldAlwaysAllowNullRssIfFileIsPublishedVersion(RightsRetentionStrategyConfiguration configuredType) {
        var originalPublication = PublicationGenerator.randomPublication(AcademicArticle.class);

        var publishedFile = createPendingOpenFileWithRrs(UUID.randomUUID(), PUBLISHED_VERSION,
                                                         NullRightsRetentionStrategy.create(configuredType));
        var updatedPublication = originalPublication.copy()
                                     .withAssociatedArtifacts(new AssociatedArtifactList(publishedFile))
                                     .build();
        var isCurator = false;
        var permissionStrategy = new FakePublicationPermissionStrategy(isCurator);
        var applier = rrsApplierForUpdatedPublication(originalPublication, updatedPublication,
                                                      getServerConfiguredRrs(
                                                          configuredType),
                                                      randomString(),
                                                      permissionStrategy);
        assertDoesNotThrow(applier::handle);
    }

    @ParameterizedTest
    @EnumSource(names = {
        "NULL_RIGHTS_RETENTION_STRATEGY",
        "RIGHTS_RETENTION_STRATEGY",
        "OVERRIDABLE_RIGHTS_RETENTION_STRATEGY"
    })
    void shouldAlwaysAllowNullRssIfFileIsNotSet(RightsRetentionStrategyConfiguration configuredType) {
        var originalPublication = PublicationGenerator.randomPublication(AcademicArticle.class);

        var fileWhereVersionIsNotNot = createPendingOpenFileWithRrs(UUID.randomUUID(), null,
                                                                    NullRightsRetentionStrategy.create(configuredType));
        var updatedPublication = originalPublication.copy()
                                     .withAssociatedArtifacts(new AssociatedArtifactList(fileWhereVersionIsNotNot))
                                     .build();

        var isCurator = false;
        var permissionStrategy = new FakePublicationPermissionStrategy(isCurator);
        var applier = rrsApplierForUpdatedPublication(originalPublication, updatedPublication,
                                                      getServerConfiguredRrs(
                                                          configuredType),
                                                      randomString(),
                                                      permissionStrategy);
        assertDoesNotThrow(applier::handle);
    }

    @ParameterizedTest
    @EnumSource(names = {
        "NULL_RIGHTS_RETENTION_STRATEGY",
        "RIGHTS_RETENTION_STRATEGY",
        "OVERRIDABLE_RIGHTS_RETENTION_STRATEGY"
    })
    void shouldAllowPublishingCuratorToOverrideRetentionStrategy(RightsRetentionStrategyConfiguration configuredType) {
        var originalPublication =
            PublicationGenerator.randomPublication(AcademicArticle.class)
                .copy()
                .withAssociatedArtifacts(List.of(createPendingOpenFileWithRrs(UUID.randomUUID(), null,
                                                                              CustomerRightsRetentionStrategy.create(
                                                                       RIGHTS_RETENTION_STRATEGY))))
                .build();
        var userName = randomString();
        var permissionStrategy = mock(PublicationPermissionStrategy.class);
        when(permissionStrategy.isPublishingCuratorOnPublication()).thenReturn(true);
        var fileWithOverridenRrs = createPendingOpenFileWithRrs(UUID.randomUUID(), ACCEPTED_VERSION,
                                                                OverriddenRightsRetentionStrategy.create(
                                                         OVERRIDABLE_RIGHTS_RETENTION_STRATEGY, userName));
        var updatedPublication = originalPublication.copy()
                                     .withAssociatedArtifacts(new AssociatedArtifactList(fileWithOverridenRrs))
                                     .build();

        var applier = rrsApplierForUpdatedPublication(originalPublication, updatedPublication,
                                                      getServerConfiguredRrs(
                                                          configuredType),
                                                      randomString(), permissionStrategy);
        assertDoesNotThrow(applier::handle);
    }

    private static PendingOpenFile createPendingOpenFileWithAcceptedVersionAndRrs(RightsRetentionStrategy rrs) {
        return createPendingOpenFileWithRrs(UUID.randomUUID(), ACCEPTED_VERSION, rrs);
    }

    private static PendingOpenFile createPendingOpenFileWithAcceptedVersionAndRrs(UUID uuid, RightsRetentionStrategy rrs) {
        return createPendingOpenFileWithRrs(uuid, ACCEPTED_VERSION, rrs);
    }

    private static PendingOpenFile createPendingOpenFileWithRrs(UUID uuid, PublisherVersion publishedVersion,
                                                                RightsRetentionStrategy rrs) {
        return new PendingOpenFile(uuid,
                                   randomString(),
                                   randomString(),
                                   RandomDataGenerator.randomInteger().longValue(),
                                   RandomDataGenerator.randomUri(),
                                   false,
                                   publishedVersion,
                                   null,
                                   rrs,
                                   randomString(), new UserUploadDetails(null, null));
    }

    private CustomerApiRightsRetention getServerConfiguredRrs(
        RightsRetentionStrategyConfiguration rightsRetentionStrategyConfiguration) {
        return new CustomerApiRightsRetention(rightsRetentionStrategyConfiguration.getValue(),
                                              randomString());
    }

    private void addFilesToPublication(Publication publication, File... files) {
        publication.setAssociatedArtifacts(new AssociatedArtifactList(files));
    }

    private static class FakePublicationPermissionStrategy extends PublicationPermissionStrategy {

        private final boolean isCurator;

        public FakePublicationPermissionStrategy(boolean isCurator) {
            super(null, null);
            this.isCurator = isCurator;
        }

        @Override
        public boolean allowsAction(PublicationOperation permission) {
            return isCurator;
        }

        @Override
        public boolean isCuratorOnPublication() {
            return isCurator;
        }

        @Override
        public Set<PublicationOperation> getAllAllowedActions() {
            return isCurator ?  Set.of(PublicationOperation.UPDATE) : Set.of();
        }

        @Override
        public void authorize(PublicationOperation requestedPermission) throws UnauthorizedException {
            if (!isCurator) {
                throw new UnauthorizedException();
            }
        }
    }
}