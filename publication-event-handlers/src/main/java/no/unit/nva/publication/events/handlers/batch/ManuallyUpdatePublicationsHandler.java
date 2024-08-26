package no.unit.nva.publication.events.handlers.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.SearchService;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManuallyUpdatePublicationsHandler implements RequestStreamHandler {

    public static final String UPDATED_PUBLICATIONS_MESSAGE = "Updated publications: {}";
    private static final Logger logger = LoggerFactory.getLogger(ManuallyUpdatePublicationsHandler.class);
    private final SearchService searchService;
    private final ResourceService resourceService;

    @JacocoGenerated
    public ManuallyUpdatePublicationsHandler() {
        this.resourceService = ResourceService.defaultService();
        this.searchService = SearchService.create(UriRetriever.defaultUriRetriever(), resourceService);
    }

    public ManuallyUpdatePublicationsHandler(SearchService searchService, ResourceService resourceService) {
        this.searchService = searchService;
        this.resourceService = resourceService;
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        var request = ManuallyUpdatePublicationsRequest.fromInputStream(inputStream);
        var publications = searchService.searchPublicationsByParam(request.searchParams());
        var updatedPublications = ManuallyUpdatePublicationUtil.create(resourceService).update(publications, request);

        logger.info(UPDATED_PUBLICATIONS_MESSAGE, getIdentifiers(updatedPublications));
    }

    private static String getIdentifiers(List<Publication> publications) {
        return publications.stream()
                   .map(Publication::getIdentifier)
                   .map(SortableIdentifier::toString)
                   .collect(Collectors.joining(System.lineSeparator()));
    }
}
