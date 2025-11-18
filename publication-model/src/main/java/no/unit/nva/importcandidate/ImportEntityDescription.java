package no.unit.nva.importcandidate;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;

@SuppressWarnings("PMD.UnusedAssignment")
@JsonTypeInfo(use = Id.NAME, property = "type")
public record ImportEntityDescription(String mainTitle, URI language, PublicationDate publicationDate,
                                      Collection<ImportContributor> contributors, String mainAbstract,
                                      Map<String, String> alternativeAbstracts, Collection<String> tags,
                                      String description, Reference reference) {

    public ImportEntityDescription {
        contributors = nonNull(contributors) ? List.copyOf(contributors) : Collections.emptyList();
        tags = nonNull(tags) ? List.copyOf(tags) : Collections.emptyList();
        alternativeAbstracts = nonNull(alternativeAbstracts) ? Map.copyOf(alternativeAbstracts) : Collections.emptyMap();
    }
}
