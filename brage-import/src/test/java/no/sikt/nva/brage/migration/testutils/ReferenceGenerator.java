package no.sikt.nva.brage.migration.testutils;

import static java.util.Objects.isNull;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import java.util.Optional;
import no.sikt.nva.brage.migration.record.Type;
import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator.Builder;
import no.sikt.nva.brage.migration.testutils.type.NvaType;
import no.unit.nva.model.Reference;

public final class ReferenceGenerator {

    public static Reference generateReference(Builder builder) {
        return Optional.ofNullable(builder.getType())
                   .map(ReferenceGenerator::buildReference)
                   .orElse(new Reference());
    }

    private static Reference buildReference(Type type) {
        if (NvaType.CHAPTER.getValue().equals(type.getNva())) {
            return new Reference.Builder()
                       .withDoi(randomDoi())
                       .build();
        }
        return new Reference.Builder().build();
    }
}
