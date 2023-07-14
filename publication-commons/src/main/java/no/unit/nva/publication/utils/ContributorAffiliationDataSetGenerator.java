package no.unit.nva.publication.utils;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.databind.JsonNode;

public class ContributorAffiliationDataSetGenerator extends AbstractDataSetGenerator {

    private static final String PUBLICATION_URL = "publicationUrl";
    private static final String CONTRIBUTOR_ID = "contributorId";
    private static final String CONTRIBUTOR_NAME = "contributorName";
    private static final String AFFILIATION_ID = "affiliationId";
    private static final String AFFILIATION_NAME = "affiliationName";

    private static final String[] COLUMNS = new String[]{
        PUBLICATION_URL,
        CONTRIBUTOR_ID,
        CONTRIBUTOR_NAME,
        AFFILIATION_ID,
        AFFILIATION_NAME
    };

    protected ContributorAffiliationDataSetGenerator() {
        super("affiliations", COLUMNS);
    }

    @Override
    public void addEntry(JsonNode rootNode, String... references) {
        var publicationUrl = references[0];
        var contributorId = references[1];
        var contributorName = references[2];

        for (JsonNode affiliationNode : rootNode) {
            var affiliationId = getOptionalNodeValue(affiliationNode.at("/id"));
            var nameNode = affiliationNode.at("/labels/nb");
            var affiliationName = nonNull(nameNode) ? nameNode.asText() : null;
            writeLine(new String[]{publicationUrl, contributorId, contributorName, affiliationId, affiliationName});
        }

    }
}
