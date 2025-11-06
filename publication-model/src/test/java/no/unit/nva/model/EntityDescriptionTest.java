package no.unit.nva.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.exceptions.UnsynchronizedPublicationChannelDateException;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.model.testing.EntityDescriptionBuilder;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static no.unit.nva.DatamodelConfig.dataModelObjectMapper;
import static no.unit.nva.model.testing.PublicationGenerator.randomEntityDescription;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Named.named;

class EntityDescriptionTest {

    private static final Javers JAVERS = JaversBuilder.javers().build();
    private static final String CHANNEL_URI_TEMPLATE =
            "https://api.nva.sikt.no/publication-channel/serial-publication/12345/%s";

    private static Stream<Named<PublicationDate>> yearValueProvider() {

        return Stream.of(
                named("Null publication date", null),
                named("Null year value", publicationDateWithYearOnly(null)),
                named("Empty year value", publicationDateWithYearOnly("")),
                named("Whitespace", publicationDateWithYearOnly(" ")),
                named("Future year", publicationDateWithYearOnly(String.valueOf(Year.now().getValue() + 10))),
                named("Random string", publicationDateWithYearOnly(randomString()))
        );
    }

    @Test
    void shouldCopyEntityDescriptionWithoutDataLoss() {

        var entityDescription = randomEntityDescription(JournalReview.class);
        var copy = entityDescription.copy().build();

        var diff = compareAsObjectNodes(entityDescription, copy);
        assertThat(diff.prettyPrint(), copy, is(equalTo(entityDescription)));
        assertThat(copy, is(not(sameInstance(entityDescription))));
    }

    @Test
    void shouldSortContributorsWhenItsSet() {
        var entityDescription = randomEntityDescription(JournalReview.class);

        var contributor1 = createRandomContributorWithSequence(2);
        var contributor2 = createRandomContributorWithSequence(1);
        entityDescription.setContributors(List.of(contributor1, contributor2));

        assertThat(entityDescription.getContributors().getFirst(), is(equalTo(contributor2)));
    }

