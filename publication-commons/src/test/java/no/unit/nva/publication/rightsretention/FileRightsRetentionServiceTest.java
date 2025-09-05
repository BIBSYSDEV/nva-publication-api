package no.unit.nva.publication.rightsretention;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.file.PublisherVersion.ACCEPTED_VERSION;
import static no.unit.nva.model.associatedartifacts.file.PublisherVersion.PUBLISHED_VERSION;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.FunderRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.OverriddenRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
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
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class FileRightsRetentionServiceTest {

    private static final String ILLEGAL_RIGHTS_RETENTION_STRATEGY_MESSAGE = "Invalid rights retention strategy";

    public static Stream<Arguments> publicationTypeAndForceNull() {
        return Stream.of(Arguments.of(AcademicArticle.class, false), Arguments.of(BookAbstracts.class, true),
                         Arguments.of(DegreeBachelor.class, true));
    }

    public static Stream<Arguments> rrsConfigIsValid() {
        return Stream.of(
            Arguments.of(OverriddenRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY, ""), true),
//            Arguments.of(OverriddenRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY, ""), false),
//            Arguments.of(OverriddenRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY, ""), false),
            Arguments.of(FunderRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY), true),
//            Arguments.of(FunderRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY), false),
//            Arguments.of(FunderRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY), true),
//            Arguments.of(NullRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY), false),
//            Arguments.of(NullRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY), false),
            Arguments.of(NullRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY), true),
            Arguments.of(CustomerRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY), true),
            Arguments.of(CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY), true)
