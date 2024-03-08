package no.unit.nva.publication.rightsretention;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAbstracts;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.commons.customer.CustomerApiRightsRetention;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.AccessRight;
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

        var randomPublication = PublicationGenerator.randomPublication(
            PublicationInstanceBuilder.randomPublicationInstance(publicationType).getClass());
        var file = createFileWithRrs(CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY));
        addFileToPublication(randomPublication, file);
        var applier = RightsRetentionsApplier.rrsApplierForNewPublication(randomPublication, getServerConfiguredRrs(RIGHTS_RETENTION_STRATEGY) ,
                                                                          randomString());
        applier.handle();

        assertThat(file.getRightsRetentionStrategy() instanceof NullRightsRetentionStrategy,is(equalTo(forceNull)));
    }

    private CustomerApiRightsRetention getServerConfiguredRrs(
        RightsRetentionStrategyConfiguration rightsRetentionStrategyConfiguration) {
        return new CustomerApiRightsRetention(rightsRetentionStrategyConfiguration.getValue(),
                                              randomString());
    }

    private void addFileToPublication(Publication publication, UnpublishedFile file) {
        publication.setAssociatedArtifacts(new AssociatedArtifactList(file));
    }

    public static Stream<Arguments> publicationTypeAndForceNull() {
        return Stream.of(Arguments.of(AcademicArticle.class, false),
                         Arguments.of(BookAbstracts.class, true),
                         Arguments.of(DegreeBachelor.class, true)
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