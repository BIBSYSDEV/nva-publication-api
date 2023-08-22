package no.unit.nva.publication.utils;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class IdentifiersDataSetGenerator extends AbstractDataSetGenerator {

    private static final String PUBLICATION_URL = "publicationUrl";
    private static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    private static final String SOURCE = "source";
    private static final String SOURCE_IDENTIFIER = "identifier";
    private static final String[] COLUMNS = new String[]{
        PUBLICATION_URL,
        PUBLICATION_IDENTIFIER,
        SOURCE,
        SOURCE_IDENTIFIER
    };

    protected IdentifiersDataSetGenerator() {
        super("identifiers", COLUMNS);
    }

    @Override
    public void addEntry(JsonNode rootNode, String... references) {
        var iterator = rootNode.elements();
        while (iterator.hasNext()) {
            extractIdentifier(references[0], references[1], iterator.next());
        }
    }

    private void extractIdentifier(String publicationUrl,
                                   String publicationIdentifier,
                                   JsonNode additionalIdentifierNode) {
        var sourceName = additionalIdentifierNode.get("sourceName").asText("");
        var identifier = additionalIdentifierNode.get("value").asText("");

        writeLine(new String[]{publicationUrl, publicationIdentifier, sourceName, identifier});
    }

    @Override
    public void exportToFile() throws IOException {
        super.exportToFile();
    }
}
