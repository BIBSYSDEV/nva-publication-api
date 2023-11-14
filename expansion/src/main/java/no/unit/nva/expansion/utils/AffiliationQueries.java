package no.unit.nva.expansion.utils;

import nva.commons.core.ioutils.IoUtils;

import java.nio.file.Path;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class AffiliationQueries {

    public static final String TOP_LEVEL_AFFILIATION =
            IoUtils.stringFromResources(Path.of("topLevelAffiliationQuery.sparql"));

    public static final String HAS_PART =
        IoUtils.stringFromResources(Path.of("hasPart.sparql"));

    private AffiliationQueries() {
    }
}
