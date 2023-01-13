package no.unit.nva.cristin.lambda;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.NvaPublicationPartOf;
import no.unit.nva.cristin.mapper.NvaPublicationPartOfCristinPublication;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import nva.commons.core.JacocoGenerated;
import com.fasterxml.jackson.databind.JsonNode;

public class PublicationRepresentations {

    private final CristinObject cristinObject;
    private final FileContentsEvent<JsonNode> eventBody;
    private Publication publication;

    public PublicationRepresentations(CristinObject cristinObject, Publication publication,
                                      FileContentsEvent<JsonNode> eventBody) {
        this.cristinObject = cristinObject;
        this.publication = publication;
        this.eventBody = eventBody;
    }

    @JacocoGenerated
    public CristinObject getCristinObject() {
        return cristinObject;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    @JacocoGenerated
    public FileContentsEvent<JsonNode> getEventBody() {
        return eventBody;
    }

    public String getNvaPublicationIdentifier() {
        return publication.getIdentifier().toString();
    }

    public URI getOriginalEventFileUri() {
        return eventBody.getFileUri();
    }

    public Instant getOriginalTimeStamp() {
        return eventBody.getTimestamp();
    }

    public String getCristinIdentifier() {
        return cristinObject.getId().toString();
    }

    public String getPartOfCristinIdentifier() {
        return cristinObject.getBookOrReportPartMetadata().getPartOf();
    }

    public NvaPublicationPartOfCristinPublication getPartOf() {
        var publicationIdentifier = getNvaPublicationIdentifier();
        var publicationIsPartOfThisCristinPublication = getPartOfCristinIdentifier();
        return
            NvaPublicationPartOfCristinPublication.builder()
                .withNvaPublicationIdentifier(publicationIdentifier)
                .withPartOf(
                    NvaPublicationPartOf.builder()
                        .withCristinId(publicationIsPartOfThisCristinPublication)
                        .build())
                .build();
    }

    public boolean cristinObjectIsPartOfAnotherPublication() {
        return nonNull(cristinObject.getBookOrReportPartMetadata()) && nonNull(
            cristinObject.getBookOrReportPartMetadata().getPartOf());
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
