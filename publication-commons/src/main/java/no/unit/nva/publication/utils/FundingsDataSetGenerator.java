package no.unit.nva.publication.utils;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.databind.JsonNode;

public class FundingsDataSetGenerator extends AbstractDataSetGenerator {

    private static final String PUBLICATION_URL = "publicationUrl";
    private static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    private static final String SOURCE = "source";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String[] COLUMN_NAMES = new String[]{
        PUBLICATION_URL,
        PUBLICATION_IDENTIFIER,
        SOURCE,
        ID,
        NAME
    };

    public FundingsDataSetGenerator() {
        super("fundings", COLUMN_NAMES);
    }

    @Override
    public void addEntry(JsonNode rootNode, String... references) {
        var fundingSource = rootNode.at("/source/identifier").asText();
        var fundingIdNode = rootNode.at("/identifier");
        var fundingId = nonNull(fundingIdNode) ? fundingIdNode.asText() : null;
        var fundingLabelsNode = rootNode.at("/labels/nb");
        var fundingName = nonNull(fundingLabelsNode) ? fundingLabelsNode.asText() : "";
        writeLine(new String[]{references[0], references[1], fundingSource, fundingId, fundingName});
    }
}
