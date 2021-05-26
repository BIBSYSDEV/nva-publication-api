package no.unit.nva.cristin.lambda.constants;

import java.net.URI;
import java.nio.file.Path;
import java.util.Set;
import no.unit.nva.testutils.IoUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public final class MappingConstants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final boolean SHOULD_CREATE_CONTRIBUTOR_ID = createCristinContributorId();
    public static final URI CRISTIN_PERSONS_URI = URI.create("https://api.cristin.no/person/");
    public static final URI CRISTIN_ORG_URI = readCristinOrgUriFromEnvOrDefault();
    public static final Set<String> IGNORED_PUBLICATION_FIELDS = readIgnoredFields();

    private MappingConstants() {

    }

    private static Set<String> readIgnoredFields() {
        return Set.copyOf(IoUtils.linesfromResource(Path.of(ignoredFieldsFile())));
    }

    private static String ignoredFieldsFile() {
        return ENVIRONMENT.readEnvOpt("IGNORED_FIELDS_FILE").orElse("ignoredFields.txt");
    }

    private static URI readCristinOrgUriFromEnvOrDefault() {
        String defaultUriForReferencingCristinOrgs = "https://api.cristin.no/v2/units/";
        return ENVIRONMENT.readEnvOpt("CRISTIN_ORG_URI")
                   .map(URI::create)
                   .orElse(URI.create(defaultUriForReferencingCristinOrgs));
    }

    @JacocoGenerated
    private static boolean createCristinContributorId() {
        return ENVIRONMENT.readEnvOpt("CREATE_CONTRIBUTOR_ID")
                   .map(Boolean::parseBoolean)
                   .orElse(false);
    }
}
