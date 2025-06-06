package no.sikt.nva.brage.migration.merger.findexistingpublication;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.publication.service.impl.ResourceService;

public class FindExistingPublicationServiceImpl implements FindExistingPublicationService {

    private final ResourceService resourceService;
    private final UriRetriever uriRetriever;
    private final String apiHost;
    private final DuplicatePublicationReporter duplicatePublicationReporter;

    public FindExistingPublicationServiceImpl(ResourceService resourceService, UriRetriever uriRetriever,
                                              String apiHost,
                                              DuplicatePublicationReporter duplicatePublicationReporter) {
        this.resourceService = resourceService;
        this.uriRetriever = uriRetriever;
        this.apiHost = apiHost;
        this.duplicatePublicationReporter = duplicatePublicationReporter;
    }

    /**
     * CristinIdentifierFinder, HandleFinder and DoiPublicationFinder do not care about the type of publications to
     * merge.
     */
    @Override
    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        var handleFinder = new HandleFinder(resourceService, uriRetriever, apiHost, duplicatePublicationReporter);
        var cristinIdentifierFinder = new CristinIdentifierFinder(resourceService, duplicatePublicationReporter);
        var doiFinder = new DoiPublicationFinder(resourceService, uriRetriever, apiHost, duplicatePublicationReporter);
        var isbnFinder = new IsbnPublicationFinder(resourceService, uriRetriever, apiHost,
                                                   duplicatePublicationReporter);
        var titleAndTypeFinder = new TitleAndTypePublicationFinder(resourceService, uriRetriever, apiHost,
                                                                   duplicatePublicationReporter);

        List<Supplier<Optional<PublicationForUpdate>>> updatePublicationSuppliers = List.of(
            () -> cristinIdentifierFinder.findExistingPublication(publicationRepresentation),
            () -> handleFinder.findExistingPublication(publicationRepresentation),
            () -> doiFinder.findExistingPublication(publicationRepresentation),
            () -> titleAndTypeFinder.findExistingPublication(publicationRepresentation),
            () -> isbnFinder.findExistingPublication(publicationRepresentation));

        return updatePublicationSuppliers.stream()
                   .map(Supplier::get)
                   .filter(Optional::isPresent)
                   .findFirst()
                   .orElse(Optional.empty());
    }
}
