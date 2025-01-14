package no.unit.nva.publication.model.storage;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.UserInstance;
import org.junit.jupiter.api.Test;

class FileDaoTest {

    @Test
    void shouldReturnCorrectOwnerAndCustomerData() {
        var publication = randomPublication();
        var file = randomOpenFile();
        var userInstance = UserInstance.fromPublication(publication);

        var fileDao = createDao(file, publication, userInstance);

        assertEquals(userInstance.getUser(), fileDao.getOwner());
        assertEquals(userInstance.getCustomerId(), fileDao.getCustomerId());
        assertEquals("File", fileDao.indexingType());
    }

    private static FileDao createDao(File file, Publication publication, UserInstance userInstance) {
        return new FileDao(new SortableIdentifier(file.getIdentifier().toString()), publication.getIdentifier(),
                           Instant.now(), FileEntry.create(file, publication.getIdentifier(), userInstance));
    }
}