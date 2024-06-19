package no.unit.nva.cristin.lambda;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.NvaPublicationPartOf;
import no.unit.nva.cristin.mapper.NvaPublicationPartOfCristinPublication;
import no.unit.nva.model.ImportDetail;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import nva.commons.core.JacocoGenerated;

public class PublicationRepresentations {

    private final CristinObject cristinObject;
    private final FileContentsEvent<JsonNode> eventBody;
    private Publication incomingPublication;
    private Publication existingPublication;

    public PublicationRepresentations(CristinObject cristinObject, Publication publication,
                                      FileContentsEvent<JsonNode> eventBody) {
        this.cristinObject = cristinObject;
        this.incomingPublication = publication;
        this.eventBody = eventBody;
    }

    @JacocoGenerated
    public CristinObject getCristinObject() {
        return cristinObject;
    }

    public Publication getIncomingPublication() {
        return incomingPublication;
    }

    public void setIncomingPublication(Publication incomingPublication) {
        this.incomingPublication = incomingPublication;
    }

    public Publication getExistingPublication() {
        return existingPublication;
    }

    public PublicationRepresentations withExistingPublication(Publication publication) {
        this.existingPublication = publication;
        return this;
    }

    @JacocoGenerated
    public FileContentsEvent<JsonNode> getEventBody() {
        return eventBody;
    }

    public String getNvaPublicationIdentifier() {
        return nonNull(incomingPublication.getIdentifier())
                   ? incomingPublication.getIdentifier().toString()
                   : existingPublication.getIdentifier().toString();
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

    public boolean updateHasEffectiveChanges() {
        var existingPublication = getExistingPublication().copy()
                                      .withModifiedDate(null)
                                      .withIdentifier(null)
                                      .withImportDetails(getConcatenatedImportDetailsFromExistingAndIncomingPublication())
                                      .build();
        var incomingPublication = getIncomingPublication().copy()
                                      .withModifiedDate(null)
                                      .withIdentifier(null)
                                      .withImportDetails(getConcatenatedImportDetailsFromExistingAndIncomingPublication())
                                      .build();
        return !existingPublication.equals(incomingPublication);
    }

    private List<ImportDetail> getConcatenatedImportDetailsFromExistingAndIncomingPublication() {
        return Stream.concat(getExistingPublication().getImportDetails().stream(),
                             getIncomingPublication().getImportDetails().stream()).toList();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getCristinObject(), getIncomingPublication(), getEventBody(), getIncomingPublication());
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
               && Objects.equals(getIncomingPublication(), that.getIncomingPublication())
               && Objects.equals(getExistingPublication(), that.getExistingPublication());
    }

    @Override
    public String toString() {
        return "PublicationRepresentations{" +
               "cristinObject=" + cristinObject +
               ", eventBody=" + eventBody +
               ", incomingPublication=" + incomingPublication +
               ", existingPublication=" + existingPublication +
               '}';
    }
}
