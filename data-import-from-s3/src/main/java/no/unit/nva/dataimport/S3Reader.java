package no.unit.nva.dataimport;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JsonUtils;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonWriter;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

public class S3Reader {

    public static final String PK0 = DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
    // Looking for Strings '<end_of_previous_object>}<possible_white_space>{"Item":{"PK0"'
    public static final String CONSECUTIVE_JSON_OBJECTS = "(})\\s*(\\{\"Item\":\\{\"" + PK0 + "\")";
    public static final String SUCCESSIVE_ELEMENTS_IN_ARRAY = "$1,$2";
    public static final String BEGIN_ARRAY_DELIMITER = "[";
    public static final String END_ARRAY_DELIMITER = "]";
    public static final String ION_ITEM = "Item";
    private final S3Driver s3Driver;

    public S3Reader(S3Driver s3Driver) {
        this.s3Driver = s3Driver;
    }

    protected List<Item> extractItemsFromS3Bucket(String filename) throws IOException {
        String content = s3Driver.getFile(filename);
        String jsonString = toJsonObjectsString(content);
        String jsonArrayString = transformMultipleJsonObjectsToJsonArrayWithObjects(jsonString);
        ArrayNode arrayNode = toArrayNode(jsonArrayString);
        return convertToItems(arrayNode);
    }

    private static String toJsonObjectsString(String ion) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (IonWriter writer = IonTextWriterBuilder.json().build(stringBuilder)) {
            rewrite(ion, writer);
        }
        return stringBuilder.toString();
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

    private static List<Item> convertToItems(ArrayNode content) throws JsonProcessingException {
        List<Item> items = new ArrayList<>();
        for (JsonNode node : content) {
            JsonNode dynamoEntry = node.get(ION_ITEM);
            Item item = toItem(dynamoEntry);
            items.add(item);
        }
        return items;
    }

    private static ArrayNode toArrayNode(String jsonString) throws JsonProcessingException {
        return (ArrayNode) JsonUtils.objectMapperNoEmpty.readTree(jsonString);
    }

    private static Item toItem(JsonNode json) throws JsonProcessingException {
        return Item.fromJSON(JsonUtils.objectMapperNoEmpty.writeValueAsString(json));
    }
}