    @Test
    void shouldSortContributorsInTheSameOrderIgnoringTheInputedOrderWhenSequencesAreSame() {
        var entityDescription1 = randomEntityDescription(JournalReview.class);
        var entityDescription2 = randomEntityDescription(JournalReview.class);

        var contributor1 = createRandomContributorWithSequence(1);
        var contributor2 = createRandomContributorWithSequence(1);
        var contributor3 = createRandomContributorWithSequence(1);

        entityDescription1.setContributors(List.of(contributor1, contributor2, contributor3));

        var expectedContributor1 = contributor1.copy().withSequence(1).build();
        var expectedContributor2 = contributor2.copy().withSequence(2).build();
        var expectedContributor3 = contributor3.copy().withSequence(3).build();
        entityDescription2.setContributors(List.of(expectedContributor1, expectedContributor2, expectedContributor3));

        var actual = entityDescription1.getContributors();
        var expected = entityDescription2.getContributors();

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldSortContributorsInTheSameOrderIgnoringTheInputedOrderWhenSequencesAreNull() {
        var entityDescription1 = randomEntityDescription(JournalReview.class);

        var contributor1 = createRandomContributorWithSequence(null);
        var contributor2 = createRandomContributorWithSequence(null);
        var contributor3 = createRandomContributorWithSequence(null);

        entityDescription1.setContributors(List.of(contributor1, contributor2, contributor3));

        var expectedContributor1 = contributor1.copy().withSequence(1).build();
        var expectedContributor2 = contributor2.copy().withSequence(2).build();
        var expectedContributor3 = contributor3.copy().withSequence(3).build();

        var actual = entityDescription1.getContributors();
        var expected = List.of(expectedContributor1, expectedContributor2, expectedContributor3);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldProvideContributorSequenceWhenSequenceIsNullAtStart() {
        var entityDescription = randomEntityDescription(JournalReview.class);
        entityDescription.setContributors(Collections.emptyList());

        var contributor1 = createRandomContributorWithSequence(null);
        var contributor1expected = contributor1.copy().withSequence(3).build();
        var contributor2 = createRandomContributorWithSequence(1);
        var contributor3 = createRandomContributorWithSequence(1);
        var contributor4 = createRandomContributorWithSequence(2);

        var contributors = List.of(contributor1, contributor2, contributor3, contributor4);
        entityDescription.setContributors(contributors);

        var expectedEntityDescription = entityDescription.copy().build();
        var expectedContributors = List.of(contributor2, contributor3, contributor4, contributor1expected);
        expectedEntityDescription.setContributors(expectedContributors);

        assertThat(entityDescription, is(equalTo(expectedEntityDescription)));
    }

    @Test
    void shouldProvideContributorSequenceWhenSequenceIsNullInBetween() {
        var entityDescription = randomEntityDescription(JournalReview.class);
        entityDescription.setContributors(Collections.emptyList());

        var contributor1 = createRandomContributorWithSequence(1);
        var contributor2 = createRandomContributorWithSequence(1);
        var contributor3 = createRandomContributorWithSequence(null);
        var contributor4 = createRandomContributorWithSequence(3);

        entityDescription.setContributors(List.of(contributor1, contributor2, contributor3, contributor4));

        var contributor2expected = contributor2.copy().withSequence(2).build();
        var contributor3expected = contributor3.copy().withSequence(4).build();
        var expectedList = List.of(contributor1, contributor2expected, contributor4, contributor3expected);

        var actual = entityDescription.getContributors();

        assertThat(actual, is(equalTo(expectedList)));
    }

    private static Contributor createRandomContributorWithSequence(Integer integer) {
        return EntityDescriptionBuilder.randomContributorWithSequence(integer);
    }

    @Test
    void shouldProvideContributorSequenceWhenSequenceIsNullAtEnd() {
        var entityDescription = randomEntityDescription(JournalReview.class);
        entityDescription.setContributors(Collections.emptyList());

        var contributor1 = createRandomContributorWithSequence(1);
        var contributor2 = createRandomContributorWithSequence(1);
        var contributor3 = createRandomContributorWithSequence(2);
        var contributor4 = createRandomContributorWithSequence(null);

        entityDescription.setContributors(List.of(contributor1, contributor2, contributor3, contributor4));

        var expectedContributor1 = contributor1.copy().withSequence(1).build();
        var expectedContributor2 = contributor2.copy().withSequence(2).build();
        var expectedContributor3 = contributor3.copy().withSequence(3).build();
        var expectedContributor4 = contributor4.copy().withSequence(4).build();


        var actual = entityDescription.getContributors();
        var expected = List.of(expectedContributor1, expectedContributor2, expectedContributor3, expectedContributor4);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldMaintainContributorSequence() {
        var entityDescription = randomEntityDescription(JournalReview.class);
        entityDescription.setContributors(Collections.emptyList());

        var contributor1 = createRandomContributorWithSequence(1);
        var contributor2 = createRandomContributorWithSequence(2);
        var contributor3 = createRandomContributorWithSequence(3);
        var contributor4 = createRandomContributorWithSequence(4);

        entityDescription.setContributors(List.of(contributor1, contributor2, contributor3, contributor4));

        var actual = entityDescription.getContributors();
        var expected = List.of(contributor1, contributor2, contributor3, contributor4);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldProvideContributorSequenceAndKeepStableSorting() {
        var entityDescription = randomEntityDescription(JournalReview.class);
        entityDescription.setContributors(Collections.emptyList());

        var contributor1 = createContributor(1, "1a");
        var contributor2 = createContributor(1, "1b");
        var expectedContributor2 = createContributor(2, "1b");
        var contributor3 = createContributor(null, "3");
        var expectedContributor3 = createContributor(3, "3");

        entityDescription.setContributors(List.of(contributor1, contributor2, contributor3));

        var expectedList = List.of(contributor1, expectedContributor2, expectedContributor3);

        assertThat(entityDescription.getContributors(), is(equalTo(expectedList)));
    }

    @ParameterizedTest(name = "Should throw when publication date is not synchronized with publication channel date")
    @MethodSource("yearValueProvider")
    void shouldThrowWhenPublicationDateIsNotSynchronizedWithPublicationChannelDate(PublicationDate value) {
        var entityDescription = randomEntityDescription(AcademicArticle.class);
        entityDescription.setPublicationDate(value);
        var reference = entityDescription.getReference();
        reference.setPublicationContext(new Journal(URI.create(CHANNEL_URI_TEMPLATE.formatted(Year.now().getValue()))));
        assertThrows(UnsynchronizedPublicationChannelDateException.class, entityDescription::validate);
    }

    @ParameterizedTest(name = "Should throw when publication channel date is not synchronized with publication date")
    @MethodSource("yearValueProvider")
    void shouldThrowWhenPublicationChannelDateIsNotSynchronizedWithPublicationDate(PublicationDate value) {
        var entityDescription = randomEntityDescription(AcademicArticle.class);
        var reference = entityDescription.getReference();
        var channelUri = URI.create(CHANNEL_URI_TEMPLATE.formatted(value));
        reference.setPublicationContext(new Journal(channelUri));
        entityDescription.setPublicationDate(publicationDateWithYearOnly(String.valueOf(Year.now().getValue())));
        assertThrows(UnsynchronizedPublicationChannelDateException.class, entityDescription::validate);
    }

    @Test
    void shouldNotThrowWhenPublicationDateIsSynchronizedWithPublicationChannelDate() {
        var entityDescription = randomEntityDescription(AcademicArticle.class);
        var reference = entityDescription.getReference();
        var value = String.valueOf(Year.now().getValue());
        var channelUri = URI.create(CHANNEL_URI_TEMPLATE.formatted(value));
        reference.setPublicationContext(new Journal(channelUri));
        entityDescription.setPublicationDate(publicationDateWithYearOnly(value));
        assertDoesNotThrow(entityDescription::validate);
    }

    private static Contributor createContributor(Integer integer, String name) {
        return new Contributor.Builder()
                .withSequence(integer)
                .withIdentity(new Identity.Builder()
                        .withName(name)
                        .build())
                .build();
    }

    private Diff compareAsObjectNodes(EntityDescription original, EntityDescription copy) {
        var originalObjectNode = dataModelObjectMapper.convertValue(original, ObjectNode.class);
        var copyObjectNode = dataModelObjectMapper.convertValue(copy, ObjectNode.class);
        return JAVERS.compare(originalObjectNode, copyObjectNode);
    }

    private static PublicationDate publicationDateWithYearOnly(String year) {
        return new PublicationDate.Builder().withYear(year).build();
    }
}