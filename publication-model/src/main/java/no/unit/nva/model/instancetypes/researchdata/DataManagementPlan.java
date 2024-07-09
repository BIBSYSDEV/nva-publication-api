package no.unit.nva.model.instancetypes.researchdata;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.RelatedDocument;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.JacocoGenerated;

/**
 * A data management plan is a document that describes the administrative processes around data sets.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class DataManagementPlan implements PublicationInstance<MonographPages> {
    public static final String RELATED_FIELD = "related";
    @JsonProperty(RELATED_FIELD)
    private final Set<RelatedDocument> related;
    private final MonographPages pages;

    /**
     * Constructor for DataManagementPlan (DMP).
     *
     * @param related A collection of URIs referencing things covered by the DMP.
     * @param pages   The pages description for the DMP document.
     */
    public DataManagementPlan(@JsonProperty(RELATED_FIELD) Set<RelatedDocument> related,
                              @JsonProperty(PAGES_FIELD) MonographPages pages) {
        this.pages = pages;
        this.related = nonNull(related) ? related : Set.of();
    }

    public Set<RelatedDocument> getRelated() {
        return related;
    }

    @Override
    public MonographPages getPages() {
        return pages;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataManagementPlan)) {
            return false;
        }
        DataManagementPlan that = (DataManagementPlan) o;
        return Objects.equals(getRelated(), that.getRelated())
                && Objects.equals(getPages(), that.getPages());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getRelated(), getPages());
    }
}
