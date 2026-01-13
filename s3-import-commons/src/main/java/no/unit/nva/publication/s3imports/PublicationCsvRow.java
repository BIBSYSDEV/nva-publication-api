package no.unit.nva.publication.s3imports;

import com.opencsv.bean.CsvBindByName;

public class PublicationCsvRow {

    @CsvBindByName(column = "identifier")
    private String identifier;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}