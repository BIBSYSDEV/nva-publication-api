package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import nva.commons.core.attempt.Try;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonWriter;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

public final class S3IonReader {

    // Looking for Strings '<end_of_previous_object>}<possible_white_space>{
    public static final String CONSECUTIVE_JSON_OBJECTS = "(})\\s*(\\{)";
    public static final String SUCCESSIVE_ELEMENTS_IN_ARRAY = "$1,$2";
    public static final String BEGIN_ARRAY_DELIMITER = "[";
    public static final String END_ARRAY_DELIMITER = "]";
    public static final boolean SEQUENTIAL = false;

    private S3IonReader() {

    }

    public static Stream<JsonNode> extractJsonNodesFromIonContent(String content) {
        return Try.of(content)
            .map(S3IonReader::toJsonObjectsString)
            .map(S3IonReader::transformMultipleJsonObjectsToJsonArrayWithObjects)
            .map(S3IonReader::toArrayNode)
            .map(S3IonReader::convertToJsonNodeStream)
            .orElseThrow();
    }

    private static String toJsonObjectsString(String ion) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (IonWriter writer = createIonToJsonTransformer(stringBuilder)) {
            rewrite(ion, writer);
        }
        return stringBuilder.toString();
    }

    private static IonWriter createIonToJsonTransformer(StringBuilder stringBuilder) {
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
        return jsonStringIsAlreadyAnArray(arrayElements)
                   ? arrayElements
                   : BEGIN_ARRAY_DELIMITER + arrayElements + END_ARRAY_DELIMITER;
    }

    private static boolean jsonStringIsAlreadyAnArray(String arrayElements) {
        return arrayElements.startsWith(BEGIN_ARRAY_DELIMITER);
    }

    private static String transformMultipleJsonObjectsToJsonArrayWithObjects(String jsonObjects) {
        String arrayElements = makeConsecutiveJsonObjectsElementsOfJsonArray(jsonObjects);
        return addArrayDelimiters(arrayElements);
    }

    private static ArrayNode toArrayNode(String jsonString) throws JsonProcessingException {
        return (ArrayNode) s3ImportsMapper.readTree(jsonString);
    }

    private static  Stream<JsonNode> convertToJsonNodeStream(ArrayNode arrayNode) {
        return StreamSupport
            .stream(Spliterators.spliteratorUnknownSize(arrayNode.iterator(), Spliterator.ORDERED), SEQUENTIAL);
    }
}
