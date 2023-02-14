package no.unit.nva.publication.testing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public final class TypeProvider {
    
    private TypeProvider() {
    
    }
    
    public static Stream<Class<?>> listSubTypes(Class<?> type) {
        var types = fetchDirectSubtypes(type);
        var result = new HashSet<Class<?>>();
        var nestedTypes = new ArrayDeque<Type>(types);
        while (!nestedTypes.isEmpty()) {
            var currentType = nestedTypes.pop();
            if (isTypeWithSubtypes(currentType)) {
                var subTypes = fetchDirectSubtypes(currentType.value());
                nestedTypes.addAll(subTypes);
            } else {
                result.add(currentType.value());
            }
        }
        return result.stream();
    }
    
    private static boolean isTypeWithSubtypes(Type type) {
        return type.value().getAnnotationsByType(JsonSubTypes.class).length > 0;
    }
    
    private static List<Type> fetchDirectSubtypes(Class<?> type) {
        var annotations = type.getAnnotationsByType(JsonSubTypes.class);
        return Arrays.asList(annotations[0].value());
    }
}
