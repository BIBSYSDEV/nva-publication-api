package no.unit.nva.publication.doi.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import java.util.Collections;
import java.util.List;
import no.unit.nva.publication.doi.PublicationMapper;
import org.junit.jupiter.api.Test;

class PublicationCollectionTest {

    private static final String EXAMPLE_TYPE = "exampleType";
    public static final PublicationDynamoEventTestDataGenerator PUBLICATION_GENERATOR =
        new PublicationDynamoEventTestDataGenerator();
    private static final String EXAMPLE_NAMESPACE = "http://example.net/namespace/";

    @Test
    void testCreatePublicationCollectionWithExampleTypeThenGetTypeReturnsExampleType() {
        assertThat(new PublicationCollection(EXAMPLE_TYPE, Collections.emptyList()).getType(),
            is(equalTo(EXAMPLE_TYPE)));
    }

    @Test
    void createPublicationCollectionWithOneItemThenSizeOfGetItemsIsOne() {
        assertThat(new PublicationCollection(EXAMPLE_TYPE, createPublicationCollection()).getItems(), hasSize(1));
    }

    private List<Publication> createPublicationCollection() {
        PublicationMapper mapper = new PublicationMapper(EXAMPLE_NAMESPACE);
        return List.of(mapper.fromDynamodbStreamRecord(
            PUBLICATION_GENERATOR.createRandomStreamRecord().asDynamoDbEvent().getRecords().get(0))
            .getNewPublication()
            .get());
    }
}