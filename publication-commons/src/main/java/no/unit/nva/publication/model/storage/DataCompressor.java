package no.unit.nva.publication.model.storage;

import static java.util.zip.Deflater.BEST_COMPRESSION;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.model.storage.DynamoEntry.CONTAINED_DATA_FIELD_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
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
import nva.commons.core.JacocoGenerated;

public class DataCompressor {

    private static final boolean NO_WRAP = true;
    private static final int COMPRESSION_LEVEL = BEST_COMPRESSION;

    @JacocoGenerated
    public DataCompressor() {
    }

    private static byte[] compress(byte[] uncompressedData) throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var deflater = new Deflater(COMPRESSION_LEVEL, NO_WRAP);
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater)) {
            deflaterOutputStream.write(uncompressedData);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] decompress(byte[] compressedData) throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var inflater = new Inflater(NO_WRAP);
        try (OutputStream inflaterOutputStream = new InflaterOutputStream(byteArrayOutputStream, inflater)) {
            inflaterOutputStream.write(compressedData);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static Map<String, AttributeValue> compressDaoData(Dao dao) {

        var attributeValue = attempt(
            () -> dynamoDbObjectMapper.convertValue(dao, JsonNode.class))
                                            .map(json -> Item.fromJSON(dynamoDbObjectMapper.writeValueAsString(json)))
                                            .map(ItemUtils::toAttributeValues)
                                            .orElseThrow();

        attributeValue.put(CONTAINED_DATA_FIELD_NAME, asBinaryAttributeValue(dao.getData()));
        return attributeValue;
    }

    public static <T> T decompressDao(Map<String, AttributeValue> valuesMap, Class<T> daoClass) {
        var dataJson = attempt(() -> valuesMap.get(CONTAINED_DATA_FIELD_NAME).getB().array())
                           .map(DataCompressor::decompress)
                           .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                           .map(dynamoDbObjectMapper::readTree)
                           .orElseThrow();

        var objectNode = attempt(() -> ItemUtils.toItem(valuesMap))
                             .map(item -> dynamoDbObjectMapper.readValue(item.toJSON(), ObjectNode.class))
                             .orElseThrow();

        objectNode.put(CONTAINED_DATA_FIELD_NAME, dataJson);

        return attempt(() -> dynamoDbObjectMapper.readValue(objectNode.toString(), daoClass)).orElseThrow();
    }

    private static AttributeValue asBinaryAttributeValue(Entity dao) {
        var compressedDataBytes = attempt(() -> dynamoDbObjectMapper.convertValue(dao, JsonNode.class))
                                      .map(JsonNode::toString)
                                      .map(jsonStr -> jsonStr.getBytes(StandardCharsets.UTF_8))
                                      .map(DataCompressor::compress)
                                      .orElseThrow();
        return new AttributeValue().withB(ByteBuffer.wrap(compressedDataBytes));
    }


}
