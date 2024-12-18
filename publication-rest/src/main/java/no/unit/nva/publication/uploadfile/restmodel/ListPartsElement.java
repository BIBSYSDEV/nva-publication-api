package no.unit.nva.publication.uploadfile.restmodel;

import com.amazonaws.services.s3.model.PartSummary;

public record ListPartsElement(String partNumber, String size, String etag) {

    public static ListPartsElement of(PartSummary partSummary) {
        return new ListPartsElement(String.valueOf(partSummary.getPartNumber()),
                                    String.valueOf(partSummary.getSize()),
                                    partSummary.getETag());
    }
}
