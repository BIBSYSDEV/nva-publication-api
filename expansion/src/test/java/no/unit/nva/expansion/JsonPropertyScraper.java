package no.unit.nva.expansion;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;

public class JsonPropertyScraper {

    public static final Set<String> fields = new HashSet<>();

    private JsonPropertyScraper() {
        // NO-OP
    }

    public static Set<String> getAllProperties(JsonNode node) {
        if (node.isObject()) {
            var iterator = node.fields();
            while (iterator.hasNext()) {
                var field = iterator.next();
                fields.add(field.getKey());
                if (field.getValue().isObject() || field.getValue().isArray()) {
                    fields.addAll(getAllProperties(field.getValue()));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                if (element.isObject() || element.isArray()) {
                    fields.addAll(getAllProperties(element));
                }
            }
        }
        return fields;
    }
}