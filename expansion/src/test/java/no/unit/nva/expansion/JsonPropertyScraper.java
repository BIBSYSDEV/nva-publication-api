package no.unit.nva.expansion;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;

public class JsonPropertyScraper {

    private JsonPropertyScraper() {
        // NO-OP
    }

    public static Set<String> getAllProperties(JsonNode node) {
        var fields = new HashSet<String>();
        var iterator = node.fields();
        while (iterator.hasNext()) {
            var field = iterator.next();
            fields.add(field.getKey());
            if (field.getValue().isObject()) {
                fields.addAll(getAllProperties(field.getValue()));
            }
        }
        return fields;
    }
}