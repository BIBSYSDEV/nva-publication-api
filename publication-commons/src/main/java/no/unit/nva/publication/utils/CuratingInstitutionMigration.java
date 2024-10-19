package no.unit.nva.publication.utils;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import software.amazon.awssdk.services.s3.S3Client;

public final class CuratingInstitutionMigration {
    private CuratingInstitutionMigration() {
        // NO-OP
    }

    public static void migrate(Resource dataEntry, S3Client s3Client, String cristinUnitsS3Uri) {
        if (curatingInstitutionsDoesNotContainContributors(dataEntry)
            && hasContributors(dataEntry)) {
            dataEntry.setCuratingInstitutions(
                CuratingInstitutionsUtil.getCuratingInstitutionsCached(
                    dataEntry.toPublication().getEntityDescription(),
                    new CristinUnitsUtil(s3Client, cristinUnitsS3Uri)));
        }
    }

    private static boolean curatingInstitutionsDoesNotContainContributors(Resource dataEntry) {
        return dataEntry.getCuratingInstitutions().stream()
                   .filter(Objects::nonNull)
                   .map(CuratingInstitution::contributorCristinIds)
                   .filter(Objects::nonNull)
                   .flatMap(Collection::stream)
                   .toList().isEmpty();
    }

    private static boolean hasContributors(Resource dataEntry) {
        return Optional.ofNullable(dataEntry.getEntityDescription())
                    .map(EntityDescription::getContributors)
                    .map(list -> !list.isEmpty())
                    .orElse(false);
    }
}