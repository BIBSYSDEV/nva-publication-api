package no.unit.nva.model.file;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingInternalFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.RejectedFile;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.reflections.Reflections;

public class FileProvider implements ArgumentsProvider {

    public static final URI LICENSE_URI = URI.create("http://creativecommons.org/licenses/by/4.0/");

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        var reflections = new Reflections(File.class.getPackageName());
        var classes = reflections.getSubTypesOf(File.class);

        var output = new ArrayList<File>();
        for (var aClass : classes) {
            if (File.class.isAssignableFrom(aClass) && !Modifier.isAbstract(aClass.getModifiers())) {
                output.add(resolveFileFromType(aClass));
            }
        }

        return output.stream().map(file -> Arguments.of(Named.of(file.getClass().getSimpleName(), file)));
    }

    private static File resolveFileFromType(Class<?> aClass) {
        return switch (aClass.getSimpleName()) {
            case OpenFile.TYPE -> randomOpenFile();
            case PendingOpenFile.TYPE -> randomPendingOpenFile();
            case RejectedFile.TYPE -> randomRejectedFile();
            case InternalFile.TYPE -> randomInternalFile();
            case PendingInternalFile.TYPE -> randomPendingInternalFile();
            case HiddenFile.TYPE -> randomHiddenFile();
            case UploadedFile.TYPE -> randomUploadedFile();
            default -> throw new IllegalArgumentException(
                "Unexpected value, make sure to include new types here: " + aClass.getSimpleName());
        };
    }

    private static File randomHiddenFile() {
        return buildNonAdministrativeAgreement().buildHiddenFile();
    }

    private static File randomUploadedFile() {
        return new UploadedFile(UUID.randomUUID(), randomString(), randomString(), randomInteger().longValue(),
                                randomInserted());
    }

    private static File randomOpenFile() {
        return buildNonAdministrativeAgreement().buildOpenFile();
    }

    private static File randomPendingOpenFile() {
        return buildNonAdministrativeAgreement().buildPendingOpenFile();
    }

    private static File randomRejectedFile() {
        return buildNonAdministrativeAgreement().buildRejectedFile();
    }

    private static File randomInternalFile() {
        return buildNonAdministrativeAgreement().buildInternalFile();
    }

    private static File randomPendingInternalFile() {
        return buildNonAdministrativeAgreement().buildPendingInternalFile();
    }

    private static Username randomUsername() {
        return new Username(randomInteger().toString() + "@" + randomString());
    }

    private static UploadDetails randomInserted() {
        return new UserUploadDetails(randomUsername(), randomInstant());
    }

    public static File.Builder buildNonAdministrativeAgreement() {
        return File.builder()
                   .withName(randomString())
                   .withMimeType(randomString())
                   .withSize(randomInteger().longValue())
                   .withEmbargoDate(randomInstant())
                   .withLicense(LICENSE_URI)
                   .withIdentifier(UUID.randomUUID())
                   .withUploadDetails(randomInserted())
                   .withPublisherVersion(randomPublisherVersion());
    }

    private static PublisherVersion randomPublisherVersion() {
        return randomBoolean() ? PublisherVersion.PUBLISHED_VERSION : PublisherVersion.ACCEPTED_VERSION;
    }
}
