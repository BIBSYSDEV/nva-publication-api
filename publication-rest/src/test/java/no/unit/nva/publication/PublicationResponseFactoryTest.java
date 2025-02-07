package no.unit.nva.publication;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedLink;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static org.mockito.Mockito.mock;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import nva.commons.apigateway.RequestInfo;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PublicationResponseFactoryTest {
    @Test
    void associatedLinkShouldHaveType() throws JsonProcessingException {
        var publication =
            randomPublication().copy().withAssociatedArtifacts(List.of(randomAssociatedLink())).build();

        var response = PublicationResponseFactory.create(publication, getRequestInfo(), getIdentityServiceClient());

        var actualString = dtoObjectMapper.writeValueAsString(response);

        Assertions.assertEquals(1, StringUtils.countMatches(actualString, "AssociatedLink\""));
    }

    @Test
    void nullAssociatedArtifactShouldHaveType() throws JsonProcessingException {
        var publication =
            randomPublication().copy().withAssociatedArtifacts(List.of(new NullAssociatedArtifact())).build();

        var response = PublicationResponseFactory.create(publication, getRequestInfo(), getIdentityServiceClient());

        var actualString = dtoObjectMapper.writeValueAsString(response);

        Assertions.assertEquals(1, StringUtils.countMatches(actualString, "NullAssociatedArtifact\""));
    }

    @Test
    void fileShouldHaveType() throws JsonProcessingException {
        var publication =
            randomPublication().copy().withAssociatedArtifacts(List.of(randomOpenFile())).build();

        var response = PublicationResponseFactory.create(publication, getRequestInfo(), getIdentityServiceClient());

        var actualString = dtoObjectMapper.writeValueAsString(response);

        Assertions.assertEquals(1, StringUtils.countMatches(actualString, "OpenFile\""));
    }

    private static RequestInfo getRequestInfo() {
        return mock(RequestInfo.class);
    }

    private static IdentityServiceClient getIdentityServiceClient() {
        return mock(IdentityServiceClient.class);
    }

    @Test
    void shouldHaveOnlyOneTypeForFile() throws JsonProcessingException {
        var file =randomOpenFile().toDto();

        var actualString = dtoObjectMapper.writeValueAsString(file);

        Assertions.assertEquals(1, StringUtils.countMatches(actualString, "File\""));
    }

    @Test
    void shouldHaveOnlyOneTypeForAssociatedLink() throws JsonProcessingException {
        var file =randomAssociatedLink().toDto();

        var actualString = dtoObjectMapper.writeValueAsString(file);

        Assertions.assertEquals(1, StringUtils.countMatches(actualString, "AssociatedLink\""));
    }

    @Test
    void shouldHaveOnlyOneTypeForNullAssociatedArtifact() throws JsonProcessingException {
        var file = new NullAssociatedArtifact().toDto();

        var actualString = dtoObjectMapper.writeValueAsString(file);

        Assertions.assertEquals(1, StringUtils.countMatches(actualString, "NullAssociatedArtifact\""));
    }
}
