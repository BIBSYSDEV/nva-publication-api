package no.unit.nva.importcandidate;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import java.util.Arrays;
import java.util.List;
import no.unit.nva.model.Identity;
import org.junit.jupiter.api.Test;

class ImportEntityDescriptionTest {

    @Test
    void shouldResequenceContributorsStartingFromOne() {
        var contributor1 = createContributor("Person1", 5);
        var contributor2 = createContributor("Person2", 10);

        var entityDescription = randomEntityDescription(contributor1, contributor2);

        var contributors = entityDescription.contributors().stream().toList();

        assertThat(contributors.getFirst().identity().getName(), is("Person1"));
        assertThat(contributors.getFirst().sequence(), is(1));

        assertThat(contributors.get(1).identity().getName(), is("Person2"));
        assertThat(contributors.get(1).sequence(), is(2));
    }

    @Test
    void shouldPlaceNullSequencesAtEnd() {
        var contributor1 = createContributor("Person1", 1);
        var contributor2 = createContributor("Person2", null);

        var description = randomEntityDescription(contributor1, contributor2);

        var contributors = description.contributors().stream().toList();

        assertThat(contributors.getFirst().identity().getName(), is("Person1"));
        assertThat(contributors.getFirst().sequence(), is(1));

        assertThat(contributors.get(1).identity().getName(), is("Person2"));
        assertThat(contributors.get(1).sequence(), is(2));
    }

    @Test
    void shouldHandleAlreadyCorrectSequences() {
        var contributor1 = createContributor("Person1", 1);
        var contributor2 = createContributor("Person2", 2);
        var contributor3 = createContributor("Person3", 3);

        var description = randomEntityDescription(contributor1, contributor2, contributor3);

        var contributors = description.contributors().stream().toList();

        assertThat(contributors.get(0).sequence(), is(1));
        assertThat(contributors.get(1).sequence(), is(2));
        assertThat(contributors.get(2).sequence(), is(3));
    }

    @Test
    void shouldNormalizeSequencesWithGaps() {
        var contributor1 = createContributor("Person1", 1);
        var contributor2 = createContributor("Person2", 5);
        var contributor3 = createContributor("Person3", 10);

        var description = randomEntityDescription(contributor1, contributor2, contributor3);

        var contributors = description.contributors().stream().toList();

        assertThat(contributors.get(0).sequence(), is(1));
        assertThat(contributors.get(1).sequence(), is(2));
        assertThat(contributors.get(2).sequence(), is(3));
    }

    @Test
    void shouldHandleEmptyContributorList() {
        var description = randomEntityDescription(emptyList());

        assertThat(description.contributors(), is(empty()));
    }

    @Test
    void shouldHandleNullContributorList() {
        var description = randomEntityDescription((ImportContributor) null);

        assertThat(description.contributors(), is(empty()));
    }

    @Test
    void shouldHandleSingleContributor() {
        var contributor = createContributor("Person1", 42);

        var description = randomEntityDescription(contributor);

        var contributors = description.contributors().stream().toList();

        assertThat(contributors.getFirst().sequence(), is(1));
    }

    @Test
    void shouldHandleAllNullSequences() {
        var contributor1 = createContributor("Person1", null);
        var contributor2 = createContributor("Person2", null);

        var description = randomEntityDescription(contributor1, contributor2);

        var contributors = description.contributors().stream().toList();

        assertThat(contributors.getFirst().sequence(), is(1));
        assertThat(contributors.get(1).sequence(), is(2));
    }

    private static ImportEntityDescription randomEntityDescription(ImportContributor... contributors) {
        return randomEntityDescription(Arrays.asList(contributors));
    }

    private static ImportEntityDescription randomEntityDescription(List<ImportContributor> contributors) {
        return new ImportEntityDescription(null, null, null, contributors, null, null, null, null, null);
    }

    private ImportContributor createContributor(String name, Integer sequence) {
        var identity = new Identity.Builder().withName(name).build();
        return new ImportContributor(identity, List.of(), null, sequence, false);
    }
}