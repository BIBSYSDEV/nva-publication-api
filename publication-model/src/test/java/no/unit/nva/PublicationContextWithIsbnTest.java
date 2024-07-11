package no.unit.nva;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublicationWithEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn13;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import java.util.Arrays;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PublicationContextWithIsbnTest {

    @ParameterizedTest
    @ValueSource(classes = {BookMonograph.class, ReportResearch.class, DegreeBachelor.class})
    void shouldCreateResourceWithIsbnAsAdditionalIdentifierWhenInvalidIsbn(Class<?> publicationContext) {
        var invalidIsbn = randomString();
        var publication = randomPublicationWithIsbns(publicationContext, invalidIsbn);
        var context = (Book) publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(context.getIsbnList(), is(emptyIterable()));
        assertThat(context.getAdditionalIdentifiers(), hasItem(new AdditionalIdentifier("ISBN", invalidIsbn)));
    }

    @ParameterizedTest
    @ValueSource(classes = {BookMonograph.class, ReportResearch.class, DegreeBachelor.class})
    void shouldCreateResourceWithIsbnAsAdditionalIdentifierWhenInvalidIsbnAndIsbnWhenValid(
        Class<?> publicationContext) {
        var invalidIsbn = randomString();
        var validIsbn = randomIsbn13();
        var publication = randomPublicationWithIsbns(publicationContext, invalidIsbn, validIsbn);
        var context = (Book) publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(context.getIsbnList(), is(hasItem(validIsbn)));
        assertThat(context.getAdditionalIdentifiers(), hasItem(new AdditionalIdentifier("ISBN", invalidIsbn)));
    }

    @ParameterizedTest
    @ValueSource(classes = {BookMonograph.class, ReportResearch.class, DegreeBachelor.class})
    void shouldCreateResourceWithEmptyIsbnListAndAdditionalIdentifiersWhenNoIsbn(Class<?> publicationContext) {
        var publication = randomPublicationWithIsbns(publicationContext);
        var context = (Book) publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(context.getIsbnList(), is(emptyIterable()));
        assertThat(context.getAdditionalIdentifiers(), is(emptyIterable()));
    }

    private Publication randomPublicationWithIsbns(Class<?> publicationContext, String... isbn) {
        var publication = randomPublicationWithEmptyValues(publicationContext);
        var context = (Book) publication.getEntityDescription().getReference().getPublicationContext();
        var updatedContext = context.copy().withIsbnList(Arrays.stream(isbn).toList()).build();
        var reference = new Reference.Builder().withPublicationInstance(
                publication.getEntityDescription().getReference().getPublicationInstance())
                            .withPublishingContext(updatedContext)
                            .build();
        var updatedEntityDescription = publication.getEntityDescription().copy().withReference(reference).build();
        return publication.copy().withEntityDescription(updatedEntityDescription).build();
    }
}
