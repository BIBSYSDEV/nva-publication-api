package no.unit.nva.expansion;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

public class JsonPropertyScraper {

    private JsonPropertyScraper() {
        // NO-OP
    }

    public static Set<String> getAllProperties(JsonNode node) {
        Set<String> fields = new HashSet<>();
        Iterator<Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Entry<String, JsonNode> field = iterator.next();
            fields.add(field.getKey());
            if (field.getValue().isObject()) {
                fields.addAll(getAllProperties(field.getValue()));
            }
        }
        return fields;
    }
}