package no.unit.nva.publication.file.upload.restmodel;

import software.amazon.awssdk.services.s3.model.Part;

public record ListPartsElement(String partNumber, String size, String etag) {

    public static ListPartsElement create(Part part) {
        return new ListPartsElement(String.valueOf(part.partNumber()),
                                    String.valueOf(part.size()),
                                    part.eTag());
    }
}
