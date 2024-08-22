package no.unit.nva.publication.events.handlers.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManuallyUpdatePublicationsHandler implements RequestStreamHandler {

    public static final String UPDATED_PUBLICATIONS_MESSAGE = "Updated publications: {}";
    private static final Logger logger = LoggerFactory.getLogger(ManuallyUpdatePublicationsHandler.class);
    private final SearchService searchService;
    private final ResourceService resourceService;

    public ManuallyUpdatePublicationsHandler(SearchService searchService, ResourceService resourceService) {
        this.searchService = searchService;
        this.resourceService = resourceService;
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        var request = ManuallyUpdatePublicationsRequest.fromInputStream(inputStream);
        var publications = searchService.searchPublicationsByParam(request.searchParams());
        var updatedPublications = updatePublications(publications, request);

        logger.info(UPDATED_PUBLICATIONS_MESSAGE, getIdentifiers(updatedPublications));
    }

    private static String getIdentifiers(List<Publication> publications) {
        return publications.stream()
                   .map(Publication::getIdentifier)
                   .map(SortableIdentifier::toString)
                   .collect(Collectors.joining(System.lineSeparator()));
    }

    private static Publisher createNewPublisher(Publication publication, String oldPublisher, String newPublisher) {
        return Optional.of(publication.getEntityDescription().getReference().getPublicationContext())
                   .map(Book.class::cast)
                   .map(Book::getPublisher)
                   .map(Publisher.class::cast)
                   .map(Publisher::getId)
                   .map(URI::toString)
                   .map(value -> value.replace(oldPublisher, newPublisher))
                   .map(URI::create)
                   .map(Publisher::new)
                   .orElseThrow();
    }

    private List<Publication> updatePublications(List<Publication> publications,
                                                 ManuallyUpdatePublicationsRequest request) {
        return publications.stream()
                   .filter(publication -> hasPublisher(publication, request.oldValue()))
                   .map(publication -> updatePublisher(publication, request.oldValue(), request.newValue()))
                   .map(resourceService::updatePublication)
                   .toList();
    }

    private Publication updatePublisher(Publication publication, String oldPublisher, String newPublisher) {
        var publicationContext = (Book) publication.getEntityDescription().getReference().getPublicationContext();
        var newPublicationContext = publicationContext.copy()
                                        .withPublisher(createNewPublisher(publication, oldPublisher, newPublisher))
                                        .build();
        publication.getEntityDescription().getReference().setPublicationContext(newPublicationContext);
        return publication;
    }

    private boolean hasPublisher(Publication publication, String publisher) {
        return Optional.of(publication.getEntityDescription().getReference().getPublicationContext())
                   .filter(Book.class::isInstance)
                   .map(Book.class::cast)
                   .map(Book::getPublisher)
                   .filter(Publisher.class::isInstance)
                   .map(Publisher.class::cast)
                   .map(Publisher::getId)
                   .map(URI::toString)
                   .filter(value -> value.contains(publisher))
                   .isPresent();
    }
}
