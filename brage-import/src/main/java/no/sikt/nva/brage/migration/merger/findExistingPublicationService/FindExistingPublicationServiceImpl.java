package no.sikt.nva.brage.migration.merger.findExistingPublicationService;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.service.impl.ResourceService;

public class FindExistingPublicationServiceImpl implements FindExistingPublicationService {

    private final ResourceService resourceService;
    private final UriRetriever uriRetriever;
    private final String apiHost;

    public FindExistingPublicationServiceImpl(ResourceService resourceService, UriRetriever uriRetriever, String apiHost) {
        this.resourceService = resourceService;
        this.uriRetriever = uriRetriever;
        this.apiHost = apiHost;
    }

    @Override
    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        var cristinIdentifierFinder = new CristinIdentifierFinder(resourceService);
        var doiFinder = new DoiPublicationFinder(resourceService, uriRetriever, apiHost);
        var isbnFinder = new IsbnPublicationFinder(resourceService, uriRetriever, apiHost);
        var titleAndTypeFinder = new TitleAndTypePublicationFinder(resourceService, uriRetriever, apiHost);

        List<Supplier<Optional<PublicationForUpdate>>> updatePublicationSuppliers = List.of(
            () -> cristinIdentifierFinder.findExistingPublication(publicationRepresentation),
            () -> doiFinder.findExistingPublication(publicationRepresentation),
            () -> isbnFinder.findExistingPublication(publicationRepresentation),
            () -> titleAndTypeFinder.findExistingPublication(publicationRepresentation));

        return updatePublicationSuppliers.stream()
                   .map(Supplier::get)
                   .filter(Optional::isPresent)
                   .findFirst()
                   .orElse(Optional.empty());
    }
}
