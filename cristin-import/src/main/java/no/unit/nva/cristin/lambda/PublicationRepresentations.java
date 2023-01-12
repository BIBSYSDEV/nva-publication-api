package no.unit.nva.cristin.lambda;

import java.util.Objects;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import nva.commons.core.JacocoGenerated;
import com.fasterxml.jackson.databind.JsonNode;

public class PublicationRepresentations {

    private CristinObject cristinObject;

    private Publication publication;

    private FileContentsEvent<JsonNode> eventBody;

    public PublicationRepresentations(CristinObject cristinObject, Publication publication,
                                      FileContentsEvent<JsonNode> eventBody) {
        this.cristinObject = cristinObject;
        this.publication = publication;
        this.eventBody = eventBody;
    }

    public CristinObject getCristinObject() {
        return cristinObject;
    }

    @JacocoGenerated
    public void setCristinObject(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
    }

    public Publication getPublication() {
        return publication;
    }

    @JacocoGenerated
    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public FileContentsEvent<JsonNode> getEventBody() {
        return eventBody;
    }

    @JacocoGenerated
    public void setEventBody(
        FileContentsEvent<JsonNode> eventBody) {
        this.eventBody = eventBody;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getCristinObject(), getPublication(), getEventBody());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicationRepresentations)) {
            return false;
        }
        PublicationRepresentations that = (PublicationRepresentations) o;
        return Objects.equals(getCristinObject(), that.getCristinObject())
               && Objects.equals(getEventBody(), that.getEventBody())
               && Objects.equals(getPublication(), that.getPublication());
    }
}
