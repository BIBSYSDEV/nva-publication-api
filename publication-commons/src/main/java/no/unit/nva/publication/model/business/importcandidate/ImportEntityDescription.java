package no.unit.nva.publication.model.business.importcandidate;

import java.net.URI;
import java.util.List;
import java.util.Map;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public record ImportEntityDescription(String mainTitle, URI language, PublicationDate publicationDate,
                                      List<ImportContributor> contributors, String mainAbstract,
                                      Map<String, String> alternativeAbstracts, List<String> tags, String description,
                                      Reference reference) {

}
