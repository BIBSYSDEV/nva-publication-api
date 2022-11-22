package no.sikt.nva.brage.migration.mapper;

import no.sikt.nva.brage.migration.model.Record;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;

public final class BrageNvaMapper {

    private BrageNvaMapper() {

    }

    public static Publication toNvaPublication(Record brageRecord) {
        var publication = new Publication();
        publication.setDoi(brageRecord.getDoi());
        publication.setHandle(brageRecord.getId());
        publication.setResourceOwner(createResourceOwnerFromBrageRecord(brageRecord));
        publication.setEntityDescription(createEntityDescriptionFromBrageRecord(brageRecord));
        return publication;
    }

    private static EntityDescription createEntityDescriptionFromBrageRecord(Record brageRecord) {
        var entityDescription = new EntityDescription();
        entityDescription.setLanguage(brageRecord.getLanguage().getNva());
        return entityDescription;
    }

    private static ResourceOwner createResourceOwnerFromBrageRecord(Record brageRecord) {
        return new ResourceOwner(null, brageRecord.getCustomerId());
    }
}
