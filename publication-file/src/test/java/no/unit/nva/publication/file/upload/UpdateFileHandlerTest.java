package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.file.upload.restmodel.UpdateFileRequest;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class UpdateFileHandlerTest extends ResourcesLocalTest {

    private static final Context CONTEXT = new FakeContext();
    private ByteArrayOutputStream output;
    private UpdateFileHandler handler;

    @BeforeEach
    public void setUp() {
        super.init();
        this.output = new ByteArrayOutputStream();
        ResourceService resourceService = getResourceServiceBuilder().build();
        this.handler = new UpdateFileHandler(resourceService);
    }

    @Test
    void shouldReturnOk() throws IOException {
        var publication = randomPublication();
        var file = randomOpenFile();
        var request = createRequest(file.getIdentifier(), publication.getIdentifier());

        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    private InputStream createRequest(UUID fileIdentifier, SortableIdentifier publicationIdentifier)
        throws JsonProcessingException {

        return new HandlerRequestBuilder<UpdateFileRequest>(dtoObjectMapper).withBody(new UpdateFileRequest())
                   .withPathParameters(
                       Map.of("publicationIdentifier", publicationIdentifier.toString(), "fileIdentifier",
                              fileIdentifier.toString()))
                   .build();
    }
}