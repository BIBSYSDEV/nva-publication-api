package no.unit.publication.service.impl;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import no.unit.publication.model.PublicationSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DynamoDBPublicationServiceTest {

    private final static UUID ID1 = UUID.randomUUID();
    private final static UUID ID2 = UUID.randomUUID();

    private final static Instant INSTANT1 = Instant.now();
    private final static Instant INSTANT2 = INSTANT1.plusSeconds(100);
    private final static Instant INSTANT3 = INSTANT2.plusSeconds(100);
    private final static Instant INSTANT4 = INSTANT3.plusSeconds(100);

    @Test
    @DisplayName("filterOutOlderVersionsOfPublications returns only the latest version of each publication")
    public void filterOutOlderVersionsOfPublicationsReturnsOnlyTheLatestVersionOfEachPublication() {
        List<PublicationSummary> publications = publicationSummariesWithDuplicateUUIDs();
        ArrayList<PublicationSummary> expected = new ArrayList<>();
        expected.add(createPublication(ID1, INSTANT2));
        expected.add(createPublication(ID2, INSTANT4));
        List<PublicationSummary> actual = DynamoDBPublicationService.filterOutOlderVersionsOfPublications(publications);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
        assertThat(expected, containsInAnyOrder(actual.toArray()));
    }

    @Test
    @DisplayName("filterOutOlderVersionsOfPublications returns only the single version of a publication")
    public void filterOutOlderVersionsOfPublicationsReturnsTheSingleVersionOfAPublication() {
        List<PublicationSummary> publications = publicationSummariesWithoutDuplicateUUIDs();
        ArrayList<PublicationSummary> expected = new ArrayList<>();
        expected.add(createPublication(ID1, INSTANT1));
        expected.add(createPublication(ID2, INSTANT3));
        List<PublicationSummary> actual = DynamoDBPublicationService.filterOutOlderVersionsOfPublications(publications);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
        assertThat(expected, containsInAnyOrder(actual.toArray()));
    }

    private List<PublicationSummary> publicationSummariesWithDuplicateUUIDs() {
        List<PublicationSummary> publicationSummaries = new ArrayList<>();

        publicationSummaries.add(createPublication(ID1, INSTANT1));
        publicationSummaries.add(createPublication(ID1, INSTANT2));
        publicationSummaries.add(createPublication(ID2, INSTANT3));
        publicationSummaries.add(createPublication(ID2, INSTANT4));

        return publicationSummaries;
    }

    private List<PublicationSummary> publicationSummariesWithoutDuplicateUUIDs() {
        List<PublicationSummary> publicationSummaries = new ArrayList<>();
        publicationSummaries.add(createPublication(ID1, INSTANT1));
        publicationSummaries.add(createPublication(ID2, INSTANT3));
        return publicationSummaries;
    }

    private PublicationSummary createPublication(UUID id, Instant modifiedDate) {
        return new PublicationSummary.Builder()
            .withIdentifier(id)
            .withModifiedDate(modifiedDate)
            .withCreatedDate(INSTANT1)
            .withOwner("junit")
            .withMainTitle("Some main title")
            .withStatus(DRAFT)
            .build();
    }
}