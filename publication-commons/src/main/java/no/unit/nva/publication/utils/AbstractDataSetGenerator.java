package no.unit.nva.publication.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVWriter;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public abstract class AbstractDataSetGenerator implements Closeable {

    private static final char UTF8_BOM = '\ufeff';
    private static final char SEPARATOR_CHAR = ';';
    private static final char QUOTE_CHAR = '"';
    private static final char ESCAPE_CHAR = '\\';
    private static final String LINE_END = "\r\n";

    private final String dataSetName;
    private final StringWriter writer;
    private final CSVWriter csvWriter;

    protected AbstractDataSetGenerator(String dataSetName, String[] columnNames) {
        this.dataSetName = dataSetName;
        this.writer = new StringWriter();
        this.csvWriter = new CSVWriter(this.writer, SEPARATOR_CHAR, QUOTE_CHAR, ESCAPE_CHAR, LINE_END);
        csvWriter.writeNext(columnNames);
    }

    public abstract void addEntry(JsonNode rootNode, String... references);

    protected void writeLine(String[] values) {
        csvWriter.writeNext(values);
    }

    protected static String getOptionalNodeValue(JsonNode node) {
        return Optional.ofNullable(node)
                   .map(nodeToMap -> nodeToMap.isMissingNode() ? null : node.asText())
                   .orElse(null);
    }

    public void exportToFile() throws IOException {
        var file = new File(dataSetName + ".csv");
        try (var fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
                                                                        StandardCharsets.UTF_8))) {
            fileWriter.write(UTF8_BOM); // UTF-8 BOM
            fileWriter.write(writer.toString());
        }
    }

    @Override
    public void close() throws IOException {
        this.csvWriter.close();
    }
}
