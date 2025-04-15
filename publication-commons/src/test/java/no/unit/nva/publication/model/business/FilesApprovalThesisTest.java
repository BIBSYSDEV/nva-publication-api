package no.unit.nva.publication.model.business;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import org.junit.jupiter.api.Test;

class FilesApprovalThesisTest {

    @Test
    void shouldDoRoundTripWithoutLossOfInformation() throws JsonProcessingException {
        var resource = Resource.fromPublication(randomPublication(DegreeBachelor.class));
        var userInstance = UserInstance.create(randomString(), randomUri());
        var filesApprovalThesis = FilesApprovalThesis.create(resource, userInstance,
                                                             REGISTRATOR_PUBLISHES_METADATA_ONLY);

        var json = JsonUtils.dtoObjectMapper.writeValueAsString(filesApprovalThesis);
        var roundTripped = JsonUtils.dtoObjectMapper.readValue(json, FilesApprovalThesis.class);

        assertEquals(filesApprovalThesis, roundTripped);
    }

    @Test
    void shouldThrowConflictExceptionWhenCreatingForNonThesis() {
        var resource = Resource.fromPublication(randomPublication(JournalArticle.class));
        var userInstance = UserInstance.create(randomString(), randomUri());

        assertThrows(IllegalStateException.class,
                     () -> FilesApprovalThesis.create(resource, userInstance, REGISTRATOR_PUBLISHES_METADATA_ONLY));
    }

    @Test
    void shouldNotThrowConflictExceptionForAllNonDegrees() {
        var resource = Resource.fromPublication(randomPublication(JournalArticle.class));
        var userInstance = UserInstance.create(randomString(), randomUri());

        assertThrows(IllegalStateException.class,
                     () -> FilesApprovalThesis.create(resource, userInstance, REGISTRATOR_PUBLISHES_METADATA_ONLY));
    }

    @Test
    void shouldCreateFileThesisApprovalWhenRegistratorCanPublishMetadataAndFiles() {
        var resource = Resource.fromPublication(randomPublication(DegreeBachelor.class));
        var userInstance = UserInstance.create(randomString(), randomUri());
        var fileApprovalThesis = FilesApprovalThesis.create(resource, userInstance, REGISTRATOR_PUBLISHES_METADATA_AND_FILES);

        assertFalse(fileApprovalThesis.getApprovedFiles().isEmpty());
    }
}