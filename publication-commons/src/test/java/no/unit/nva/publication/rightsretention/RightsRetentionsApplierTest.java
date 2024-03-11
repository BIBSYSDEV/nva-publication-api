package no.unit.nva.publication.rightsretention;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.publication.rightsretention.RightsRetentionsApplier.rrsApplierForUpdatedPublication;
import static no.unit.nva.publication.rightsretention.RightsRetentionsValueFinder.ILLEGAL_RIGHTS_RETENTION_STRATEGY_ON_FILE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
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
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAbstracts;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.commons.customer.CustomerApiRightsRetention;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RightsRetentionsApplierTest {

    @ParameterizedTest
    @MethodSource("publicationTypeAndForceNull")
    public void shouldForceNullRightsRetentionIfNotAcademicArticle(Class<? extends PublicationInstance> publicationType,
                                                                   boolean forceNull) throws BadRequestException {

        var publication = PublicationGenerator.randomPublication(PublicationInstanceBuilder.randomPublicationInstance(publicationType).getClass());
        var file = createFileWithRrs(CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY));
        addFilesToPublication(publication, file);
        var applier = RightsRetentionsApplier.rrsApplierForNewPublication(publication, getServerConfiguredRrs(RIGHTS_RETENTION_STRATEGY) ,
                                                                          randomString());
        applier.handle();

        assertThat(file.getRightsRetentionStrategy() instanceof NullRightsRetentionStrategy,is(equalTo(forceNull)));
    }

    @ParameterizedTest
    @MethodSource("rrsConfigIsValid")
    public void shouldThrowBadRequestWhenFileHasInvalidRrs(RightsRetentionStrategy rrs, boolean isValid) {
        var publication = PublicationGenerator.randomPublication(AcademicArticle.class);
        var file = createFileWithRrs(rrs);
        addFilesToPublication(publication, file);
        var applier = RightsRetentionsApplier.rrsApplierForNewPublication(publication, getServerConfiguredRrs(rrs.getConfiguredType()) ,
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
    public void shouldNotResetRrsWhenFilesMetadataIsChanged() throws BadRequestException {
        var originalPublication = PublicationGenerator.randomPublication(AcademicArticle.class);
        var overriddenBy = randomString();
        var originalRrs = OverriddenRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY, overriddenBy);
        var fileId = UUID.randomUUID();
        var originalFile = createFileWithRrs(fileId, originalRrs);
        var updatedFile = createFileWithRrs(fileId, originalRrs);

        var updatedPublication = originalPublication.copy()
            .withAssociatedArtifacts(new AssociatedArtifactList(updatedFile))
            .build();

        addFilesToPublication(originalPublication, originalFile);
        var applier = rrsApplierForUpdatedPublication(originalPublication, updatedPublication,
                                                                              getServerConfiguredRrs(
                                                                                  NULL_RIGHTS_RETENTION_STRATEGY),
                                                                              randomString());
        applier.handle();

        assertThat(updatedFile.getRightsRetentionStrategy(), is(equalTo(originalRrs)));
    }

    @Test
    public void shouldNotChangeRrsIfClientSetsNewStrategy() throws BadRequestException {
        var originalPublication = PublicationGenerator.randomPublication(AcademicArticle.class);
        var overriddenBy = randomString();
        var originalRrs = OverriddenRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY, overriddenBy);
        var updatedRrs = OverriddenRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY, overriddenBy);
        var fileId = UUID.randomUUID();
        var originalFile = createFileWithRrs(fileId, originalRrs);
        var updatedFile = createFileWithRrs(fileId, updatedRrs);

        var updatedPublication = originalPublication.copy()
                                     .withAssociatedArtifacts(new AssociatedArtifactList(updatedFile))
                                     .build();

        addFilesToPublication(originalPublication, originalFile);
        var applier = rrsApplierForUpdatedPublication(originalPublication, updatedPublication,
                                                      getServerConfiguredRrs(
                                                          NULL_RIGHTS_RETENTION_STRATEGY),
                                                      randomString());
        applier.handle();

        assertThat(updatedFile.getRightsRetentionStrategy(), is(equalTo(originalRrs)));
    }

    private CustomerApiRightsRetention getServerConfiguredRrs(
        RightsRetentionStrategyConfiguration rightsRetentionStrategyConfiguration) {
        return new CustomerApiRightsRetention(rightsRetentionStrategyConfiguration.getValue(),
                                              randomString());
    }

    private void addFilesToPublication(Publication publication, File... files) {
        publication.setAssociatedArtifacts(new AssociatedArtifactList(files));
    }

    public static Stream<Arguments> publicationTypeAndForceNull() {
        return Stream.of(Arguments.of(AcademicArticle.class, false),
                         Arguments.of(BookAbstracts.class, true),
                         Arguments.of(DegreeBachelor.class, true)
        );
    }

    //            case OverriddenRightsRetentionStrategy strategy -> Set.of(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
    //            case NullRightsRetentionStrategy strategy -> Set.of(NULL_RIGHTS_RETENTION_STRATEGY);
    //            case CustomerRightsRetentionStrategy strategy -> Set.of(RIGHTS_RETENTION_STRATEGY, OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
    //            case FunderRightsRetentionStrategy strategy -> Set.of(NULL_RIGHTS_RETENTION_STRATEGY, OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);

    public static Stream<Arguments> rrsConfigIsValid() {
        return Stream.of(Arguments.of(OverriddenRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY,
                                                                               ""), true),
                         Arguments.of(OverriddenRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY, ""), false),
                         Arguments.of(OverriddenRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY, ""), false),
                         Arguments.of(FunderRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY), true),
                         Arguments.of(FunderRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY), false),
                         Arguments.of(FunderRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY), true),
                         Arguments.of(NullRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY), false),
                         Arguments.of(NullRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY), false),
                         Arguments.of(NullRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY), true),
                         Arguments.of(CustomerRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY), true),
                         Arguments.of(CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY), true),
                         Arguments.of(CustomerRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY), false)

        );
    }

    private static UnpublishedFile createFileWithRrs(RightsRetentionStrategy rrs) {
        return createFileWithRrs(UUID.randomUUID(), rrs);
    }

    private static UnpublishedFile createFileWithRrs(UUID uuid, RightsRetentionStrategy rrs) {
        return new UnpublishedFile(uuid,
                                   randomString(),
                                   randomString(),
                                   RandomDataGenerator.randomInteger().longValue(),
                                   RandomDataGenerator.randomUri(),
                                   false,
                                   false,
                                   (Instant) null,
                                   rrs,
                                   randomString());
    }

}