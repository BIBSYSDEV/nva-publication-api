package no.unit.nva.model.associatedartifacts.file;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AssociatedArtifactsMigrationTest {

    public static Stream<Arguments> migratingFilesJsonValuesSupplier() {
        return Stream.of(
            Arguments.of(Named.of("PublishedFile migrated to OpenFile", PublishedFile.TYPE), OpenFile.class),
            Arguments.of(Named.of("UnpublishedFile migrated to PendingOpenFile", UnpublishedFile.TYPE),
                         PendingOpenFile.class),
            Arguments.of(Named.of("UnpublishableFile migrated to InternalFile", AdministrativeAgreement.TYPE),
                         InternalFile.class));
    }

    public static Stream<Arguments> migratingFilesObjectSupplier() {
        return Stream.of(Arguments.of(File.builder().buildPublishedFile(), PublishedFile.class, OpenFile.class),
                         Arguments.of(File.builder().buildUnpublishedFile(), UnpublishedFile.class,
                                      PendingOpenFile.class),
                         Arguments.of(File.builder().buildUnpublishableFile(), AdministrativeAgreement.class,
                                      InternalFile.class));
    }

    @ParameterizedTest
    @MethodSource("migratingFilesJsonValuesSupplier")
    void shouldMigrateFileFromJsonValues(String initialFileType, Class<? extends File> fileTypeAfterMigration)
        throws JsonProcessingException {
        var file = fileType(initialFileType);

        var roundTrippedFile = JsonUtils.dtoObjectMapper.readValue(file, File.class);

        assertInstanceOf(fileTypeAfterMigration, roundTrippedFile);
    }

    @ParameterizedTest
    @MethodSource("migratingFilesObjectSupplier")
    void shouldMigrateFilesFromObjects(File file, Class<? extends File> oldType, Class<? extends File> newType)
        throws JsonProcessingException {
        assertInstanceOf(oldType, file);

        var roundTrippedFile = JsonUtils.dtoObjectMapper.readValue(file.toJsonString(), File.class);

        assertInstanceOf(newType, roundTrippedFile);
    }

    private String fileType(String value) {
        return """
            {
              "type" : "%s",
              "identifier" : "8a8c04c7-cefa-45d0-b351-4441c0666493"
            }
            """.formatted(value);
    }
}
