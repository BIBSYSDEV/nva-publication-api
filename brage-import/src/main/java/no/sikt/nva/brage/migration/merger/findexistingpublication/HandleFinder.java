package no.sikt.nva.brage.migration.merger.findexistingpublication;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.AdditionalIdentifierBase;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.service.impl.ResourceService;

public class HandleFinder implements FindExistingPublicationService {

    private final SearchApiFinder searchApiFinder;
    private final DuplicatePublicationReporter duplicatePublicationReporter;

    public HandleFinder(ResourceService resourceService, UriRetriever uriRetriever, String apiHost,
                        DuplicatePublicationReporter duplicatePublicationReporter) {
        this.searchApiFinder = new SearchApiFinder(resourceService, uriRetriever, apiHost);
        this.duplicatePublicationReporter = duplicatePublicationReporter;
    }

    @Override
    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        var handle = publicationRepresentation.brageRecord().getId();
        var urlEncodedHandle = URLEncoder.encode(handle.toString(), StandardCharsets.UTF_8);

        var searchResponseContainingHandlesInRootOfObjectOrInAdditionalIdentifiers =
            searchApiFinder.fetchPublicationsByParam("handle", urlEncodedHandle);

        var publicationsWithHandleInAdditionalIdentifiers =
            searchResponseContainingHandlesInRootOfObjectOrInAdditionalIdentifiers.stream().filter(publication ->
                                               hasHandleInAdditionalIdentifiers(handle, publication)).toList();
        reportDuplicatesIfNecessary(publicationRepresentation, publicationsWithHandleInAdditionalIdentifiers);
        return  publicationsWithHandleInAdditionalIdentifiers.isEmpty()
                    ? Optional.empty()
                    : Optional.of(new PublicationForUpdate(MergeSource.HANDLE,
                                                           publicationsWithHandleInAdditionalIdentifiers.getFirst()));
    }

    private void reportDuplicatesIfNecessary(PublicationRepresentation publicationRepresentation,
                                             List<Publication> publicationsWithHandleInAdditionalIdentifiers) {
        if (FindExistingPublicationService.moreThanOneDuplicateFound(publicationsWithHandleInAdditionalIdentifiers)) {
            duplicatePublicationReporter.reportDuplicatePublications(publicationsWithHandleInAdditionalIdentifiers,
                                                                     publicationRepresentation.brageRecord(), DuplicateDetectionCause.HANDLE_DUPLICATES);
        }
    }

    private boolean hasHandleInAdditionalIdentifiers(URI handle, Publication publication) {
        return publication.getAdditionalIdentifiers().stream().anyMatch(additionalIdentifier -> matchesHandle(handle,
                                                                                                              additionalIdentifier));
    }

    private boolean matchesHandle(URI handle, AdditionalIdentifierBase additionalIdentifier) {
        return "handle".equalsIgnoreCase(additionalIdentifier.sourceName()) && additionalIdentifier.value()
                                                                                      .equals(handle.toString());
    }
}
