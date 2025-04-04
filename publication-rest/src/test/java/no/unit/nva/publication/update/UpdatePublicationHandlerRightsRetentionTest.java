package no.unit.nva.publication.update;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.testing.PublicationGenerator.randomEntityDescription;
import static no.unit.nva.publication.CustomerApiStubs.stubCustomerResponseAcceptingFilesForAllTypesAndOverridableRrs;
import static no.unit.nva.publication.CustomerApiStubs.stubSuccessfulTokenResponse;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.OverriddenRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest(httpsEnabled = true)
class UpdatePublicationHandlerRightsRetentionTest extends UpdatePublicationHandlerTest {

    @BeforeEach
    public void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) throws NotFoundException {
        super.setUp(wireMockRuntimeInfo);
    }

    @Test
    void shouldSetCustomersConfiguredRrsWithOverridenByWhenFileIsNew() throws BadRequestException, IOException,
                                                                              NotFoundException {
        WireMock.reset();
        var academicArticle = publication.copy()
                                  .withEntityDescription(randomEntityDescription(AcademicArticle.class))
                                  .withAssociatedArtifacts(List.of())
                                  .build();

        var persistedPublication = Resource.fromPublication(academicArticle)
                                       .persistNew(resourceService, UserInstance.fromPublication(academicArticle));
        var username = academicArticle.getResourceOwner().getOwner().getValue();


        OverriddenRightsRetentionStrategy userSetRrs = OverriddenRightsRetentionStrategy.create(
            OVERRIDABLE_RIGHTS_RETENTION_STRATEGY,
            username);
        var file = createPendingOpenFileWithRrs(null);
        FileEntry.create(file, persistedPublication.getIdentifier(), UserInstance.fromPublication(persistedPublication))
            .persist(resourceService);
        file.setRightsRetentionStrategy(userSetRrs);
        var update = persistedPublication.copy().withAssociatedArtifacts(List.of(file)).build();
        var input = ownerUpdatesOwnPublication(persistedPublication.getIdentifier(), update);


        stubSuccessfulTokenResponse();
        stubCustomerResponseAcceptingFilesForAllTypesAndOverridableRrs(publication.getPublisher().getId());

        updatePublicationHandler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, Publication.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_OK)));

        var updatedPublication = resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        var insertedFile = (File) updatedPublication.getAssociatedArtifacts().getFirst();

        assertTrue(insertedFile.getRightsRetentionStrategy() instanceof OverriddenRightsRetentionStrategy);
        assertThat(((OverriddenRightsRetentionStrategy) insertedFile.getRightsRetentionStrategy()).getOverriddenBy(),
                   Is.is(IsEqual.equalTo(username)));
    }

    @Test
    void shouldSetNullRightsRetentionWhenChangingAcademicArticleToSomethingElse() throws BadRequestException,
                                                                                         IOException,
                                                                                         NotFoundException {
        WireMock.reset();

        OverriddenRightsRetentionStrategy userSetRrs = OverriddenRightsRetentionStrategy.create(
            OVERRIDABLE_RIGHTS_RETENTION_STRATEGY,
            randomString());
        var file = createPendingOpenFileWithRrs(userSetRrs);
        var academicArticle = publication.copy()
                                  .withEntityDescription(randomEntityDescription(AcademicArticle.class))
                                  .withAssociatedArtifacts(List.of(file))
                                  .build();

        var persistedPublication = Resource.fromPublication(academicArticle)
                                       .persistNew(resourceService, UserInstance.fromPublication(academicArticle));

        var update = persistedPublication.copy().withEntityDescription(
            randomEntityDescription(DegreeBachelor.class)
        ).build();
        var input = ownerUpdatesOwnPublication(persistedPublication.getIdentifier(), update);

        stubSuccessfulTokenResponse();
        stubCustomerResponseAcceptingFilesForAllTypesAndOverridableRrs(publication.getPublisher().getId());

        updatePublicationHandler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, Publication.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_OK)));

        var updatedPublication = resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        var insertedFile = (File) updatedPublication.getAssociatedArtifacts().getFirst();

        assertTrue(insertedFile.getRightsRetentionStrategy() instanceof NullRightsRetentionStrategy);
    }

    @Test
    void shouldPreserveRrsOverridenByWhenChangingNonRrsFileMetadata() throws BadRequestException,
                                                                             IOException,
                                                                             NotFoundException {
        WireMock.reset();
        var rrsOverriddenBy = randomString();
        OverriddenRightsRetentionStrategy userSetRrs = OverriddenRightsRetentionStrategy.create(
            OVERRIDABLE_RIGHTS_RETENTION_STRATEGY, rrsOverriddenBy
        );
        var fileId = UUID.randomUUID();
        var file = createPendingOpenFileWithRrs(fileId, userSetRrs);
        var updatedFile = createPendingOpenFileWithRrs(fileId, userSetRrs);
        var academicArticle = publication.copy()
                                  .withEntityDescription(randomEntityDescription(AcademicArticle.class))
                                  .withAssociatedArtifacts(List.of(file))
                                  .build();

        var persistedPublication = Resource.fromPublication(academicArticle)
                                       .persistNew(resourceService, UserInstance.fromPublication(academicArticle));

        var update = persistedPublication.copy().withAssociatedArtifacts(
            List.of(updatedFile)
        ).build();
        var input = ownerUpdatesOwnPublication(persistedPublication.getIdentifier(), update);

        stubSuccessfulTokenResponse();
        stubCustomerResponseAcceptingFilesForAllTypesAndOverridableRrs(publication.getPublisher().getId());

        updatePublicationHandler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, Publication.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_OK)));

        var updatedPublication = resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        var insertedFile = (File) updatedPublication.getAssociatedArtifacts().getFirst();

        assertInstanceOf(OverriddenRightsRetentionStrategy.class, insertedFile.getRightsRetentionStrategy());
        assertThat(((OverriddenRightsRetentionStrategy) insertedFile.getRightsRetentionStrategy()).getOverriddenBy(),
                   Is.is(IsEqual.equalTo(rrsOverriddenBy)));
    }

    private static PendingOpenFile createPendingOpenFileWithRrs(RightsRetentionStrategy rrs) {
        return createPendingOpenFileWithRrs(UUID.randomUUID(), rrs);
    }

    private static PendingOpenFile createPendingOpenFileWithRrs(UUID uuid, RightsRetentionStrategy rrs) {
        return new PendingOpenFile(uuid,
                                   RandomDataGenerator.randomString(),
                                   RandomDataGenerator.randomString(),
                                   RandomDataGenerator.randomInteger().longValue(),
                                   RandomDataGenerator.randomUri(),
                                   PublisherVersion.ACCEPTED_VERSION,
                                   null,
                                   rrs,
                                   RandomDataGenerator.randomString(),
                                   new UserUploadDetails(null, null));
    }

}