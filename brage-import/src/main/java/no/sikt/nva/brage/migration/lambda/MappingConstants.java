package no.sikt.nva.brage.migration.lambda;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;

public final class MappingConstants {

    public static final Environment ENVIRONMENT = new Environment();

    public static final Set<String>
        IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS = readAllIngnoredAndPossiblyEmptyFields();

    private static Set<String> readAllIngnoredAndPossiblyEmptyFields() {
        Set<String> result = new HashSet<>(Set.copyOf(IoUtils.linesfromResource(Path.of(ignoredFieldsFile()))));
        result.addAll(Set.copyOf(IoUtils.linesfromResource(Path.of("possiblyEmptyFields.txt"))));
        return result;
    }

    private static String ignoredFieldsFile() {
        return ENVIRONMENT.readEnvOpt("IGNORED_FIELDS_FILE").orElse("ignoredFields.txt");
    }
}
