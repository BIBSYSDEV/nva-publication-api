package no.unit.nva.publication.doi.dto;

import no.unit.nva.publication.doi.PublicationMapper;

public class PublicationDtoTestDataGenerator {

    private static final String EXAMPLE_NAMESPACE = "http://example.net/namespace/";
    private final PublicationDynamoEventTestDataGenerator generator;
    private final PublicationMapper mapper;

    /**
     * Default constructor for PublicationDynamoEventTestDataGenerator.
     */
    public PublicationDtoTestDataGenerator() {
        this.generator = new PublicationDynamoEventTestDataGenerator();
        this.mapper = new PublicationMapper(EXAMPLE_NAMESPACE);
    }

    /**
     * Creates a stream record filled by faker.
     * @return PublicationDtoTestDataGenerator with one additional stream record added to list of records.
     */
    public PublicationDtoTestDataGenerator createRandomStreamRecord() {
        generator.createRandomStreamRecord(null);
        return this;
    }

    /**
     * Clear the added stream records.
     */
    public void clearRecords() {
        generator.clearRecords();
    }

    /**
     * Provides a Publication object.
     *
     * @return  publication
     */
    public Publication asPublicationDto() {
        return mapper.fromDynamodbStreamRecord(
            generator.createRandomStreamRecord().asDynamoDbEvent().getRecords().get(0))
            .getNewPublication()
            .get();
    }

}
