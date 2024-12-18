package no.unit.nva.publication.uploadfile.restmodel;

import com.amazonaws.services.s3.model.PartSummary;

public class ListPartsElement {

    private String partNumber;
    private String size;
    private String etag;

    public static ListPartsElement of(PartSummary partSummary) {
        var listPartsElement = new ListPartsElement();
        listPartsElement.setPartNumber(Integer.toString(partSummary.getPartNumber()));
        listPartsElement.setSize(Long.toString(partSummary.getSize()));
        listPartsElement.setEtag(partSummary.getETag());
        return listPartsElement;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }
}
