package no.unit.nva.importcandidate;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.UnusedAssignment")
@JacocoGenerated
public record ImportEntityDescription(String mainTitle, URI language, PublicationDate publicationDate,
                                      Collection<ImportContributor> contributors, String mainAbstract,
                                      Map<String, String> alternativeAbstracts, Collection<String> tags,
                                      String description, Reference reference) {

    public ImportEntityDescription {
        contributors =
            nonNull(contributors) ? Collections.unmodifiableCollection(contributors) : Collections.emptyList();
        tags = nonNull(tags) ? Collections.unmodifiableCollection(tags) : Collections.emptyList();
    }
}
