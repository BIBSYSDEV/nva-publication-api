package no.unit.nva.importcandidate;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;

@SuppressWarnings("PMD.UnusedAssignment")
@JsonTypeInfo(use = Id.NAME, property = "type")
public record ImportEntityDescription(String mainTitle, URI language, PublicationDate publicationDate,
                                      Collection<ImportContributor> contributors, String mainAbstract,
                                      Map<String, String> alternativeAbstracts, Collection<String> tags,
                                      String description, Reference reference) {

    public ImportEntityDescription {
        contributors =
            nonNull(contributors) ? List.copyOf(updateSequenceNumber(contributors)) : Collections.emptyList();
        tags = nonNull(tags) ? List.copyOf(tags) : Collections.emptyList();
        alternativeAbstracts =
            nonNull(alternativeAbstracts) ? Map.copyOf(alternativeAbstracts) : Collections.emptyMap();
    }

    private static ImportContributor updateContributorWithSequence(ImportContributor contributor, int sequenceCounter) {
        return new ImportContributor(contributor.identity(), contributor.affiliations(), contributor.role(),
                                     sequenceCounter, contributor.correspondingAuthor());
    }

    private List<ImportContributor> updateSequenceNumber(Collection<ImportContributor> contributors) {
        var contributorList = contributors.stream()
                                  .filter(Objects::nonNull)
                                  .sorted(Comparator.comparing(ImportContributor::sequence,
                                                               Comparator.nullsLast(Comparator.naturalOrder())))
                                  .toList();

        return updatedContributorSequence(contributorList);
    }

    private List<ImportContributor> updatedContributorSequence(List<ImportContributor> contributorList) {
        return IntStream.range(0, contributorList.size())
                   .mapToObj(sequenceCounter -> updateContributorWithSequence(contributorList.get(sequenceCounter),
                                                                              sequenceCounter + 1))
                   .toList();
    }
}
