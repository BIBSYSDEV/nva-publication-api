package no.unit.nva.importcandidate;

import static java.util.Collections.emptyList;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.List;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import nva.commons.core.paths.UriWrapper;
import org.jspecify.annotations.NonNull;
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

    @Test
    void shouldStoreLegacyUioIdentifierAsUioIdentifier() {
        var organization = Organization.fromUri(UriWrapper.fromUri(randomUri()).addChild("185.90.0.0").getUri());
        var contributor = createContributor(organization);

        var replacedOrganization = getReplacedOrganization(contributor);

        var expectedIdentifier = UriWrapper.fromUri(replacedOrganization.getId()).getLastPathElement();

        assertEquals("185.90.0.0", expectedIdentifier);
    }

    private static Organization getReplacedOrganization(ImportContributor contributor) {
        return contributor.affiliations()
                   .stream()
                   .findFirst()
                   .map(Affiliation::targetOrganization)
                   .map(Organization.class::cast)
                   .stream()
                   .findFirst()
                   .orElseThrow();
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

    private ImportContributor createContributor(Organization organization) {
        var identity = new Identity.Builder().withName(randomString()).build();
        return new ImportContributor(identity, List.of(new Affiliation(organization, null)), null, randomInteger(), false);
    }
}
