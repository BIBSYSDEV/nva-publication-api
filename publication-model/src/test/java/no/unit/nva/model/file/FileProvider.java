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
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingInternalFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.RejectedFile;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.reflections.Reflections;

public class FileProvider implements ArgumentsProvider {

    public static final URI LICENSE_URI = URI.create("http://creativecommons.org/licenses/by/4.0/");
    public static final boolean NOT_ADMINISTRATIVE_AGREEMENT = false;

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
            case PublishedFile.TYPE -> randomPublishedFile();
            case UnpublishedFile.TYPE -> randomUnpublishedFile();
            case "AdministrativeAgreement" -> randomUnpublishableFile();
            case OpenFile.TYPE -> randomOpenFile();
            case PendingOpenFile.TYPE -> randomPendingOpenFile();
            case RejectedFile.TYPE -> randomRejectedFile();
            case InternalFile.TYPE -> randomInternalFile();
            case PendingInternalFile.TYPE -> randomPendingInternalFile();
            default -> throw new IllegalArgumentException(
                "Unexpected value, make sure to include new types here: " + aClass.getSimpleName());
        };
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

    public static File randomUnpublishableFile() {
        return new AdministrativeAgreement(UUID.randomUUID(), randomString(), randomString(),
                                           randomInteger().longValue(), LICENSE_URI, randomBoolean(),
                                           PublisherVersion.ACCEPTED_VERSION, randomInstant(),
                                           randomInserted());
    }

    private static Username randomUsername() {
        return new Username(randomInteger().toString() + "@" + randomString());
    }

    public static File randomUnpublishedFile() {
        return buildNonAdministrativeAgreement().buildUnpublishedFile();
    }

    public static File randomPublishedFile() {
        return buildNonAdministrativeAgreement().buildPublishedFile();
    }

    private static UploadDetails randomInserted() {
        return new UserUploadDetails(randomUsername(), randomInstant());
    }

    public static File.Builder buildNonAdministrativeAgreement() {
        return File.builder()
                   .withName(randomString())
                   .withAdministrativeAgreement(NOT_ADMINISTRATIVE_AGREEMENT)
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