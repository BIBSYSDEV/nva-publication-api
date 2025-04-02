package no.unit.nva.model.instancetypes.degree;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class DegreePhd extends DegreeBase {

    public static final String RELATED_PUBLICATIONS_FIELD = "related";
    private final Set<RelatedDocument> related;

    public DegreePhd(@JsonProperty(PAGES_FIELD) MonographPages pages,
                     @JsonProperty(SUBMITTED_DATE_FIELD) PublicationDate submittedDate,
                     @JsonProperty(RELATED_PUBLICATIONS_FIELD) Set<RelatedDocument> related) {
        super(pages, submittedDate);
        this.related = related;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DegreePhd degreePhd)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return getRelated().containsAll(degreePhd.getRelated());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getRelated());
    }

    public Set<RelatedDocument> getRelated() {
        return related;
    }
}
