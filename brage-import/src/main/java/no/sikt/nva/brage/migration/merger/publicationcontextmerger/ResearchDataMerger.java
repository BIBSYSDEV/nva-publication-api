package no.sikt.nva.brage.migration.merger.publicationcontextmerger;

import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.ResearchData;

public class ResearchDataMerger extends PublicationContextMerger{

    public ResearchDataMerger(Record record) {
        super(record);
    }

    public ResearchData merge(ResearchData researchData, PublicationContext publicationContext) {
        if (publicationContext instanceof ResearchData newResearchData) {
            return new ResearchData(getPublisher(researchData.getPublisher(), newResearchData.getPublisher()));
        } else {
            return researchData;
        }
    }
}
