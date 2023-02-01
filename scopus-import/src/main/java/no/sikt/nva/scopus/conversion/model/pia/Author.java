package no.sikt.nva.scopus.conversion.model.pia;

import nva.commons.core.JacocoGenerated;

public class Author {

    private Publication publication;
    private String externalId;
    private int cristinId;
    private int sequenceNr;
    private String surname;
    private String firstname;
    private String authorName;
    private String orcid;

    @JacocoGenerated
    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    @JacocoGenerated
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @JacocoGenerated
    public int getCristinId() {
        return cristinId;
    }

    public void setCristinId(int cristinId) {
        this.cristinId = cristinId;
    }

    @JacocoGenerated
    public int getSequenceNr() {
        return sequenceNr;
    }

    public void setSequenceNr(int sequenceNr) {
        this.sequenceNr = sequenceNr;
    }

    @JacocoGenerated
    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @JacocoGenerated
    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    @JacocoGenerated
    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    @JacocoGenerated
    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }
}