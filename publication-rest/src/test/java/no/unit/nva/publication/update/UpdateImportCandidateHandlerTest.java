package no.unit.nva.publication.update;

import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.SCOPUS_IDENTIFIER;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.PROCESS_IMPORT_CANDIDATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateImportCandidateHandlerTest extends ResourcesLocalTest {

    public static final String IDENTIFIER = "importCandidateIdentifier";
    private static final Context CONTEXT = mock(Context.class);
    private ByteArrayOutputStream output;
    private ResourceService importCandidateService;
    private UpdateImportCandidateHandler handler;

    @BeforeEach
    public void setUp() {
        super.init("someTable");
        output = new ByteArrayOutputStream();
        importCandidateService = new ResourceService(client, "someTable");
        handler = new UpdateImportCandidateHandler(importCandidateService);
    }

    @Test
    void shouldUpdatedContributorsOnlyWhenUpdatingImportCandidate() throws NotFoundException, IOException {
        var importCandidate = createPersistedImportCandidate();
        var updatedImportCandidate = update(importCandidate);
        handler.handleRequest(request(updatedImportCandidate), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, ImportCandidate.class);

        var actualContributors = importCandidateService
                                     .getImportCandidateByIdentifier(importCandidate.getIdentifier())
                                     .getEntityDescription()
                                     .getContributors();

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(actualContributors, is(emptyIterable()));
    }

    @Test
    void shouldThrowUnauthorizedWhenMissingAccessRights() throws NotFoundException, IOException {
        handler.handleRequest(nonAuthorizedRequest(createPersistedImportCandidate()), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, ImportCandidate.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnNotFoundWhenAttemptingToUpdateNotExistingImportCandidate() throws IOException {
        handler.handleRequest(request(createImportCandidate()), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, ImportCandidate.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    private InputStream nonAuthorizedRequest(ImportCandidate importCandidate) throws JsonProcessingException {
        return new HandlerRequestBuilder<ImportCandidate>(restApiMapper)
                   .withBody(importCandidate)
                   .withUserName(importCandidate.getResourceOwner().getOwner().getValue())
                   .withCurrentCustomer(importCandidate.getPublisher().getId())
                   .withPathParameters(Map.of(IDENTIFIER, importCandidate.getIdentifier().toString()))
                   .build();
    }

    private ImportCandidate update(ImportCandidate importCandidate) {
        importCandidate.getEntityDescription().setContributors(List.of());
        importCandidate.setDoi(randomDoi());
        return importCandidate;
    }

    private InputStream request(ImportCandidate importCandidate) throws JsonProcessingException {
        return new HandlerRequestBuilder<ImportCandidate>(restApiMapper)
                   .withBody(importCandidate)
                   .withUserName(importCandidate.getResourceOwner().getOwner().getValue())
                   .withCurrentCustomer(importCandidate.getPublisher().getId())
                   .withAccessRights(importCandidate.getPublisher().getId(), PROCESS_IMPORT_CANDIDATE.name())
                   .withPathParameters(Map.of(IDENTIFIER, importCandidate.getIdentifier().toString()))
                   .build();
    }

    private ImportCandidate createPersistedImportCandidate() throws NotFoundException {
        var candidate = createImportCandidate();
        var importCandidate = importCandidateService.persistImportCandidate(candidate);
        return importCandidateService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
    }

    private ImportCandidate createImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(randomEntityDescription())
                   .withLink(randomUri())
                   .withIndexedDate(Instant.now())
                   .withPublishedDate(Instant.now())
                   .withHandle(randomUri())
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withSubjects(List.of(randomUri()))
                   .withIdentifier(SortableIdentifier.next())
                   .withRightsHolder(randomString())
                   .withProjects(List.of(new ResearchProject.Builder().withId(randomUri()).build()))

                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(SCOPUS_IDENTIFIER, randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .build();
    }

    private EntityDescription randomEntityDescription() {
        return new EntityDescription.Builder()
                   .withPublicationDate(new PublicationDate.Builder().withYear("2020").build())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withContributors(List.of(randomContributor()))
                   .withMainTitle(randomString())
                   .build();
    }

    private Contributor randomContributor() {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .build();
    }
}
