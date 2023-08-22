package no.unit.nva.publication.utils;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class ContributorDataSetGenerator extends AbstractDataSetGenerator {

    private static final String PUBLICATION_URL = "publicationUrl";
    private static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String SEQUENCE_NO = "sequenceNo";
    private static final String ROLE = "role";

    private static final String[] COLUMNS = new String[]{
        PUBLICATION_URL,
        PUBLICATION_IDENTIFIER,
        ID,
        NAME,
        SEQUENCE_NO,
        ROLE
    };

    private ContributorAffiliationDataSetGenerator contributorAffiliationDataSetGenerator =
        new ContributorAffiliationDataSetGenerator();

    protected ContributorDataSetGenerator() {
        super("contributors", COLUMNS);
    }

    @Override
    public void addEntry(JsonNode rootNode, String... references) {
        var name = rootNode.at("/identity/name").asText();
        var id = getOptionalNodeValue(rootNode.at("/identity/id"));
        var sequenceNo = rootNode.at("/sequence").asText();
        var role = rootNode.at("/role/type").asText();

        writeLine(new String[]{references[0], references[1], id, name, sequenceNo, role});

        var affiliationsNode = rootNode.at("/affiliations");
        if (affiliationsNode != null) {
            contributorAffiliationDataSetGenerator.addEntry(affiliationsNode, references[0], references[1], id, name);
        }
    }

    @Override
    public void exportToFile() throws IOException {
        super.exportToFile();

        contributorAffiliationDataSetGenerator.exportToFile();
    }
}
