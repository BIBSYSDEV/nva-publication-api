package no.sikt.nva.brage.migration.merger.findexistingpublication;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.service.impl.ResourceService;

public class HandleFinder implements FindExistingPublicationService {

    private final SearchApiFinder searchApiFinder;

    public HandleFinder(ResourceService resourceService, UriRetriever uriRetriever, String apiHost) {
        this.searchApiFinder = new SearchApiFinder(resourceService, uriRetriever, apiHost);
    }

    @Override
    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        var handle = publicationRepresentation.brageRecord().getId();
        var urlEncodedHandle = URLEncoder.encode(handle.toString(), StandardCharsets.UTF_8);

        var searchResponseContainingHandlesInRootOfObjectOrInAdditionalIdentifiers =
            searchApiFinder.fetchPublicationsByParam("handle", urlEncodedHandle);
        var publicationsWithHandleInAdditionalIdentifiers =
            searchResponseContainingHandlesInRootOfObjectOrInAdditionalIdentifiers.stream().filter(publication ->
                                               hasHandleInAdditionalIdentifiers(handle, publication)).findFirst();
        return publicationsWithHandleInAdditionalIdentifiers.map(
            publication -> new PublicationForUpdate(MergeSource.HANDLE, publication));
    }

    private boolean hasHandleInAdditionalIdentifiers(URI handle, Publication publication) {
        return publication.getAdditionalIdentifiers().stream().anyMatch(additionalIdentifier -> matchesHandle(handle,
                                                                                                              additionalIdentifier));
    }

    private boolean matchesHandle(URI handle, AdditionalIdentifier additionalIdentifier) {
        return "handle".equalsIgnoreCase(additionalIdentifier.getSourceName()) && additionalIdentifier.getValue()
                                                                                      .equals(handle.toString());
    }
}
