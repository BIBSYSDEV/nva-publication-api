package no.sikt.nva.brage.migration.merger;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record MergeResult(Publication institutionImage, Publication oldImage, Publication newImage)
    implements JsonSerializable {

    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }
}