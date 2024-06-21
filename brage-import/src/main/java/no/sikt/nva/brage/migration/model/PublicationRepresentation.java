package no.sikt.nva.brage.migration.model;

import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Publication;

public record PublicationRepresentation(Record brageRecord, Publication publication) {

    public String getCustomerName() {
        return brageRecord.getCustomer().getName();
    }

}
