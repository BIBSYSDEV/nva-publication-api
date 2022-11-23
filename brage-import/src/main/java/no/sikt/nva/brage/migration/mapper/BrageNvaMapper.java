package no.sikt.nva.brage.migration.mapper;

import no.sikt.nva.brage.migration.model.Record;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;

public final class BrageNvaMapper {

    private BrageNvaMapper() {

    }

    public static Publication toNvaPublication(Record brageRecord) {
        var publication = new Publication();
        publication.setDoi(brageRecord.getDoi());
        publication.setHandle(brageRecord.getId());
        publication.setPublisher(createOrganization(brageRecord));
        publication.setEntityDescription(createEntityDescriptionFromBrageRecord(brageRecord));

        return publication;
    }

    private static Organization createOrganization(Record brageRecord) {
        var orginazation = new Organization();
        orginazation.setId(brageRecord.getCustomerId());
        return orginazation;
    }

    private static EntityDescription createEntityDescriptionFromBrageRecord(Record brageRecord) {
        var entityDescription = new EntityDescription();
        entityDescription.setLanguage(brageRecord.getEntityDescription().getLanguage().getNva());
        return entityDescription;
    }
}
