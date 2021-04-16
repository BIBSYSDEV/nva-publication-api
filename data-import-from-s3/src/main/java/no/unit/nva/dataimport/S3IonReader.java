package no.unit.nva.dataimport;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Try;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonWriter;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

public class S3IonReader {

    // Looking for Strings '<end_of_previous_object>}<possible_white_space>{"Item":{"PK0"'
    public static final String CONSECUTIVE_JSON_OBJECTS = "(})\\s*(\\{\"Item\":\\{\".+?\")";
    public static final String SUCCESSIVE_ELEMENTS_IN_ARRAY = "$1,$2";
    public static final String BEGIN_ARRAY_DELIMITER = "[";
    public static final String END_ARRAY_DELIMITER = "]";
    public static final boolean SEQUENTIAL = false;
    public static final String ION_ITEM = "Item";
    public static final String FILE_NOT_FOUND_ERROR_MESSAGE = "File not found: ";
    private final S3Driver s3Driver;

    public S3IonReader(S3Driver s3Driver) {
        this.s3Driver = s3Driver;
    }

    public List<JsonNode> extractJsonNodesFromS3File(String filename) throws IOException {
        return extractJsonNodeStreamFromS3File(filename).collect(Collectors.toList());
    }

    public Stream<JsonNode> extractJsonNodeStreamFromS3File(String filename) throws IOException {
        String content = fetchFile(filename);
        String jsonString = toJsonObjectsString(content);
        String jsonArrayString = transformMultipleJsonObjectsToJsonArrayWithObjects(jsonString);
        ArrayNode arrayNode = toArrayNode(jsonArrayString);
        return convertToJsonNodeStream(arrayNode);
    }

    public List<Item> extractItemsFromS3File(String filename) throws IOException {
        String content = fetchFile(filename);
        String jsonString = toJsonObjectsString(content);
        String jsonArrayString = transformMultipleJsonObjectsToJsonArrayWithObjects(jsonString);
        ArrayNode arrayNode = toArrayNode(jsonArrayString);
        return convertToItems(convertToJsonNodeStream(arrayNode));
    }

    private static String toJsonObjectsString(String ion) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (IonWriter writer = createIonWriter(stringBuilder)) {
            rewrite(ion, writer);
        }
        return stringBuilder.toString();
    }

    private static IonWriter createIonWriter(StringBuilder stringBuilder) {
        return IonTextWriterBuilder.json().withCharset(StandardCharsets.UTF_8).build(stringBuilder);
    }

    private static void rewrite(String textIon, IonWriter writer) throws IOException {
        try (IonReader reader = IonReaderBuilder.standard().build(textIon)) {
            writer.writeValues(reader);
        }
    }

    private static String makeConsecutiveJsonObjectsElementsOfJsonArray(String jsonObjects) {
        return jsonObjects.replaceAll(CONSECUTIVE_JSON_OBJECTS, SUCCESSIVE_ELEMENTS_IN_ARRAY);
    }

    private static String addArrayDelimiters(String arrayElements) {
        return BEGIN_ARRAY_DELIMITER + arrayElements + END_ARRAY_DELIMITER;
    }

    private static String transformMultipleJsonObjectsToJsonArrayWithObjects(String jsonObjects) {
        String arrayElements = makeConsecutiveJsonObjectsElementsOfJsonArray(jsonObjects);
        return addArrayDelimiters(arrayElements);
    }

    private static List<Item> convertToItems(Stream<JsonNode> content) {
        return content.map(attempt(S3IonReader::toItem))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    private static ArrayNode toArrayNode(String jsonString) throws JsonProcessingException {
        return (ArrayNode) JsonUtils.objectMapperNoEmpty.readTree(jsonString);
    }

    private static Item toItem(JsonNode json) throws JsonProcessingException {
        return Item.fromJSON(JsonUtils.objectMapperNoEmpty.writeValueAsString(json));
    }

    private String fetchFile(String filename) {
        try {
            return s3Driver.getFile(filename);
        } catch (NoSuchKeyException e) {
            throw new IllegalArgumentException(FILE_NOT_FOUND_ERROR_MESSAGE + filename);
        }
    }

    private Stream<JsonNode> convertToJsonNodeStream(ArrayNode arrayNode) {
        return StreamSupport
                   .stream(Spliterators.spliteratorUnknownSize(arrayNode.iterator(), Spliterator.ORDERED), SEQUENTIAL)
                   .map(node -> node.get(ION_ITEM));
    }
}
