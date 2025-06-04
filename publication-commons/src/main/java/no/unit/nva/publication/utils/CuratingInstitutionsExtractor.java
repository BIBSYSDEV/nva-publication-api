package no.unit.nva.publication.utils;

import java.net.URI;
import java.util.List;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.publication.model.business.Resource;

public final class CuratingInstitutionsExtractor {
    private CuratingInstitutionsExtractor() {
        // Utility class, no instantiation
    }
    public static List<URI> getCuratingInstitutionsIdList(Resource resource) {
        return resource.getCuratingInstitutions().stream()
                   .map(CuratingInstitution::id)
                   .toList();
    }
}
