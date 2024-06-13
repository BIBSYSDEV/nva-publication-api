package no.sikt.nva.brage.migration.lambda;

import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator;
import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator.Builder;
import no.unit.nva.model.Publication;

public class BrageTestRecord {

    private final NvaBrageMigrationDataGenerator.Builder generatorBuilder;
    private final Publication existingPublication;

    public Builder getGeneratorBuilder() {
        return generatorBuilder;
    }

    public Publication getExistingPublication() {
        return existingPublication;
    }

    public BrageTestRecord(Builder generatorBuilder, Publication existingPublication) {
        this.generatorBuilder = generatorBuilder;
        this.existingPublication = existingPublication;
    }
}