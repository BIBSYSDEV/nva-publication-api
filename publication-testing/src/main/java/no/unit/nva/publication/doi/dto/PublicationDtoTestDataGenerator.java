package no.unit.nva.publication.doi.dto;

import no.unit.nva.publication.doi.PublicationMapper;

public final class PublicationDtoTestDataGenerator {

    private static final String EXAMPLE_NAMESPACE = "http://example.net/namespace/";
    public static final PublicationDynamoEventTestDataGenerator PUBLICATION_GENERATOR =
        new PublicationDynamoEventTestDataGenerator();

    private PublicationDtoTestDataGenerator() {
    }

    /**
     * Creates a Publication object populated with random values.
     *
     * @return  publication
     */
    public static Publication createPublication() {
        PublicationMapper mapper = new PublicationMapper(EXAMPLE_NAMESPACE);
        return mapper.fromDynamodbStreamRecord(
            PUBLICATION_GENERATOR.createRandomStreamRecord().asDynamoDbEvent().getRecords().get(0))
            .getNewPublication()
            .get();
    }

}
