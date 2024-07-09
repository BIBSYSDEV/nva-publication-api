package no.unit.nva;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.associatedartifacts.file.File;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class LicenseMigrationTest {

    public static Stream<String> fileWithLicenseProvider() {
        return Stream.of(generateOldPublishedFile(),generateOldUnpublishedFile(),
                         generateOldUnpublishableFile(),  generateNewFile());
    }

    @ParameterizedTest(name = "should accept legacy and current formatting for License")
    @MethodSource("fileWithLicenseProvider")
    void shouldMigrateLicense(String file) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(file, File.class));
    }

    private static String generateNewFile() {
        return "{\n"
               + "    \"type\" : \"PublishedFile\",\n"
               + "    \"identifier\" : \"d9fc5844-f1a3-491b-825a-5a4cabc12aa2\",\n"
               + "    \"name\" : \"Per Magne Østertun.pdf\",\n"
               + "    \"mimeType\" : \"application/pdf\",\n"
               + "    \"size\" : 1025817,\n"
               + "    \"license\" : \"https://creativecommons.org/licenses/by-nc/2.0/\",\n"
               + "    \"administrativeAgreement\" : false,\n"
               + "    \"publisherAuthority\" : false,\n"
               + "    \"publishedDate\" : \"2023-05-25T19:31:17.302914Z\",\n"
               + "    \"visibleForNonOwner\" : true\n"
               + "  }";
    }

    private static String generateOldPublishedFile() {
        return "{\n"
               + "    \"type\" : \"PublishedFile\",\n"
               + "    \"identifier\" : \"d9fc5844-f1a3-491b-825a-5a4cabc12aa2\",\n"
               + "    \"name\" : \"Per Magne Østertun.pdf\",\n"
               + "    \"mimeType\" : \"application/pdf\",\n"
               + "    \"size\" : 1025817,\n"
               + "    \"license\" : {\n"
               + "      \"type\" : \"License\",\n"
               + "      \"identifier\" : \"CC BY-NC\",\n"
               + "      \"labels\" : {\n"
               + "        \"nb\" : \"CC BY-NC\"\n"
               + "      }\n"
               + "    },\n"
               + "    \"administrativeAgreement\" : false,\n"
               + "    \"publisherAuthority\" : false,\n"
               + "    \"publishedDate\" : \"2023-05-25T19:31:17.302914Z\",\n"
               + "    \"visibleForNonOwner\" : true\n"
               + "  }";
    }

    private static String generateOldUnpublishedFile() {
        return "{\n"
               + "    \"type\" : \"UnpublishedFile\",\n"
               + "    \"identifier\" : \"d9fc5844-f1a3-491b-825a-5a4cabc12aa2\",\n"
               + "    \"name\" : \"Per Magne Østertun.pdf\",\n"
               + "    \"mimeType\" : \"application/pdf\",\n"
               + "    \"size\" : 1025817,\n"
               + "    \"license\" : {\n"
               + "      \"type\" : \"License\",\n"
               + "      \"identifier\" : \"CC BY-NC\",\n"
               + "      \"labels\" : {\n"
               + "        \"nb\" : \"CC BY-NC\"\n"
               + "      }\n"
               + "    },\n"
               + "    \"administrativeAgreement\" : false,\n"
               + "    \"publisherAuthority\" : true,\n"
               + "    \"publishedDate\" : \"2023-05-25T19:31:17.302914Z\",\n"
               + "    \"visibleForNonOwner\" : false\n"
               + "  }";
    }

    private static String generateOldUnpublishableFile() {
        return "{\n"
               + "    \"type\" : \"UnpublishableFile\",\n"
               + "    \"identifier\" : \"d9fc5844-f1a3-491b-825a-5a4cabc12aa2\",\n"
               + "    \"name\" : \"Per Magne Østertun.pdf\",\n"
               + "    \"mimeType\" : \"application/pdf\",\n"
               + "    \"size\" : 1025817,\n"
               + "    \"license\" : {\n"
               + "      \"type\" : \"License\",\n"
               + "      \"identifier\" : \"CC BY-NC\",\n"
               + "      \"labels\" : {\n"
               + "        \"nb\" : \"CC BY-NC\"\n"
               + "      }\n"
               + "    },\n"
               + "    \"administrativeAgreement\" : true,\n"
               + "    \"publisherAuthority\" : true,\n"
               + "    \"publishedDate\" : \"2023-05-25T19:31:17.302914Z\",\n"
               + "    \"visibleForNonOwner\" : false\n"
               + "  }";
    }
}
