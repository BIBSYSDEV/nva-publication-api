package no.unit.nva.publication.utils;

public final class CsvExportGenerator {
//
//    /* package */ static final String CRLF = "\r\n";
//    /* package */ static final char SEPARATOR = ';';
//    /* package */ static final char QUOTE_CHAR = '"';
//    /* package */ static final char ESCAPE_CHAR = '\\';
//
//    /* package */ static final String COLUMN_NAME_URL = "url";
//    /* package */ static final String COLUMN_NAME_TITLE = "title";
//    /* package */ static final String COLUMN_NAME_CATEGORY = "category";
//    /* package */ static final String COLUMN_NAME_PUBLICATION_DATE = "publicationDate";
//    /* package */ static final String COLUMN_NAME_CONTRIBUTORS = "contributors";
//    /* package */ static final String COLUMN_NAME_CHANNEL_TYPE = "channelType";
//    /* package */ static final String COLUMN_NAME_CHANNEL_IDENTIFIER = "channelIdentifier";
//    /* package */ static final String COLUMN_NAME_CHANNEL_NAME = "channelName";
//    /* package */ static final String COLUMN_NAME_CHANNEL_ONLINE_ISSN = "channelOnlineIssn";
//    /* package */ static final String COLUMN_NAME_CHANNEL_PRINT_ISSN = "channelPrintIssn";
//    /* package */ static final String COLUMN_NAME_CHANNEL_LEVEL = "channelLevel";
//    /* package */ static final String COLUMN_NAME_FUNDING_SOURCES = "fundingSources";
//    /* package */ static final String COLUMN_NAME_CONTRIBUTOR_PUBLICATION_URL = "publicationUrl";
//    /* package */ static final String COLUMN_NAME_CONTRIBUTOR_ID = "contributorId";
//    /* package */ static final String COLUMN_NAME_CONTRIBUTOR_NAME = "contributorName";
//    /* package */ static final String COLUMN_NAME_CONTRIBUTOR_SEQUENCE = "sequenceNo";
//    /* package */ static final String COLUMN_NAME_CONTRIBUTOR_ROLE = "role";
//    /* package */ static final String COLUMN_NAME_FUNDINGS_PUBLICATION_URL = "publicationUrl";
//    /* package */ static final String COLUMN_NAME_FUNDINGS_ID = "id";
//    /* package */ static final String COLUMN_NAME_FUNDINGS_SOURCE = "source";
//    /* package */ static final String COLUMN_NAME_FUNDINGS_NAME = "name";
//    private static final int ONE = 1;
//
//    private CsvExportGenerator() {
//    }
//
//    public static ExportDataSet generate(JsonNode... documents) {
//        var writersPerDataSet = Map.of(
//            DATA_SET_PUBLICATIONS, new StringWriter(),
//            DATA_SET_CONTRIBUTORS, new StringWriter(),
//            DATA_SET_FUNDINGS, new StringWriter());
//
//        var publicationsCsvWriter = new CSVWriter(writersPerDataSet.get(DATA_SET_PUBLICATIONS),
//                                                  SEPARATOR,
//                                                  QUOTE_CHAR,
//                                                  ESCAPE_CHAR,
//                                                  CRLF);
//        var contributorsCsvWriter = new CSVWriter(writersPerDataSet.get(DATA_SET_CONTRIBUTORS),
//                                                  SEPARATOR,
//                                                  QUOTE_CHAR,
//                                                  ESCAPE_CHAR,
//                                                  CRLF);
//        var fundingsCsvWriter = new CSVWriter(writersPerDataSet.get(DATA_SET_FUNDINGS),
//                                              SEPARATOR,
//                                              QUOTE_CHAR,
//                                              ESCAPE_CHAR,
//                                              CRLF);
//        var csvWritersPerDataSet = Map.of(
//            DATA_SET_PUBLICATIONS,
//            publicationsCsvWriter,
//            DATA_SET_CONTRIBUTORS,
//            contributorsCsvWriter,
//            DATA_SET_FUNDINGS,
//            fundingsCsvWriter);
//
//        publicationsCsvWriter.writeNext(new String[]{
//            COLUMN_NAME_URL,
//            COLUMN_NAME_TITLE,
//            COLUMN_NAME_CATEGORY,
//            COLUMN_NAME_PUBLICATION_DATE,
//            COLUMN_NAME_CHANNEL_TYPE,
//            COLUMN_NAME_CHANNEL_IDENTIFIER,
//            COLUMN_NAME_CHANNEL_NAME,
//            COLUMN_NAME_CHANNEL_ONLINE_ISSN,
//            COLUMN_NAME_CHANNEL_PRINT_ISSN,
//            COLUMN_NAME_CHANNEL_LEVEL
//        });
//
//        contributorsCsvWriter.writeNext(new String[]{
//            COLUMN_NAME_CONTRIBUTOR_PUBLICATION_URL,
//            COLUMN_NAME_CONTRIBUTOR_ID,
//            COLUMN_NAME_CONTRIBUTOR_NAME,
//            COLUMN_NAME_CONTRIBUTOR_SEQUENCE,
//            COLUMN_NAME_CONTRIBUTOR_ROLE
//        });
//        fundingsCsvWriter.writeNext(new String[]{
//            COLUMN_NAME_FUNDINGS_PUBLICATION_URL,
//            COLUMN_NAME_FUNDINGS_SOURCE,
//            COLUMN_NAME_FUNDINGS_ID,
//            COLUMN_NAME_FUNDINGS_NAME
//        });
//        Arrays.stream(documents)
//            .map(CsvExportGenerator::generateLine)
//            .forEach(linesPerDataSetMap -> writeLines(csvWritersPerDataSet, linesPerDataSetMap));
//
//        csvWritersPerDataSet.values().forEach(csvWriter -> attempt(() -> {
//            csvWriter.close();
//            return null;
//        }).orElseThrow());
//        return new ExportDataSet(writersPerDataSet.get(DATA_SET_PUBLICATIONS).toString(),
//                                 writersPerDataSet.get(DATA_SET_CONTRIBUTORS).toString(),
//                                 writersPerDataSet.get(DATA_SET_FUNDINGS).toString());
//    }
//
//    private static void writeLines(Map<String, CSVWriter> csvWritersPerDataSet,
//                                   Map<String, List<String[]>> linesPerDataSet) {
//        linesPerDataSet.forEach((dataSet, lines) -> {
//            var csvWriter = csvWritersPerDataSet.get(dataSet);
//            csvWriter.writeAll(lines);
//        });
//    }
//
//    private static Map<String, List<String[]>> generateLine(JsonNode document) {
//        var url = document.get("id").asText();
//        var title = document.at("/entityDescription/mainTitle").asText();
//        var category = document.at("/entityDescription/reference/publicationInstance/type").asText();
//        String publicationDate = extractPublicationDate(document);
//        var contributors = extractContributors(url, document);
//        var publicationContextNode = document.at("/entityDescription/reference/publicationContext");
//        String channelType = null;
//        String channelIdentifier = null;
//        String channelName = null;
//        String channelOnlineIssn = null;
//        String channelPrintIssn = null;
//        String channelLevel = null;
//        if (publicationContextNode.get("id") != null) {
//            channelType = extractChannelType(publicationContextNode);
//            channelIdentifier = extractChannelIdentifier(publicationContextNode);
//            channelName = extractChannelName(publicationContextNode);
//            channelOnlineIssn = extractChannelOnlineIssn(publicationContextNode);
//            channelPrintIssn = extractChannelPrintIssn(publicationContextNode);
//            channelLevel = extractChannelLevel(publicationContextNode);
//        }
//        var fundingSources = extractFundingSources(url, document);
//
//        return Map.of(
//            DATA_SET_PUBLICATIONS, List.<String[]>of(new String[]{
//                url,
//                title,
//                category,
//                publicationDate,
//                channelType,
//                channelIdentifier,
//                channelName,
//                channelOnlineIssn,
//                channelPrintIssn,
//                channelLevel}),
//            DATA_SET_CONTRIBUTORS, contributors,
//            DATA_SET_FUNDINGS, fundingSources);
//    }
//
//    private static List<String[]> extractFundingSources(String url, JsonNode document) {
//        var fundingsNode = document.at("/fundings");
//        if (fundingsNode != null) {
//            var iterator = fundingsNode.elements();
//            var fundingSources = new ArrayList<String[]>();
//            while (iterator.hasNext()) {
//                var fundingNode = iterator.next();
//                var fundingSource = fundingNode.at("/source").asText();
//                var fundingIdNode = fundingNode.at("/id");
//                var fundingId = nonNull(fundingIdNode) ? fundingIdNode.asText() : null;
//                var fundingLabelsNode = fundingNode.at("/labels/nb");
//                var fundingName = nonNull(fundingLabelsNode) ? fundingLabelsNode.asText() : "";
//                fundingSources.add(new String[]{url, fundingSource, fundingId, fundingName});
//            }
//            return fundingSources;
//        } else {
//            return Collections.emptyList();
//        }
//    }
//
//    private static String extractChannelType(JsonNode publicationContextNode) {
//        return publicationContextNode.get("type").asText();
//    }
//
//    private static String extractChannelIdentifier(JsonNode publicationContextNode) {
//        return Optional.ofNullable(publicationContextNode.get("identifier")).map(JsonNode::asText).orElse(null);
//    }
//
//    private static String extractChannelName(JsonNode publicationContextNode) {
//        return Optional.ofNullable(publicationContextNode.get("name")).
//                   map(JsonNode::asText)
//                   .orElse(null);
//    }
//
//    private static String extractChannelOnlineIssn(JsonNode publicationContextNode) {
//        return Optional.ofNullable(publicationContextNode.get("onlineIssn")).
//                   map(JsonNode::asText)
//                   .orElse(null);
//    }
//
//    private static String extractChannelPrintIssn(JsonNode publicationContextNode) {
//        return Optional.ofNullable(publicationContextNode.get("printIssn")).
//                   map(JsonNode::asText)
//                   .orElse(null);
//    }
//
//    private static String extractChannelLevel(JsonNode publicationContextNode) {
//        return Optional.ofNullable(publicationContextNode.get("level")).
//                   map(JsonNode::asText)
//                   .orElse("");
//    }
//
//    private static List<String[]> extractContributors(String publication_url, JsonNode document) {
//        var contributorsNode = document.at("/entityDescription/contributors");
//        var contributorLines = new ArrayList<String[]>();
//        var iterator = contributorsNode.elements();
//        while (iterator.hasNext()) {
//            var contributorNode = iterator.next();
//
//            var name = contributorNode.at("/identity/name").asText();
//            var idNode = contributorNode.at("/identity/id");
//            var id = nonNull(idNode) ? idNode.asText() : null;
//            var sequenceNo = contributorNode.at("/sequence").asText();
//            var role = contributorNode.at("/role/type").asText();
//            contributorLines.add(new String[]{publication_url, id, name, sequenceNo, role});
//        }
//        return contributorLines;
//    }
//
//    private static String extractPublicationDate(JsonNode document) {
//        var publicationDateNode = document.at("/entityDescription/publicationDate");
//        var publicationYear = Optional.ofNullable(publicationDateNode.get("year"))
//                                  .map(JsonNode::asText)
//                                  .orElse(null);
//        var publicationMonth = Optional.ofNullable(publicationDateNode.get("month"))
//                                   .map(JsonNode::asText)
//                                   .orElse(null);
//        var publicationDay = Optional.ofNullable(publicationDateNode.get("day"))
//                                 .map(JsonNode::asText)
//                                 .orElse(null);
//        return publicationYear
//               + (publicationMonth != null ? "-" + zeroPrefixedToTwoCharacters(publicationMonth) : "")
//               + (
//                   publicationDay != null ?
//                       "-" + zeroPrefixedToTwoCharacters(publicationDay) :
//                                                                             "");
//    }
//
//    private static String zeroPrefixedToTwoCharacters(String input) {
//        if (input.length() == ONE) {
//            return "0" + input;
//        } else {
//            return input;
//        }
//    }
}
