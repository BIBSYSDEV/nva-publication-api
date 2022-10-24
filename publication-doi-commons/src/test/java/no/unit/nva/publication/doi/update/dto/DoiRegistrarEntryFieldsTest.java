package no.unit.nva.publication.doi.update.dto;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.testing.PublicationGenerator;
import org.junit.jupiter.api.Test;

public class DoiRegistrarEntryFieldsTest {
    
    public static final String SOME_OTHER_TITLE = "someOtherTitle";
    public static final URI SOME_DOI = URI.create("https://doi.org/10.000/123");
    
    @Test
    public void fromPublicationCreatesObjectWithNonEmptyValuesWhenPublicationContainsAllNecessaryInformation() {
        Publication publication = createSamplePublication();
        var doiRegistrarEntryFields = DoiRegistrarEntryFields.fromPublication(publication);
        assertThat(doiRegistrarEntryFields, doesNotHaveEmptyValues());
    }
    
    @Test
    public void equalsReturnsTrueForTwoInstancesWithTheSameData() {
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        var firstInstance = DoiRegistrarEntryFields.fromPublication(publication);
        var secondInstance = DoiRegistrarEntryFields.fromPublication(publication);
        assertThat(firstInstance, is(not(sameInstance(secondInstance))));
        assertThat(firstInstance, is(equalTo(secondInstance)));
    }
    
    @Test
    public void equalsReturnsFalseForTwoInstancesWithDifferentData() {
        Publication publication1 = PublicationGenerator.publicationWithIdentifier();
        Publication publication2 = alterPublication(publication1);
        var firstInstance = DoiRegistrarEntryFields.fromPublication(publication1);
        var secondInstance = DoiRegistrarEntryFields.fromPublication(publication2);
        assertThat(firstInstance, is(not(equalTo(secondInstance))));
    }
    
    @Test
    public void equalsReturnsTrueForTwoInstancesWithEqualNonNullValuesAndCommonNullValues() {
        Publication publication1 = new Publication.Builder().withIdentifier(SortableIdentifier.next()).build();
        var firstInstance = DoiRegistrarEntryFields.fromPublication(publication1);
        var secondInstance = DoiRegistrarEntryFields.fromPublication(publication1);
        assertThat(firstInstance, is(equalTo(secondInstance)));
    }
    
    private Publication createSamplePublication() {
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        publication.setDoi(SOME_DOI);
        return publication;
    }
    
    private Publication alterPublication(Publication publication) {
        EntityDescription.Builder entityDescriptionCopy = copyEntityDescription(publication);
        EntityDescription alteredEntityDescription = entityDescriptionCopy.withMainTitle(SOME_OTHER_TITLE).build();
        return attempt(() -> publication
                   .copy()
                   .withEntityDescription(alteredEntityDescription)
                   .build()).orElseThrow();
    }
    
    private EntityDescription.Builder copyEntityDescription(Publication publication) {
        EntityDescription entityDescription = publication.getEntityDescription();
        Reference reference = new Reference.Builder()
                                  .withPublicationInstance(entityDescription.getReference().getPublicationInstance())
                                  .build();
        return new EntityDescription.Builder()
                   .withMainTitle(entityDescription.getMainTitle())
                   .withContributors(entityDescription.getContributors())
                   .withReference(reference)
                   .withDate(entityDescription.getDate());
    }
}