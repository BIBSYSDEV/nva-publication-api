package no.unit.nva.publication.model.business;

import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.instancetypes.degree.ArtisticDegreePhd;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import org.junit.jupiter.api.Test;

class FilesApprovalThesisTest {

    @Test
    void shouldDoRoundTripWithoutLossOfInformation() throws JsonProcessingException {
        var resource = randomDegree();
        var userInstance = UserInstance.create(randomString(), randomUri());
        var filesApprovalThesis = FilesApprovalThesis.createForUserInstitution(resource, userInstance,
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
                     () -> FilesApprovalThesis.createForUserInstitution(resource, userInstance, REGISTRATOR_PUBLISHES_METADATA_ONLY));
    }

    @Test
    void shouldCreateFileThesisApprovalWhenRegistratorCanPublishMetadataAndFiles() {
        var resource = randomDegree();
        var userInstance = UserInstance.create(randomString(), randomUri());
        var fileApprovalThesis = FilesApprovalThesis.createForUserInstitution(resource, userInstance, REGISTRATOR_PUBLISHES_METADATA_AND_FILES);

        assertFalse(fileApprovalThesis.getApprovedFiles().isEmpty());
    }

    @Test
    void shouldUpdateReceivingOrganizationDetailsWhenPublicationChannelClaimIsApplied() {
        var resource = randomDegree();
        var userInstance = UserInstance.create(randomString(), randomUri());
        var fileApprovalThesis = FilesApprovalThesis.createForUserInstitution(resource, userInstance, REGISTRATOR_PUBLISHES_METADATA_AND_FILES);

        var channelOwner = randomUri();
        var channelIdentifier = SortableIdentifier.next();
        fileApprovalThesis.applyPublicationChannelClaim(channelOwner, channelIdentifier);

        assertEquals(fileApprovalThesis.getReceivingOrganizationDetails().topLevelOrganizationId(), channelOwner);
        assertEquals(fileApprovalThesis.getReceivingOrganizationDetails().subOrganizationId(), channelOwner);
        assertEquals(fileApprovalThesis.getReceivingOrganizationDetails().influencingChannelClaim(), channelIdentifier);
    }

    private static Resource randomDegree() {
        return Resource.fromPublication(randomDegreePublication());
    }
}