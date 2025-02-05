package no.unit.nva.publication.events.handlers.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.SearchService;
import nva.commons.core.JacocoGenerated;

public class ManuallyUpdatePublicationsHandler implements RequestStreamHandler {

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
        ManuallyUpdatePublicationUtil.create(resourceService).update(publications, request);
    }
}
