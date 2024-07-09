package no.unit.nva.model.instancetypes.exhibition.manifestations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ExhibitionCatalog")
public class ExhibitionCatalogReference implements ExhibitionProductionManifestation {

    private static final String ID_FIELD = "id";
    @JsonProperty(ID_FIELD)
    private final URI id;

    @JsonCreator
    public ExhibitionCatalogReference(@JsonProperty(ID_FIELD) URI id) {
        this.id = id;
    }

    public URI getId() {
        return id;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExhibitionCatalogReference)) {
            return false;
        }
        ExhibitionCatalogReference that = (ExhibitionCatalogReference) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId());
    }
}
