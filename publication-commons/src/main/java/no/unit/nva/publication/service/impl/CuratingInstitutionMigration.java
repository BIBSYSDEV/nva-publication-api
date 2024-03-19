package no.unit.nva.publication.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import nva.commons.core.Environment;

public class CuratingInstitutionMigration {

    private CuratingInstitutionMigration() {
    }


    public static void migrate(Resource dataEntry, HttpClient httpClient, Environment environment) {
        if (dataEntry.getCuratingInstitutions().isEmpty() && hasContributors(dataEntry)) {
            dataEntry.setCuratingInstitutions(
                CuratingInstitutionsUtil.defaultCuratingInstitutionsUtilWitchLargeCache(httpClient, environment).getCuratingInstitutions(
                    dataEntry.toPublication(), UriRetriever.defaultUriRetriever()));
        }
    }

    private static boolean hasContributors(Resource dataEntry) {
        return !Optional.of(dataEntry.getEntityDescription()).map(
            EntityDescription::getContributors).map(List::isEmpty).orElse(true);
    }
}
