package no.unit.nva.publication.utils;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Optional;

public class PublicationDataSetGenerator extends AbstractDataSetGenerator {

    private static final String URL = "url";
    private static final String TITLE = "title";
    private static final String CATEGORY = "category";
    private static final String PUBLICATION_DATE = "publicationDate";
    private static final String CHANNEL_TYPE = "channelType";
    private static final String CHANNEL_IDENTIFIER = "channelIdentifier";
    private static final String CHANNEL_NAME = "channelName";
    private static final String CHANNEL_ONLINE_ISSN = "channelOnlineIssn";
    private static final String CHANNEL_PRINT_ISSN = "channelPrintIssn";
    private static final String CHANNEL_LEVEL = "channelLevel";

    private static final String[] COLUMNS_NAMES = new String[]{
        URL,
        TITLE,
        CATEGORY,
        PUBLICATION_DATE,
        CHANNEL_TYPE,
        CHANNEL_IDENTIFIER,
        CHANNEL_NAME,
        CHANNEL_ONLINE_ISSN,
        CHANNEL_PRINT_ISSN,
        CHANNEL_LEVEL
    };
    private static final int ONE = 1;
    private final ContributorDataSetGenerator contributorDataSetGenerator = new ContributorDataSetGenerator();
    private final FundingsDataSetGenerator fundingsDataSetGenerator = new FundingsDataSetGenerator();

    public PublicationDataSetGenerator() {
        super("publications", COLUMNS_NAMES);
    }

    @Override
    public void addEntry(JsonNode rootNode, String... references) {
        var url = rootNode.get("id").asText();
        var title = rootNode.at("/entityDescription/mainTitle").asText();
        var category = rootNode.at("/entityDescription/reference/publicationInstance/type").asText();
        String publicationDate = extractPublicationDate(rootNode);
        extractContributors(url, rootNode);
        var publicationContextNode = rootNode.at("/entityDescription/reference/publicationContext");
        String channelType = null;
        String channelIdentifier = null;
        String channelName = null;
        String channelOnlineIssn = null;
        String channelPrintIssn = null;
        String channelLevel = null;
        if (publicationContextNode.get("id") != null) {
            channelType = extractChannelType(publicationContextNode);
            channelIdentifier = extractChannelIdentifier(publicationContextNode);
            channelName = extractChannelName(publicationContextNode);
            channelOnlineIssn = extractChannelOnlineIssn(publicationContextNode);
            channelPrintIssn = extractChannelPrintIssn(publicationContextNode);
            channelLevel = extractChannelLevel(publicationContextNode);
        }
        extractFundingSources(url, rootNode);
        writeLine(new String[]{
            url,
            title,
            category,
            publicationDate,
            channelType,
            channelIdentifier,
            channelName,
            channelOnlineIssn,
            channelPrintIssn,
            channelLevel});
    }

    @Override
    public void exportToFile() throws IOException {
        super.exportToFile();
        this.contributorDataSetGenerator.exportToFile();
        this.fundingsDataSetGenerator.exportToFile();
    }

    private void extractFundingSources(String url, JsonNode document) {
        var fundingsNode = document.at("/fundings");
        if (fundingsNode != null) {
            var iterator = fundingsNode.elements();
            while (iterator.hasNext()) {
                fundingsDataSetGenerator.addEntry(iterator.next(), url);
            }
        }
    }

    private static String extractChannelType(JsonNode publicationContextNode) {
        return publicationContextNode.get("type").asText();
    }

    private static String extractChannelIdentifier(JsonNode publicationContextNode) {
        return Optional.ofNullable(publicationContextNode.get("identifier")).map(JsonNode::asText).orElse(null);
    }

    private static String extractChannelName(JsonNode publicationContextNode) {
        return Optional.ofNullable(publicationContextNode.get("name")).
                   map(JsonNode::asText)
                   .orElse(null);
    }

    private static String extractChannelOnlineIssn(JsonNode publicationContextNode) {
        return Optional.ofNullable(publicationContextNode.get("onlineIssn")).
                   map(JsonNode::asText)
                   .orElse(null);
    }

    private static String extractChannelPrintIssn(JsonNode publicationContextNode) {
        return Optional.ofNullable(publicationContextNode.get("printIssn")).
                   map(JsonNode::asText)
                   .orElse(null);
    }

    private static String extractChannelLevel(JsonNode publicationContextNode) {
        return Optional.ofNullable(publicationContextNode.get("level")).
                   map(JsonNode::asText)
                   .orElse("");
    }

    private void extractContributors(String url, JsonNode document) {
        var contributorsNode = document.at("/entityDescription/contributors");
        var iterator = contributorsNode.elements();
        while (iterator.hasNext()) {
            contributorDataSetGenerator.addEntry(iterator.next(), url);
        }
    }

    private static String extractPublicationDate(JsonNode document) {
        var publicationDateNode = document.at("/entityDescription/publicationDate");
        var publicationYear = Optional.ofNullable(publicationDateNode.get("year"))
                                  .map(JsonNode::asText)
                                  .orElse(null);
        var publicationMonth = Optional.ofNullable(publicationDateNode.get("month"))
                                   .map(JsonNode::asText)
                                   .orElse(null);
        var publicationDay = Optional.ofNullable(publicationDateNode.get("day"))
                                 .map(JsonNode::asText)
                                 .orElse(null);
        return publicationYear
               + (publicationMonth != null ? "-" + zeroPrefixedToTwoCharacters(publicationMonth) : "")
               + (
                   publicationDay != null ?
                       "-" + zeroPrefixedToTwoCharacters(publicationDay) :
                                                                             "");
    }

    private static String zeroPrefixedToTwoCharacters(String input) {
        if (input.length() == ONE) {
            return "0" + input;
        } else {
            return input;
        }
    }

    @Override
    public void close() throws IOException {
        super.close();

        contributorDataSetGenerator.close();
        fundingsDataSetGenerator.close();
    }
}
