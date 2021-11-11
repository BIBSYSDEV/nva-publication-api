package no.unit.nva.publication;

import static no.unit.nva.publication.RandomUtils.randomBoolean;
import static no.unit.nva.publication.RandomUtils.randomLabels;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.util.List;
import java.util.UUID;

import no.unit.nva.file.model.File;
import no.unit.nva.file.model.FileSet;
import no.unit.nva.file.model.License;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class FileSetGenerator {

    public static FileSet randomFileSet() {
        return new FileSet(randomFiles());
    }

    private static List<File> randomFiles() {
        return List.of(randomFile());
    }

    private static File randomFile() {
        return new File.Builder()
                   .withIdentifier(UUID.randomUUID())
                   .withLicense(randomLicense())
                   .withName(randomString())
                   .withAdministrativeAgreement(randomBoolean())
                   .withEmbargoDate(randomInstant())
                   .withMimeType(randomString())
                   .withSize(randomInteger().longValue())
                   .build();
    }

    private static License randomLicense() {
        return new License.Builder()
                   .withIdentifier(randomLicenseIdentifier())
                   .withLabels(randomLabels())
                   .withLink(randomUri())
                   .build();
    }

    private static String randomLicenseIdentifier() {
        return randomString();
    }
}
