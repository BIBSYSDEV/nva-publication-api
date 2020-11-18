package no.unit.nva.publication.doi.dto;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;


import org.junit.jupiter.api.Test;

public class PublicationDtoTestDataGeneratorTest {

    @Test
    public void createPublicationReturnsPopulatedPublication() {
        PublicationDtoTestDataGenerator generator = new PublicationDtoTestDataGenerator();
        Publication publication = generator.createRandomStreamRecord().asPublicationDto();
        assertThat(publication, doesNotHaveNullOrEmptyFields());
        generator.clearRecords();
    }

}
