package no.unit.nva.publication;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedLink;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.FileDto;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;
import no.unit.nva.publication.model.business.publicationchannel.ChannelType;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.Constraint;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

class PublicationResponseFactoryTest {

    @Test
    void associatedLinkShouldHaveTypeWhenSerializedThroughResponse() throws JsonProcessingException {
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(randomAssociatedLink())).build();
        var resource = Resource.fromPublication(publication);

        var response = PublicationResponseFactory.create(resource, getRequestInfo(), getIdentityServiceClient());
        var jsonString = dtoObjectMapper.writeValueAsString(response);
        var typeCount = StringUtils.countMatches(jsonString, "AssociatedLink\"");

        assertEquals(1, typeCount);
    }

    @Test
    void nullAssociatedArtifactShouldHaveTypeWhenSerializedThroughResponse() throws JsonProcessingException {
        var publication = randomPublication().copy()
                              .withAssociatedArtifacts(List.of(new NullAssociatedArtifact()))
                              .build();
        var resource = Resource.fromPublication(publication);

        var response = PublicationResponseFactory.create(resource, getRequestInfo(), getIdentityServiceClient());
        var jsonString = dtoObjectMapper.writeValueAsString(response);
        var typeCount = StringUtils.countMatches(jsonString, "NullAssociatedArtifact\"");

        assertEquals(1, typeCount);
    }

    @Test
    void fileShouldHaveTypeWhenSerializedThroughResponse() throws JsonProcessingException {
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(randomOpenFile())).build();
        var resource = Resource.fromPublication(publication);

        var response = PublicationResponseFactory.create(resource, getRequestInfo(), getIdentityServiceClient());
        var jsonString = dtoObjectMapper.writeValueAsString(response);
        var typeCount = StringUtils.countMatches(jsonString, "OpenFile\"");

        assertEquals(1, typeCount);
    }

    private static RequestInfo getRequestInfo() {
        return mock(RequestInfo.class);
    }

    private static IdentityServiceClient getIdentityServiceClient() {
        return mock(IdentityServiceClient.class);
    }

    @Test
    void shouldHaveOnlyOneTypeForFile() throws JsonProcessingException {
        var file = randomOpenFile().toDto();

        var actualString = dtoObjectMapper.writeValueAsString(file);

        assertEquals(1, StringUtils.countMatches(actualString, "File\""));
    }

    @Test
    void shouldHaveOnlyOneTypeForAssociatedLink() throws JsonProcessingException {
        var file = randomAssociatedLink().toDto();

        var actualString = dtoObjectMapper.writeValueAsString(file);

        assertEquals(1, StringUtils.countMatches(actualString, "AssociatedLink\""));
    }

    @Test
    void shouldHaveOnlyOneTypeForNullAssociatedArtifact() throws JsonProcessingException {
        var file = new NullAssociatedArtifact().toDto();

        var actualString = dtoObjectMapper.writeValueAsString(file);

        assertEquals(1, StringUtils.countMatches(actualString, "NullAssociatedArtifact\""));
    }

    @Test
    void shouldSerializeFileOperationsDirectly() throws JsonProcessingException {
        var file = ((FileDto) randomOpenFile().toDto()).copy()
                       .withAllowedOperations(Set.of(FileOperation.READ_METADATA))
                       .build();

        var actualString = dtoObjectMapper.writeValueAsString(file);

        assertThat(actualString, containsString(FileOperation.READ_METADATA.getValue()));
    }

    @Test
    void shouldRoundTripFileOperationsWhenSerializedThroughResponse() throws JsonProcessingException {
        var publication =
            randomPublication().copy().withStatus(PUBLISHED).withAssociatedArtifacts(List.of(randomOpenFile())).build();
        var resource =Resource.fromPublication(publication);

        var response = PublicationResponseFactory.create(resource, getRequestInfo(), getIdentityServiceClient());

        var actualString = dtoObjectMapper.writeValueAsString(response);

        assertThat(actualString, containsString(FileOperation.READ_METADATA.getValue()));

        var object = dtoObjectMapper.readValue(actualString, PublicationResponse.class);
        var actualAllowedOperations = object.getAssociatedArtifacts()
                                        .stream()
                                        .findFirst()
                                        .map(FileDto.class::cast)
                                        .get()
                                        .allowedOperations();
        assertThat( actualAllowedOperations, equalTo(Set.of(FileOperation.READ_METADATA, FileOperation.DOWNLOAD)));
    }

    @Test
    void shouldReturnAuthenticatedResponseWhenUserHasPartialUpdateAndUploadFileAllowedOperationsOnly() {
        var resource = Resource.fromPublication(randomPublication(DegreeBachelor.class));
        var claim = randomClaimedChannel(resource.getInstanceType().orElseThrow());
        resource.setPublicationChannels(List.of(claim));
        resource.setStatus(PUBLISHED);
        var requestInfo = getRequestInfo();
        when(requestInfo.getAccessRights()).thenReturn(List.of(AccessRight.MANAGE_RESOURCES_STANDARD));
        when(requestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.ofNullable(claim.getOrganizationId()));
        var response =  PublicationResponseFactory.create(resource, requestInfo, getIdentityServiceClient());

        assertTrue(response.getAllowedOperations().contains(PublicationOperation.PARTIAL_UPDATE));
        assertTrue(response.getAllowedOperations().contains(PublicationOperation.UPLOAD_FILE));
    }

    private ClaimedPublicationChannel randomClaimedChannel(String instanceType) {
        return new ClaimedPublicationChannel(randomUri(), randomUri(), randomUri(),
                                             new Constraint(ChannelPolicy.EVERYONE, ChannelPolicy.OWNER_ONLY,
                                                            List.of(instanceType)), ChannelType.PUBLISHER,
                                             SortableIdentifier.next(), SortableIdentifier.next(), Instant.now(), Instant.now());
    }
}
