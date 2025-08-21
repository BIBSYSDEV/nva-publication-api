package no.sikt.nva.scopus.update;

import static no.unit.nva.publication.model.business.importcandidate.CandidateStatus.IMPORTED;
import java.util.Optional;
import no.unit.nva.model.additionalidentifiers.ScopusIdentifier;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.importcandidate.CandidateStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.service.impl.ResourceService;

public class ScopusUpdater {


    private final ResourceService resourceService;

    public ScopusUpdater(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public ImportCandidate updateImportCandidate(ImportCandidate importCandidate) {
        var existingImportCandidate = getScopusIdentifier(importCandidate)
                                          .map(resourceService::getResourcesByScopusIdentifier)
                                          .flatMap(list -> list.stream().findFirst())
                                          .map(Resource::toImportCandidate);
        if (existingImportCandidate.isPresent() && !IMPORTED.equals(getImportStatus(existingImportCandidate.get()))) {
            return updateImportCandidate(importCandidate, existingImportCandidate.get());
        }
        return importCandidate;
    }

    private CandidateStatus getImportStatus(ImportCandidate importCandidate) {
        return importCandidate.getImportStatus().candidateStatus();
    }

    private static ImportCandidate updateImportCandidate(ImportCandidate importCandidate,
                                                      ImportCandidate persistedImportcandidate) {
        return persistedImportcandidate.copyImportCandidate()
                   .withEntityDescription(importCandidate.getEntityDescription())
                   .withAssociatedArtifacts(importCandidate.getAssociatedArtifacts())
                   .withAdditionalIdentifiers(importCandidate.getAdditionalIdentifiers())
                   .build();
    }

    private Optional<ScopusIdentifier> getScopusIdentifier(ImportCandidate importCandidate) {
        return importCandidate.getAdditionalIdentifiers().stream()
                   .filter(ScopusIdentifier.class::isInstance)
                   .map(ScopusIdentifier.class::cast)
                   .findFirst();
    }
}
