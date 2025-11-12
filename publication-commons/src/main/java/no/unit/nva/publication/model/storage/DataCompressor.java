package no.unit.nva.publication.model.storage;

import static java.util.zip.Deflater.BEST_COMPRESSION;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.model.storage.DynamoEntry.CONTAINED_DATA_FIELD_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.storage.importcandidate.DatabaseEntryWithData;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCompressor {

    private static final Logger logger = LoggerFactory.getLogger(DataCompressor.class);
    private static final boolean NO_WRAP = true;
    private static final int COMPRESSION_LEVEL = BEST_COMPRESSION;

    @JacocoGenerated
    public DataCompressor() {
    }

    public static byte[] decompress(byte[] compressedData) throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var inflater = new Inflater(NO_WRAP);
        try (OutputStream inflaterOutputStream = new InflaterOutputStream(byteArrayOutputStream, inflater)) {
            inflaterOutputStream.write(compressedData);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static <T> Map<String, AttributeValue> compress(DatabaseEntryWithData<T> value) {
        var attributeValue = toAttributeValueMap(value);
        attributeValue.put(CONTAINED_DATA_FIELD_NAME, asBinaryAttributeValue(value.getData()));
        return attributeValue;
    }

    public static <T> T decompress(Map<String, AttributeValue> map, Class<T> clazz) {
        var objectNode = toObjectNode(map);
        objectNode.set(CONTAINED_DATA_FIELD_NAME, getData(map));
        return attempt(() -> dynamoDbObjectMapper.readValue(objectNode.toString(), clazz)).orElseThrow();
    }

    public static Map<String, AttributeValue> compressDaoData(Dao dao) {

        var attributeValue = attempt(() -> dynamoDbObjectMapper.convertValue(dao, JsonNode.class)).map(
            DataCompressor::toItem).map(ItemUtils::toAttributeValues).orElseThrow(failure -> logFailure(failure, dao));

        attributeValue.put(CONTAINED_DATA_FIELD_NAME, asBinaryAttributeValue(dao.getData()));
        return attributeValue;
    }

    public static <T> T decompressDao(Map<String, AttributeValue> valuesMap, Class<T> daoClass) {
        var dataJson = getData(valuesMap);

        var objectNode = toObjectNode(valuesMap);

        objectNode.put(CONTAINED_DATA_FIELD_NAME, dataJson);

        return attempt(() -> dynamoDbObjectMapper.readValue(objectNode.toString(), daoClass)).orElseThrow();
    }

    private static <T> Map<String, AttributeValue> toAttributeValueMap(DatabaseEntryWithData<T> value) {
        return attempt(() -> dynamoDbObjectMapper.convertValue(value, JsonNode.class)).map(DataCompressor::toItem)
                   .map(ItemUtils::toAttributeValues)
                   .orElseThrow();
    }

    private static Item toItem(JsonNode json) throws JsonProcessingException {
        return Item.fromJSON(dynamoDbObjectMapper.writeValueAsString(json));
    }

    private static ObjectNode toObjectNode(Map<String, AttributeValue> map) {
        return attempt(() -> ItemUtils.toItem(map)).map(
            item -> dynamoDbObjectMapper.readValue(item.toJSON(), ObjectNode.class)).orElseThrow();
    }

    private static JsonNode getData(Map<String, AttributeValue> map) {
        return attempt(() -> map.get(CONTAINED_DATA_FIELD_NAME).getB().array()).map(DataCompressor::decompress)
                   .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                   .map(dynamoDbObjectMapper::readTree)
                   .orElseThrow();
    }

    private static byte[] compress(byte[] uncompressedData) throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var deflater = new Deflater(COMPRESSION_LEVEL, NO_WRAP);
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater)) {
            deflaterOutputStream.write(uncompressedData);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static RuntimeException logFailure(Failure<Map<String, AttributeValue>> failure, Dao dao) {
        logger.error("Failure while converting dao to jsonNode. Dao identifier: {}", dao.getIdentifier());
        var exception = failure.getException();
        return exception instanceof IllegalArgumentException ? new IllegalArgumentException(exception)
                   : new RuntimeException(exception);
    }

    private static AttributeValue asBinaryAttributeValue(Entity dao) {
        var compressedDataBytes = attempt(() -> dynamoDbObjectMapper.convertValue(dao, JsonNode.class)).map(
                JsonNode::toString)
                                      .map(jsonStr -> jsonStr.getBytes(StandardCharsets.UTF_8))
                                      .map(DataCompressor::compress)
                                      .orElseThrow();
        return new AttributeValue().withB(ByteBuffer.wrap(compressedDataBytes));
    }

    private static <T> AttributeValue asBinaryAttributeValue(T value) {
        var compressedDataBytes = attempt(() -> dynamoDbObjectMapper.convertValue(value, JsonNode.class)).map(
                JsonNode::toString)
                                      .map(jsonStr -> jsonStr.getBytes(StandardCharsets.UTF_8))
                                      .map(DataCompressor::compress)
                                      .orElseThrow();
        return new AttributeValue().withB(ByteBuffer.wrap(compressedDataBytes));
    }
}
