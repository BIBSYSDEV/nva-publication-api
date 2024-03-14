package no.unit.nva.publication.service.impl;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import no.unit.nva.publication.utils.CristinUnitsUtil;

public class CuratingInstitutionMigration {

    private CuratingInstitutionMigration() {
    }

    public static final URI CRISTIN_UNIT_API_URI = URI.create("https://api.cristin.no/v2/units");

    public static void migrate(Resource dataEntry) {
        if (dataEntry.getCuratingInstitutions().isEmpty() && hasContributors(dataEntry)) {
            dataEntry.setCuratingInstitutions(
                new CuratingInstitutionsUtil(
                    (uri, uriRetriever) -> new CristinUnitsUtil(uriRetriever, CRISTIN_UNIT_API_URI).getTopLevel(
                        uri)).getCuratingInstitutions(dataEntry.toPublication(), UriRetriever.defaultUriRetriever()));
        }
    }

    private static boolean hasContributors(Resource dataEntry) {
        return !Optional.of(dataEntry.getEntityDescription()).map(
            EntityDescription::getContributors).map(List::isEmpty).orElse(true);
    }
}
