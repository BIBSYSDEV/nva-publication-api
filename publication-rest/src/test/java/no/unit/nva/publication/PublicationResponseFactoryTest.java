package no.unit.nva.publication;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedLink;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Set;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.FileDto;
import no.unit.nva.publication.model.business.Resource;
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
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(randomOpenFile())).build();
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
}
