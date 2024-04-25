package no.unit.nva.publication.utils;

import java.util.Optional;
import java.util.List;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import no.unit.nva.model.EntityDescription;
import software.amazon.awssdk.services.s3.S3Client;

public final class CuratingInstitutionMigration {
    private CuratingInstitutionMigration() {
    }

    public static void migrate(Resource dataEntry, S3Client s3Client, String cristinUnitsS3Uri) {
        if (dataEntry.getCuratingInstitutions().isEmpty() && hasContributors(dataEntry)) {
            dataEntry.setCuratingInstitutions(
                CuratingInstitutionsUtil.getCuratingInstitutionsCached(
                    dataEntry.toPublication().getEntityDescription(),
                    new CristinUnitsUtil(s3Client, cristinUnitsS3Uri)));
        }
    }

    private static boolean hasContributors(Resource dataEntry) {
        return !Optional.of(dataEntry.getEntityDescription()).map(
            EntityDescription::getContributors).map(List::isEmpty).orElse(true);
    }
}
