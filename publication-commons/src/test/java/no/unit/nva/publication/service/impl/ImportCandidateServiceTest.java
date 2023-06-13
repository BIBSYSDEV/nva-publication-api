package no.unit.nva.publication.service.impl;

import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.util.Set;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.importcandidate.CandidateStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ImportCandidateServiceTest extends ResourcesLocalTest {

    public static final String IMPORT_CANDIDATES_TABLE_NAME = "import-candidates";
    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init(IMPORT_CANDIDATES_TABLE_NAME);
        resourceService = new ResourceService(client, IMPORT_CANDIDATES_TABLE_NAME);
    }

    @Test
    void shouldCreateResourceFromImportCandidate() throws NotFoundException {
        var importCandidate = randomImportCandidate();
        var persistedImportCandidate = resourceService.persistImportCandidate(importCandidate);
        var fetchedImportCandidate = resourceService.getImportCandidateByIdentifier(
            persistedImportCandidate.getIdentifier());
        assertThat(persistedImportCandidate, is(equalTo(fetchedImportCandidate)));
    }

    @Test
    void shouldUpdateImportStatus() throws NotFoundException {
        var importCandidate = resourceService.persistImportCandidate(randomImportCandidate());
        resourceService.updateImportStatus(importCandidate.getIdentifier(),
                                           ImportStatusFactory.createImported(null, null));
        var fetchedPublication = resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
        assertThat(fetchedPublication.getImportStatus().getCandidateStatus(), equalTo(CandidateStatus.IMPORTED));
    }

    @Test
    void shouldDeleteImportCandidatePermanently() throws BadMethodException, NotFoundException {
        var importCandidate = resourceService.persistImportCandidate(randomImportCandidate());
        var appender = LogUtils.getTestingAppenderForRootLogger();
        resourceService.deleteImportCandidate(importCandidate);
        assertThrows(NotFoundException.class,
                     () -> resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier()));
    }

    @Test
    void shouldThrowBadMethodExpectedWhenDeletingImportCandidateWithStatusImported() throws NotFoundException {
        var importCandidate = resourceService.persistImportCandidate(randomImportCandidate());
        resourceService.updateImportStatus(importCandidate.getIdentifier(),
                                           ImportStatusFactory.createImported(null, null));
        assertThrows(BadMethodException.class,
                     () -> resourceService.deleteImportCandidate(importCandidate));
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                   .withStatus(PublicationStatus.PUBLISHED)
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withLink(randomUri())
                   .withDoi(randomDoi())
                   .withHandle(randomUri())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withSubjects(List.of(randomUri()))
                   .withRightsHolder(randomString())
                   .withProjects(List.of(new ResearchProject.Builder().withId(randomUri()).build()))
                   .withFundings(List.of())
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(randomString(), randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .build();
    }
}
