package no.unit.nva.cristin.lambda.constants;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import no.unit.nva.testutils.IoUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public final class MappingConstants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final boolean SHOULD_CREATE_CONTRIBUTOR_ID = createCristinContributorId();
    public static final URI CRISTIN_PERSONS_URI = URI.create("https://api.cristin.no/person/");
    public static final URI CRISTIN_ORG_URI = readCristinOrgUriFromEnvOrDefault();
    public static final Set<String>
        IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS = readAllIngnoredAndPossiblyEmptyFields();

    public static final String NVA_API_DOMAIN = "https://api." + readDomainName();
    public static final String NSD_PROXY_PATH = "publication-channels";
    public static final String NSD_PROXY_PATH_JOURNAL = "journal";

    private MappingConstants() {

    }

    private static String readDomainName() {
        return ENVIRONMENT.readEnv("DOMAIN_NAME");
    }

    private static Set<String> readAllIngnoredAndPossiblyEmptyFields() {
        Set<String> result = new HashSet<>(Set.copyOf(IoUtils.linesfromResource(Path.of(ignoredFieldsFile()))));
        result.addAll(Set.copyOf(IoUtils.linesfromResource(Path.of("possiblyEmptyFields.txt"))));
        return result;
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
