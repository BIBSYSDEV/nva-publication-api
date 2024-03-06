package no.sikt.nva.brage.migration.merger;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;

public record BrageMergingReport(Publication oldImage, Publication newImage) implements JsonSerializable {

    @Override
    public String toString() {
        return toJsonString();
    }
}
