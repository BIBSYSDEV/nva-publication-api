package no.unit.nva.publication.model.business.importcandidate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.Username;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Imported implements ImportStatus {

    private final static String IMPORTED_DATE = "setDate";
    private final static String PUBLICATION_ID = "NvaPublicationId";
    private final static String IMPORTED_BY = "setBy";

    @JsonProperty(IMPORTED_DATE)
    private final Instant setDate;

    @JsonProperty(PUBLICATION_ID)
    private final URI NvaPublicationId;

    @JsonProperty(IMPORTED_BY)
    private final Username setBy;

    @JsonCreator
    public Imported(@JsonProperty(IMPORTED_DATE) Instant importedDate,
                    @JsonProperty(PUBLICATION_ID) URI publicationId,
                    @JsonProperty(IMPORTED_BY) Username importedBy) {
        this.setDate = importedDate;
        this.setBy = importedBy;
        this.NvaPublicationId = publicationId;

    }

    public Instant getSetDate() {
        return setDate;
    }


    public URI getNvaPublicationId() {
        return NvaPublicationId;
    }


    public Username getSetBy() {
        return setBy;
    }


    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Imported)) {
            return false;
        }
        Imported imported = (Imported) o;
        return Objects.equals(getSetDate(), imported.getSetDate())
                && Objects.equals(getNvaPublicationId(), imported.getNvaPublicationId())
                && Objects.equals(getSetBy(), imported.getSetBy());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getSetDate(),
                getNvaPublicationId(),
                getSetBy());
    }
}
