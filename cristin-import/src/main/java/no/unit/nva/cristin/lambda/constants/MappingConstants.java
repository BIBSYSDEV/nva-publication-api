package no.unit.nva.cristin.lambda.constants;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;

public final class MappingConstants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final Set<String>
        IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS = readAllIngnoredAndPossiblyEmptyFields();

    public static final Set<String> IGNORE_CONTRIBUTOR_FIELDS_ADDITIONALLY =
        readAllIngnoredAndPossiblyEmptyFieldsAndAddContributorsToList();

    public static final String NVA_API_DOMAIN = "https://" + readDomainName();

    public static final String CRISTIN_PATH = "cristin";
    public static final String PERSON_PATH = "person";
    public static final String ORGANIZATION_PATH = "organization";
    public static final String NVA_CHANNEL_REGISTRY_V2 = "publication-channels-v2";
    public static final String NSD_PROXY_PATH_PUBLISHER = "publisher";
    public static final String PATH_CUSTOMER = "customer";

    public static final Map<String, String> HRCS_CATEGORIES_MAP =
        Map.ofEntries(Map.entry("1", "HRCS_HC_01BLO"),
                      Map.entry("2", "HRCS_HC_10MEN"),
                      Map.entry("3", "HRCS_HC_11MET"),
                      Map.entry("4", "HRCS_HC_12MUS"),
                      Map.entry("5", "HRCS_HC_13NEU"),
                      Map.entry("6", "HRCS_HC_14ORA"),
                      Map.entry("7", "HRCS_HC_15REN"),
                      Map.entry("8", "HRCS_HC_16REP"),
                      Map.entry("9", "HRCS_HC_17RES"),
                      Map.entry("10", "HRCS_HC_18SKI"),
                      Map.entry("11", "HRCS_HC_19STR"),
                      Map.entry("12", "HRCS_HC_02CAN"),
                      Map.entry("13", "HRCS_HC_20GEN"),
                      Map.entry("14", "HRCS_HC_21OTH"),
                      Map.entry("15", "HRCS_HC_03CAR"),
                      Map.entry("16", "HRCS_HC_04CON"),
                      Map.entry("17", "HRCS_HC_05EAR"),
                      Map.entry("18", "HRCS_HC_06EYE"),
                      Map.entry("19", "HRCS_HC_07INF"),
                      Map.entry("20", "HRCS_HC_08IMM"),
                      Map.entry("21", "HRCS_HC_09INJ"));

    public static final Map<String, String> HRCS_ACTIVITIES_MAP =
        Map.ofEntries(Map.entry("1", "HRCS_RAG_1"),
                      Map.entry("1.1", "HRCS_RA_1_1"),
                      Map.entry("1.2", "HRCS_RA_1_2"),
                      Map.entry("1.3", "HRCS_RA_1_3"),
                      Map.entry("1.4", "HRCS_RA_1_4"),
                      Map.entry("1.5", "HRCS_RA_1_5"),
                      Map.entry("2", "HRCS_RAG_2"),
                      Map.entry("2.1", "HRCS_RA_2_1"),
                      Map.entry("2.2", "HRCS_RA_2_2"),
                      Map.entry("2.3", "HRCS_RA_2_3"),
                      Map.entry("2.4", "HRCS_RA_2_4"),
                      Map.entry("2.5", "HRCS_RA_2_5"),
                      Map.entry("2.6", "HRCS_RA_2_6"),
                      Map.entry("3", "HRCS_RAG_3"),
                      Map.entry("3.1", "HRCS_RA_3_1"),
                      Map.entry("3.2", "HRCS_RA_3_2"),
                      Map.entry("3.3", "HRCS_RA_3_3"),
                      Map.entry("3.4", "HRCS_RA_3_4"),
                      Map.entry("3.5", "HRCS_RA_3_5"),
                      Map.entry("4", "HRCS_RAG_4"),
                      Map.entry("4.1", "HRCS_RA_4_1"),
                      Map.entry("4.2", "HRCS_RA_4_2"),
                      Map.entry("4.3", "HRCS_RA_4_3"),
                      Map.entry("4.4", "HRCS_RA_4_4"),
                      Map.entry("4.5", "HRCS_RA_4_5"),
                      Map.entry("5", "HRCS_RAG_5"),
                      Map.entry("5.1", "HRCS_RA_5_1"),
                      Map.entry("5.2", "HRCS_RA_5_2"),
                      Map.entry("5.3", "HRCS_RA_5_3"),
                      Map.entry("5.4", "HRCS_RA_5_4"),
                      Map.entry("5.5", "HRCS_RA_5_5"),
                      Map.entry("5.6", "HRCS_RA_5_6"),
                      Map.entry("5.7", "HRCS_RA_5_7"),
                      Map.entry("5.8", "HRCS_RA_5_8"),
                      Map.entry("5.9", "HRCS_RA_5_9"),
                      Map.entry("6", "HRCS_RAG_6"),
                      Map.entry("6.1", "HRCS_RA_6_1"),
                      Map.entry("6.2", "HRCS_RA_6_2"),
                      Map.entry("6.3", "HRCS_RA_6_3"),
                      Map.entry("6.4", "HRCS_RA_6_4"),
                      Map.entry("6.5", "HRCS_RA_6_5"),
                      Map.entry("6.6", "HRCS_RA_6_6"),
                      Map.entry("6.7", "HRCS_RA_6_7"),
                      Map.entry("6.8", "HRCS_RA_6_8"),
                      Map.entry("6.9", "HRCS_RA_6_9"),
                      Map.entry("7", "HRCS_RAG_7"),
                      Map.entry("7.1", "HRCS_RA_7_1"),
                      Map.entry("7.2", "HRCS_RA_7_2"),
                      Map.entry("7.3", "HRCS_RA_7_3"),
                      Map.entry("7.4", "HRCS_RA_7_4"),
                      Map.entry("8", "HRCS_RAG_8"),
                      Map.entry("8.1", "HRCS_RA_8_1"),
                      Map.entry("8.2", "HRCS_RA_8_2"),
                      Map.entry("8.3", "HRCS_RA_8_3"),
                      Map.entry("8.4", "HRCS_RA_8_4"),
                      Map.entry("8.5", "HRCS_RA_8_5"));

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

    private static Set<String> readAllIngnoredAndPossiblyEmptyFieldsAndAddContributorsToList() {
        var result = readAllIngnoredAndPossiblyEmptyFields();
        result.add("entityDescription.contributors");
        return result;
    }

    private static String ignoredFieldsFile() {
        return ENVIRONMENT.readEnvOpt("IGNORED_FIELDS_FILE").orElse("ignoredFields.txt");
    }
}
