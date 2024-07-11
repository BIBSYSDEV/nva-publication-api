package no.unit.nva.model.instancetypes.degree;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Set;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.pages.MonographPages;

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

    public Set<RelatedDocument> getRelated() {
        return related;
    }
}
