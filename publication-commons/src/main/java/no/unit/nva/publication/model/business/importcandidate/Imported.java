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

    private final static String IMPORTED_DATE = "importedDate";
    private final static String PUBLICATION_ID = "publicationId";
    private final static String IMPORTED_BY = "importedBy";

    @JsonProperty(IMPORTED_DATE)
    private final Instant importedDate;

    @JsonProperty(PUBLICATION_ID)
    private final URI publicationId;

    @JsonProperty(IMPORTED_BY)
    private final Username importedBy;

    @JsonCreator
    public Imported(@JsonProperty(IMPORTED_DATE) Instant importedDate,
                    @JsonProperty(PUBLICATION_ID) URI publicationId,
                    @JsonProperty(IMPORTED_BY) Username importedBy) {
        this.importedDate = importedDate;
        this.importedBy = importedBy;
        this.publicationId = publicationId;

    }

    public Instant getImportedDate() {
        return importedDate;
    }


    public URI getPublicationId() {
        return publicationId;
    }


    public Username getImportedBy() {
        return importedBy;
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
        return Objects.equals(getImportedDate(), imported.getImportedDate())
                && Objects.equals(getPublicationId(), imported.getPublicationId())
                && Objects.equals(getImportedBy(), imported.getImportedBy());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getImportedDate(),
                getPublicationId(),
                getImportedBy());
    }
}
