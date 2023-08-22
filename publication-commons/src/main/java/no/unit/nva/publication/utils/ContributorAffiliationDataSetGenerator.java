package no.unit.nva.publication.utils;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.databind.JsonNode;
import nva.commons.core.paths.UriWrapper;

public class ContributorAffiliationDataSetGenerator extends AbstractDataSetGenerator {

    private static final String PUBLICATION_URL = "publicationUrl";
    private static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    private static final String CONTRIBUTOR_ID = "contributorId";
    private static final String CONTRIBUTOR_NAME = "contributorName";
    private static final String AFFILIATION_ID = "affiliationId";
    private static final String AFFILIATION_NAME = "affiliationName";
    private static final String INSTITUTION_ID = "institutionId";
    private static final String DEPARTMENT_ID = "departmentId";
    private static final String SUB_DEPARTMENT_ID = "subDepartmentId";
    private static final String GROUP_ID = "groupId";

    private static final String[] COLUMNS = new String[]{
        PUBLICATION_URL,
        PUBLICATION_IDENTIFIER,
        CONTRIBUTOR_ID,
        CONTRIBUTOR_NAME,
        AFFILIATION_ID,
        AFFILIATION_NAME,
        INSTITUTION_ID,
        DEPARTMENT_ID,
        SUB_DEPARTMENT_ID,
        GROUP_ID
    };

    protected ContributorAffiliationDataSetGenerator() {
        super("affiliations", COLUMNS);
    }

    @Override
    public void addEntry(JsonNode rootNode, String... references) {
        var publicationUrl = references[0];
        var publicationIdentifier = references[1];
        var contributorId = references[2];
        var contributorName = references[3];

        for (JsonNode affiliationNode : rootNode) {
            var affiliationId = getOptionalNodeValue(affiliationNode.at("/id"));
            String institutionId = null;
            String departmentId = null;
            String subDepartmentId = null;
            String groupId = null;
            if (affiliationId != null) {
                var idPart = UriWrapper.fromUri(affiliationId).getLastPathElement();
                var ids = idPart.split("\\.");
                institutionId = ids[0];
                departmentId = ids[1];
                subDepartmentId = ids[2];
                groupId = ids[3];
            }
            var nameNode = affiliationNode.at("/labels/nb");
            var affiliationName = nonNull(nameNode) ? nameNode.asText() : null;
            writeLine(new String[]{publicationUrl, publicationIdentifier, contributorId, contributorName,
                affiliationId, affiliationName, institutionId, departmentId, subDepartmentId, groupId});
        }
    }
}