//            Arguments.of(CustomerRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY), false)
            );
    }

    // Test for new publications (single parameter method)
    @ParameterizedTest
    @MethodSource("publicationTypeAndForceNull")
    void shouldForceNullRightsRetentionIfNotAcademicArticleForNewPublication(
        Class<? extends PublicationInstance<?>> publicationType, boolean forceNull) {

        var publication = PublicationGenerator.randomPublication(
            PublicationInstanceBuilder.randomPublicationInstance(publicationType).getClass());
        var file = createPendingOpenFileWithAcceptedVersionAndRrs(
            CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY));
        addFilesToPublication(publication, file);

        var service = new FileRightsRetentionService(getServerConfiguredRrs(RIGHTS_RETENTION_STRATEGY),
                                                     UserInstance.fromPublication(publication));

        service.applyRightsRetention(Resource.fromPublication(publication));

        assertThat(file.getRightsRetentionStrategy() instanceof NullRightsRetentionStrategy, is(equalTo(forceNull)));
    }

    @ParameterizedTest
    @MethodSource("rrsConfigIsValid")
    void shouldValidateRightsRetentionStrategyForNewPublication(RightsRetentionStrategy rrs, boolean isValid) {
        var publication = PublicationGenerator.randomPublication(AcademicArticle.class);
        var file = createPendingOpenFileWithAcceptedVersionAndRrs(rrs);
        addFilesToPublication(publication, file);

        var service = new FileRightsRetentionService(getServerConfiguredRrs(rrs.getConfiguredType()),
                                                     randomUserInstance());

        if (isValid) {
            assertDoesNotThrow(() -> service.applyRightsRetention(Resource.fromPublication(publication)));
        } else {
            var e = assertThrows(BadRequestException.class,
                                 () -> service.applyRightsRetention(Resource.fromPublication(publication)));
            assertThat(e.getMessage(), containsString(ILLEGAL_RIGHTS_RETENTION_STRATEGY_MESSAGE));
        }
    }

    @Test
    void shouldApplyDefaultStrategyToAllFilesInNewPublication() {
        var publication = PublicationGenerator.randomPublication(AcademicArticle.class);
        var file1 = createPendingOpenFileWithAcceptedVersionAndRrs(
            CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY));
        var file2 = createPendingOpenFileWithAcceptedVersionAndRrs(
            CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY));
        addFilesToPublication(publication, file1, file2);

        var service = new FileRightsRetentionService(getServerConfiguredRrs(RIGHTS_RETENTION_STRATEGY),
                                                     UserInstance.fromPublication(publication));

        service.applyRightsRetention(Resource.fromPublication(publication));

        assertThat(file1.getRightsRetentionStrategy(), instanceOf(CustomerRightsRetentionStrategy.class));
        assertThat(file2.getRightsRetentionStrategy(), instanceOf(CustomerRightsRetentionStrategy.class));
    }

    // Test for existing publications (two parameter method)
    @Test
    void shouldNotResetRrsWhenFileMetadataIsUnchanged() {
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

        var service = new FileRightsRetentionService(getServerConfiguredRrs(NULL_RIGHTS_RETENTION_STRATEGY),
                                                     UserInstance.fromPublication(originalPublication));

        service.applyRightsRetention(Resource.fromPublication(updatedPublication),
                                     Resource.fromPublication(originalPublication));

        assertThat(updatedFile.getRightsRetentionStrategy(), is(equalTo(originalRrs)));
    }

    @Test
    void shouldPreserveExistingStrategyWhenClientTriesToChangeIt() {
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

        var service = new FileRightsRetentionService(getServerConfiguredRrs(NULL_RIGHTS_RETENTION_STRATEGY),
                                                     UserInstance.fromPublication(originalPublication));

        service.applyRightsRetention(Resource.fromPublication(updatedPublication),
                                     Resource.fromPublication(originalPublication));

        assertThat(updatedFile.getRightsRetentionStrategy(), is(equalTo(originalRrs)));
    }

    @Test
    void shouldApplyDefaultStrategyToNewFileInExistingPublication() {

        var originalPublication = PublicationGenerator.randomPublication(AcademicArticle.class);
        var existingFile = createPendingOpenFileWithAcceptedVersionAndRrs(
            CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY));
        addFilesToPublication(originalPublication, existingFile);

        var newFile = createPendingOpenFileWithAcceptedVersionAndRrs(
            CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY));
        var updatedPublication = originalPublication.copy()
                                     .withAssociatedArtifacts(new AssociatedArtifactList(existingFile, newFile))
                                     .build();

        var service = new FileRightsRetentionService(getServerConfiguredRrs(RIGHTS_RETENTION_STRATEGY),
                                                     UserInstance.fromPublication(originalPublication));

        service.applyRightsRetention(Resource.fromPublication(updatedPublication),
                                     Resource.fromPublication(originalPublication));

        assertThat(newFile.getRightsRetentionStrategy(), instanceOf(CustomerRightsRetentionStrategy.class));
    }

    @ParameterizedTest
    @EnumSource(names = {"NULL_RIGHTS_RETENTION_STRATEGY", "RIGHTS_RETENTION_STRATEGY",
        "OVERRIDABLE_RIGHTS_RETENTION_STRATEGY"})
    void shouldAlwaysAllowNullRssIfFileIsPublishedVersion(RightsRetentionStrategyConfiguration configuredType) {
        var originalPublication = PublicationGenerator.randomPublication(AcademicArticle.class);

        var publishedFile = createPendingOpenFileWithRrs(UUID.randomUUID(), PUBLISHED_VERSION,
                                                         NullRightsRetentionStrategy.create(configuredType));
        var updatedPublication = originalPublication.copy()
                                     .withAssociatedArtifacts(new AssociatedArtifactList(publishedFile))
                                     .build();

        var service = new FileRightsRetentionService(getServerConfiguredRrs(configuredType),
                                                     UserInstance.fromPublication(originalPublication));

        assertDoesNotThrow(() -> service.applyRightsRetention(Resource.fromPublication(updatedPublication),
                                                              Resource.fromPublication(originalPublication)));
    }

    @ParameterizedTest
    @EnumSource(names = {"NULL_RIGHTS_RETENTION_STRATEGY", "RIGHTS_RETENTION_STRATEGY",
        "OVERRIDABLE_RIGHTS_RETENTION_STRATEGY"})
    void shouldAlwaysAllowNullRssIfFileVersionIsNotSet(RightsRetentionStrategyConfiguration configuredType) {
        var originalPublication = PublicationGenerator.randomPublication(AcademicArticle.class);

        var fileWithoutVersion = createPendingOpenFileWithRrs(UUID.randomUUID(), null,
                                                              NullRightsRetentionStrategy.create(configuredType));
        var updatedPublication = originalPublication.copy()
                                     .withAssociatedArtifacts(new AssociatedArtifactList(fileWithoutVersion))
                                     .build();

        var service = new FileRightsRetentionService(getServerConfiguredRrs(configuredType),
                                                     UserInstance.fromPublication(originalPublication));

        assertDoesNotThrow(() -> service.applyRightsRetention(Resource.fromPublication(updatedPublication),
                                                              Resource.fromPublication(originalPublication)));
    }

    @Test
    void shouldAllowOverrideWhenUserHasPermission() {
        var originalPublication = PublicationGenerator.randomPublication(AcademicArticle.class);
        var originalFile = createPendingOpenFileWithAcceptedVersionAndRrs(
            CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY));
        addFilesToPublication(originalPublication, originalFile);


        var userName = randomString();
        var overriddenFile = createPendingOpenFileWithAcceptedVersionAndRrs(originalFile.getIdentifier(),
                                                                            OverriddenRightsRetentionStrategy.create(
                                                                                OVERRIDABLE_RIGHTS_RETENTION_STRATEGY,
                                                                                userName));
        var updatedPublication = originalPublication.copy()
                                     .withAssociatedArtifacts(new AssociatedArtifactList(overriddenFile))
                                     .build();

        var service = new FileRightsRetentionService(getServerConfiguredRrs(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY),
                                                     UserInstance.fromPublication(originalPublication));

        assertDoesNotThrow(() -> service.applyRightsRetention(Resource.fromPublication(updatedPublication),
                                                              Resource.fromPublication(originalPublication)));
    }

    @Test
    void shouldIgnoreRightsRetentionForInternalFiles() {
        var publication = PublicationGenerator.randomPublication(AcademicArticle.class);
        var internalFile = mock(InternalFile.class);
        when(internalFile.getPublisherVersion()).thenReturn(ACCEPTED_VERSION);
        when(internalFile.getRightsRetentionStrategy()).thenReturn(
            CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY));

        addFilesToPublication(publication, internalFile);

        var service = new FileRightsRetentionService(getServerConfiguredRrs(RIGHTS_RETENTION_STRATEGY),
                                                     UserInstance.fromPublication(publication));

        assertDoesNotThrow(() -> service.applyRightsRetention(Resource.fromPublication(publication)));
    }

    // Helper methods
    private static PendingOpenFile createPendingOpenFileWithAcceptedVersionAndRrs(RightsRetentionStrategy rrs) {
        return createPendingOpenFileWithRrs(UUID.randomUUID(), ACCEPTED_VERSION, rrs);
    }

    private static PendingOpenFile createPendingOpenFileWithAcceptedVersionAndRrs(UUID uuid,
                                                                                  RightsRetentionStrategy rrs) {
        return createPendingOpenFileWithRrs(uuid, ACCEPTED_VERSION, rrs);
    }

    private static PendingOpenFile createPendingOpenFileWithRrs(UUID uuid, PublisherVersion publisherVersion,
                                                                RightsRetentionStrategy rrs) {
        return new PendingOpenFile(uuid, randomString(), randomString(),
                                   RandomDataGenerator.randomInteger().longValue(), RandomDataGenerator.randomUri(),
                                   publisherVersion, null, rrs, randomString(), new UserUploadDetails(null, null));
    }

    private CustomerApiRightsRetention getServerConfiguredRrs(
        RightsRetentionStrategyConfiguration rightsRetentionStrategyConfiguration) {
        return new CustomerApiRightsRetention(rightsRetentionStrategyConfiguration.getValue(), randomString());
    }

    private void addFilesToPublication(Publication publication, File... files) {
        publication.setAssociatedArtifacts(new AssociatedArtifactList(files));
    }
}