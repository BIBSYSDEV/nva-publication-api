package no.unit.nva.publication.utils;

import java.util.Optional;
import java.util.List;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import no.unit.nva.model.EntityDescription;
import nva.commons.core.Environment;
import software.amazon.awssdk.services.s3.S3Client;

public final class CuratingInstitutionMigration {
    private static final String CRISTIN_UNITS_S3_URI_ENV = "CRISTIN_UNITS_S3_URI";

    private CuratingInstitutionMigration() {
    }

    public static void migrate(Resource dataEntry, S3Client s3Client, Environment environment) {
        if (dataEntry.getCuratingInstitutions().isEmpty() && hasContributors(dataEntry)) {
            dataEntry.setCuratingInstitutions(
                CuratingInstitutionsUtil.getCuratingInstitutionsCached(
                    dataEntry.toPublication().getEntityDescription(),
                    new CristinUnitsUtil(s3Client, environment.readEnv(CRISTIN_UNITS_S3_URI_ENV))));
        }
    }

    private static boolean hasContributors(Resource dataEntry) {
        return !Optional.of(dataEntry.getEntityDescription()).map(
            EntityDescription::getContributors).map(List::isEmpty).orElse(true);
    }
}
