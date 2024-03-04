package no.unit.nva.expansion.utils;

import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class AffiliationQueries {

    public static final String TOP_LEVEL_ORGANIZATION =
        IoUtils.stringFromResources(Path.of("constructTopLevelAffiliationQuery.sparql"));

    public static final String HAS_PART =
        IoUtils.stringFromResources(Path.of("constructHasPartAsInverseOfPartOf.sparql"));

    public static final String CONTRIBUTOR_ORGANIZATION =
        IoUtils.stringFromResources(Path.of("constructContributorOrganization.sparql"));

    private AffiliationQueries() {
    }
}
