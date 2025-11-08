package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedArtifacts;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.model.business.importcandidate.CandidateStatus.IMPORTED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ImportCandidateServiceTest extends ResourcesLocalTest {

    public static final String IMPORT_CANDIDATES_TABLE_NAME = "import-candidates";
    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init(IMPORT_CANDIDATES_TABLE_NAME);
        resourceService = getResourceService(client, IMPORT_CANDIDATES_TABLE_NAME);
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
                                           ImportStatusFactory.createImported(randomString(), SortableIdentifier.next()));
        var fetchedPublication = resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
        assertThat(fetchedPublication.getImportStatus().candidateStatus(), equalTo(IMPORTED));
    }

    @Test
    void shouldDeleteImportCandidatePermanently() throws BadMethodException, NotFoundException {
        var importCandidate = resourceService.persistImportCandidate(randomImportCandidate());
        resourceService.deleteImportCandidate(importCandidate);
        assertThrows(NotFoundException.class,
                     () -> resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier()));
    }

    @Test
    void shouldThrowBadMethodExpectedWhenDeletingImportCandidateWithStatusImported() throws NotFoundException {
        var importCandidate = resourceService.persistImportCandidate(randomImportCandidate());
        resourceService.updateImportStatus(importCandidate.getIdentifier(),
                                           ImportStatusFactory.createImported(randomString(), SortableIdentifier.next()));
        assertThrows(BadMethodException.class,
                     () -> resourceService.deleteImportCandidate(importCandidate));
    }

    @Test
    void shouldUpdateExistingNotImportedImportCandidate() throws BadRequestException, NotFoundException {
        var importCandidate = resourceService.persistImportCandidate(randomImportCandidate());
        var updatedImportCandidate = update(importCandidate);
        resourceService.updateImportCandidate(updatedImportCandidate);
        var fetchedImportCandidate = resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier());

        var expectedImportCandidate = updatedImportCandidate.copy()
                                    .withModifiedDate(fetchedImportCandidate.getModifiedDate())
                                    .build();
        assertEquals(expectedImportCandidate, fetchedImportCandidate);
    }

    @Test
    void shouldOverwriteFilesWhenUpdatingExistingNotImportedImportCandidate() throws BadRequestException,
                                                                                 NotFoundException {
        var file = randomOpenFile();
        var importCandidate = randomImportCandidate().copy().withAssociatedArtifacts(List.of(file)).build();
        var existingImportCandidate = resourceService.persistImportCandidate(importCandidate);
        var newFile = randomOpenFile();
        var updatedImportCandidate = existingImportCandidate.copy().withAssociatedArtifacts(List.of(newFile)).build();
        resourceService.updateImportCandidate(updatedImportCandidate);
        var fetchedImportCandidate = resourceService.getImportCandidateByIdentifier(existingImportCandidate.getIdentifier());

        assertThat(fetchedImportCandidate.getAssociatedArtifacts(),
                   not(containsInAnyOrder(existingImportCandidate.getAssociatedArtifacts())));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingImportedImportCandidate() throws NotFoundException {
        var importCandidate = resourceService.persistImportCandidate(randomImportCandidate());
        resourceService.updateImportStatus(importCandidate.getIdentifier(),
                                           ImportStatusFactory.createImported(randomString(),
                                                                              SortableIdentifier.next()));
        var updatedImportCandidate = update(importCandidate);
        assertThrows(BadRequestException.class, () -> resourceService.updateImportCandidate(updatedImportCandidate));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUpdatingStatusForNotExistingImportCandidate() {
        assertThrows(NotFoundException.class,
                     () -> resourceService.updateImportStatus(SortableIdentifier.next(), null));
    }

    @Test
    void shouldThrowExceptionWhenCanNotFetchImportCandidateWhenUpdatingIt() {
        var importCandidate = randomImportCandidate();
        assertThrows(NotFoundException.class, () -> resourceService.updateImportCandidate(importCandidate));
    }

    private ImportCandidate update(ImportCandidate importCandidate) {
        importCandidate.setAssociatedArtifacts(new AssociatedArtifactList(randomAssociatedArtifacts()));
        return importCandidate;
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(randomString(), randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .build();
    }
}
