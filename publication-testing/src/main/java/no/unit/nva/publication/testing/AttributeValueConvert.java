package no.unit.nva.publication.testing;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Utility class for converting between SDK v2 AttributeValue and Lambda events AttributeValue.
 *
 * <p>The Lambda events library (aws-lambda-java-events) uses its own AttributeValue class
 * for DynamoDB Stream events. This is separate from both SDK v1 and SDK v2 AttributeValue types.
 * When testing Lambda handlers that process DynamoDB Stream events, we need to convert
 * our SDK v2 AttributeValue objects to Lambda events AttributeValue format.</p>
 */
public final class AttributeValueConvert {

    private AttributeValueConvert() {
    }

    /**
     * Converts a map of SDK v2 AttributeValues to Lambda events AttributeValues.
     */
    public static Map<String, AttributeValue> toLambdaAttributeValueMap(
            Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> sdkMap) {
        return sdkMap.entrySet().stream()
                   .collect(Collectors.toMap(Entry::getKey, e -> toLambdaAttributeValue(e.getValue())));
    }

    /**
     * Converts an SDK v2 AttributeValue to Lambda events AttributeValue.
     */
    public static AttributeValue toLambdaAttributeValue(
            software.amazon.awssdk.services.dynamodb.model.AttributeValue value) {
        return switch (value.type()) {
            case S -> new AttributeValue().withS(value.s());
            case SS -> new AttributeValue().withSS(value.ss());
            case N -> new AttributeValue().withN(value.n());
            case NS -> new AttributeValue().withNS(value.ns());
            case B -> new AttributeValue().withB(value.b().asByteBuffer());
            case BS -> new AttributeValue().withBS(value.bs().stream()
                           .map(software.amazon.awssdk.core.SdkBytes::asByteBuffer).collect(Collectors.toList()));
            case M -> new AttributeValue().withM(toLambdaAttributeValueMap(value.m()));
            case L -> new AttributeValue().withL(value.l().stream()
                          .map(AttributeValueConvert::toLambdaAttributeValue)
                          .collect(Collectors.toList()));
            case NUL -> new AttributeValue().withNULL(value.nul());
            case BOOL -> new AttributeValue().withBOOL(value.bool());
            default -> throw new IllegalArgumentException("Unknown AttributeValue type: " + value.type());
        };
    }
}
