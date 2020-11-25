package no.unit.nva.publication.doi.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import no.unit.nva.publication.doi.PublicationMapper;
import org.junit.jupiter.api.Test;

class PublicationHolderTest {

    public static final PublicationDynamoEventTestDataGenerator PUBLICATION_GENERATOR =
        new PublicationDynamoEventTestDataGenerator();
    private static final String EXAMPLE_TYPE = "exampleType";
    private static final String EXAMPLE_NAMESPACE = "http://example.net/namespace/";

    @Test
    void testCreatePublicationHolderWithExampleTypeThenGetTypeReturnsExampleType() {
        assertThat(new PublicationHolder(EXAMPLE_TYPE, null).getType(),
            is(equalTo(EXAMPLE_TYPE)));
    }

    @Test
    void createPublicationHolderWithItemThenHasItem() {
        assertThat(new PublicationHolder(EXAMPLE_TYPE, createPublication()).getItem(), notNullValue());
    }

    private Publication createPublication() {
        PublicationMapper mapper = new PublicationMapper(EXAMPLE_NAMESPACE);
        return mapper.fromDynamodbStreamRecord(
            PUBLICATION_GENERATOR.createRandomStreamRecord().asDynamoDbEvent().getRecords().get(0))
            .getNewPublication()
            .get();
    }
}