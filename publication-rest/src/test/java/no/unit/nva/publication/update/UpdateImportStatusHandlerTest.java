package no.unit.nva.publication.update;

import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.SCOPUS_IDENTIFIER;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_IMPORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.ImportStatusDto;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateImportStatusHandlerTest extends ResourcesLocalTest {

    public static final String IDENTIFIER = "importCandidateIdentifier";
    public static final String TABLE_NAME = "import-candidates";
    private static final Context CONTEXT = new FakeContext();
    private ByteArrayOutputStream output;
    private ResourceService importCandidateService;
    private UpdateImportStatusHandler handler;

    @BeforeEach
    public void setUp() {
        super.init(TABLE_NAME);
        output = new ByteArrayOutputStream();
        importCandidateService = new ResourceService(client, TABLE_NAME);
        handler = new UpdateImportStatusHandler(importCandidateService);
    }

    @Test
    void shouldReturnUnauthorizedIfUserHasNoAccessRights() throws IOException, NotFoundException {
        var importCandidate = createPersistedImportCandidate();
        var request = request(importCandidate, notApplicableImportStatus(), AccessRight.USER);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, ImportCandidate.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnNotFoundWhenAttemptingToUpdateStatusOnNonExistingImportCandidate() throws IOException {
        var importCandidate = createImportCandidate();
        var request = request(importCandidate, notApplicableImportStatus(), MANAGE_IMPORT);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, ImportCandidate.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldUpdateImportStatusSuccessfully() throws NotFoundException, IOException {
        var importCandidate = createPersistedImportCandidate();
        var request = request(importCandidate, notApplicableImportStatus(), MANAGE_IMPORT);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, ImportCandidate.class);
        var updatedImportCandidate = importCandidateService
                                         .getImportCandidateByIdentifier(importCandidate.getIdentifier());

        assertThat(updatedImportCandidate.getImportStatus().modifiedDate(), is(not(nullValue())));
        assertThat(updatedImportCandidate.getImportStatus().setBy(), is(not(nullValue())));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    private static ImportStatus notApplicableImportStatus() {
        return ImportStatusFactory.createNotApplicable(new Username(randomString()), null);
    }

    private InputStream request(ImportCandidate importCandidate, ImportStatus importStatus, AccessRight accessRight)
        throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(IDENTIFIER, importCandidate.getIdentifier().toString());
        var customerId = importCandidate.getPublisher().getId();
        return new HandlerRequestBuilder<ImportStatusDto>(restApiMapper)
                   .withUserName(importCandidate.getResourceOwner().getOwner().getValue())
                   .withCurrentCustomer(customerId)
                   .withBody(toImportStatusDto(importStatus))
                   .withAccessRights(customerId, accessRight)
                   .withPathParameters(pathParameters)
                   .build();
    }

    private static ImportStatusDto toImportStatusDto(ImportStatus importStatus) {
        return new ImportStatusDto(importStatus.candidateStatus(),
                                   importStatus.nvaPublicationId(),
                                   importStatus.comment());
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
                   .withDoi(randomDoi())
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
                   .withFundings(List.of(new FundingBuilder().build()))
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
