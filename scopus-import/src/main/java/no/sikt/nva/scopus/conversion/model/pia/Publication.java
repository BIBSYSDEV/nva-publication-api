package no.sikt.nva.scopus.conversion.model.pia;

import nva.commons.core.JacocoGenerated;

public class Publication {

    private String sourceCode;
    private String externalId;

    @JacocoGenerated
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @JacocoGenerated
    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }
}