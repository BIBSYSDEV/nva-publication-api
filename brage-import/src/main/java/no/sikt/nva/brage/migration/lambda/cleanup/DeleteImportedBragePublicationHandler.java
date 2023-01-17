package no.sikt.nva.brage.migration.lambda.cleanup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;

@JacocoGenerated
public class DeleteImportedBragePublicationHandler implements RequestHandler<InputStream, Void> {

    private final ResourceService resourceService;

    public DeleteImportedBragePublicationHandler(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @JacocoGenerated
    public DeleteImportedBragePublicationHandler() {
        this(ResourceService.defaultService());
    }

    @Override
    public Void handleRequest(InputStream input, Context context) {
        try {
            var identifier = parserInput(input);
            resourceService.updatePublishedStatusToDeleted(identifier);
        } catch (NotFoundException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private SortableIdentifier parserInput(InputStream input) throws JsonProcessingException {
        var string = IoUtils.streamToString(input);
        var jsonNode = new ObjectMapper().readTree(string);
        return new SortableIdentifier(jsonNode.get("id").asText());
    }
}
