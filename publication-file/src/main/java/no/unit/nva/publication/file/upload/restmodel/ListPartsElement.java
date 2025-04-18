package no.unit.nva.publication.file.upload.restmodel;

import com.amazonaws.services.s3.model.PartSummary;

public record ListPartsElement(String partNumber, String size, String etag) {

    public static ListPartsElement create(PartSummary partSummary) {
        return new ListPartsElement(String.valueOf(partSummary.getPartNumber()),
                                    String.valueOf(partSummary.getSize()),
                                    partSummary.getETag());
    }
}
