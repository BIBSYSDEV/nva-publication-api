package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.StorageModelTestUtils.randomPublishingRequest;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import org.junit.jupiter.api.Test;

class PublishingRequestCaseTest {

    @Test
    void shouldReturnCopyWithoutInformationLoss() {
        var original = createSample(randomElement(TicketStatus.values()));
        var copy = original.copy();
        assertThat(copy, is(equalTo(original)));
        assertThat(copy, is(not(sameInstance(original))));
    }

    @Test
    void shouldReturnTrueWhenApprovedFilesListContainsFileIdentifier() {
        var file = OpenFile.builder().withIdentifier(UUID.randomUUID()).buildOpenFile();
        var publishingRequestCase = createSample(randomElement(TicketStatus.values())).withFilesForApproval(
            Set.of(file)).approveFiles();

        assertTrue(publishingRequestCase.fileIsApproved(file));
    }

    @Deprecated
    @Test
    void shouldDeserializeApprovedFilesFromUUID() throws JsonProcessingException {
        var json = publishingRequestWithApprovedFilesAsUUID();
        var publishingRequestCase = JsonUtils.dtoObjectMapper.readValue(json, PublishingRequestCase.class);

        assertThat(publishingRequestCase.getApprovedFiles().size(), is(equalTo(1)));
        assertInstanceOf(OpenFile.class, publishingRequestCase.getApprovedFiles().iterator().next());
    }

    @Deprecated
    @Test
    void shouldDeserializeApprovedFilesFromFile() throws JsonProcessingException {
        var json = publishingRequestWithApprovedFilesAsFile();
        var publishingRequestCase = JsonUtils.dtoObjectMapper.readValue(json, PublishingRequestCase.class);

        assertThat(publishingRequestCase.getApprovedFiles().size(), is(equalTo(1)));
        assertInstanceOf(OpenFile.class, publishingRequestCase.getApprovedFiles().iterator().next());
    }

    @Deprecated
    @Test
    void shouldDeserializeFilesForApprovalFromObject() throws JsonProcessingException {
        var json = publishingRequestWithFileForApproval();
        var publishingRequestCase = JsonUtils.dtoObjectMapper.readValue(json, PublishingRequestCase.class);

        assertThat(publishingRequestCase.getFilesForApproval().size(), is(equalTo(1)));
        assertInstanceOf(OpenFile.class, publishingRequestCase.getFilesForApproval().iterator().next());
    }

    @Deprecated
    @Test
    void shouldDeserializeFilesForApprovalFromFile() throws JsonProcessingException {
        var json = publishingRequestWithFileForApprovalAsFile();
        var publishingRequestCase = JsonUtils.dtoObjectMapper.readValue(json, PublishingRequestCase.class);

        assertThat(publishingRequestCase.getFilesForApproval().size(), is(equalTo(1)));
        assertInstanceOf(OpenFile.class, publishingRequestCase.getFilesForApproval().iterator().next());
    }

    private String publishingRequestWithFileForApproval() {
        return """
        {
            "type" : "PublishingRequestCase",
            "filesForApproval" : [ {
                "identifier": "6f8d9cbc-2750-4c09-83a3-68ebddcf9921"
                }
            ]
          }
        """;
    }

    private String publishingRequestWithFileForApprovalAsFile() {
        return """
        {
            "type" : "PublishingRequestCase",
            "filesForApproval" : [ {
                "type" : "OpenFile",
                "identifier" : "6f8d9cbc-2750-4c09-83a3-68ebddcf9921",
                "administrativeAgreement" : false,
                "rightsRetentionStrategy" : {
                  "type" : "NullRightsRetentionStrategy",
                  "configuredType" : "Unknown"
                },
                "publishedDate" : "2024-11-07T11:48:03.501170Z",
                "visibleForNonOwner" : true
              }
            ]
          }
        """;
    }

    private String publishingRequestWithApprovedFilesAsUUID() {
        return """
            {
                "type" : "PublishingRequestCase",
                "approvedFiles" : [ "6f8d9cbc-2750-4c09-83a3-68ebddcf9921" ]
              }
            """;
    }

    private String publishingRequestWithApprovedFilesAsFile() {
        return """
            {
              "type" : "PublishingRequestCase",
              "approvedFiles" : [ {
                "type" : "OpenFile",
                "identifier" : "6f8d9cbc-2750-4c09-83a3-68ebddcf9921",
                "administrativeAgreement" : false,
                "rightsRetentionStrategy" : {
                  "type" : "NullRightsRetentionStrategy",
                  "configuredType" : "Unknown"
                },
                "publishedDate" : "2024-11-07T11:48:03.501170Z",
                "visibleForNonOwner" : true
              } ]
            }
            """;
    }

    private PublishingRequestCase createSample(TicketStatus status) {
        var sample = randomPublishingRequest();
        sample.setStatus(status);
        return sample;
    }
}