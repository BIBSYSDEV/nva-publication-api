package no.sikt.nva.brage.migration.merger.publicationcontextmerger;

import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.ResearchData;

public final class ResearchDataMerger extends PublicationContextMerger{

    private ResearchDataMerger() {
        super();
    }

    public static ResearchData merge(ResearchData researchData, PublicationContext publicationContext) {
        if (publicationContext instanceof ResearchData newResearchData) {
            return new ResearchData(getPublisher(researchData.getPublisher(), newResearchData.getPublisher()));
        } else {
            return researchData;
        }
    }
}
