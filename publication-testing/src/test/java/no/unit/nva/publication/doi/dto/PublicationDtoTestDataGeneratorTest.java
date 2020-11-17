package no.unit.nva.publication.doi.dto;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;


import org.junit.jupiter.api.Test;

public class PublicationDtoTestDataGeneratorTest {

    @Test
    public void createPublicationReturnsPopulatedPublication() {
        Publication publication = PublicationDtoTestDataGenerator.createPublication();
        assertThat(publication, doesNotHaveNullOrEmptyFields());
    }

}
