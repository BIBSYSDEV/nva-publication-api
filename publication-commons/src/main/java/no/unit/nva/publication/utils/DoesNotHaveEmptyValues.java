package no.unit.nva.publication.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DoesNotHaveEmptyValues {

    public static final String DELIMITER = ", ";
    public static final String JAVA_NAMESPACE = "java.";
    public static final String JAVAX_NAMESPACE = "javax.";
    public static final String PATH_DELIMITER = ".";
    public static final String EMPTY_PATH = "";

    private DoesNotHaveEmptyValues() {
    }

    public static void checkForEmptyFields(Object object,
                                           Set<String> excludedFields) throws MissingFieldException {
        checkForEmptyFields(object, trimExcludedFields(excludedFields), EMPTY_PATH, new HashSet<>());
    }

    private static void checkForEmptyFields(Object object,
                                            Set<String> excludedFields,
                                            String prefix,
                                            Set<Object> visited) throws MissingFieldException {
        checkForNullObject(object);

        if (!visited.add(object)) {
            return;
        }

        final var emptyFields = new ArrayList<String>();
        var fields = object.getClass().getDeclaredFields();

        for (var field : fields) {
            field.trySetAccessible();

            try {
                var value = field.get(object);
                var fieldName = createFieldName(prefix, field);

                if (isMissingField(excludedFields, value, fieldName)) {
                    emptyFields.add(fieldName);
                }

                if (value instanceof Iterable<?> iterable) {
                    iterateObject(excludedFields, visited, iterable, fieldName, emptyFields);
                } else if (isNestedObject(value)) {
                    checkForEmptyFields(value, excludedFields, fieldName, visited);
                }

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field " + field.getName(), e);
            }
        }
        if (!emptyFields.isEmpty()) {
            throw new MissingFieldException("Empty fields found: " + String.join(DELIMITER, emptyFields));
        }
    }

    private static Set<String> trimExcludedFields(Set<String> excludedFields) {
        return excludedFields.stream()
                   .filter(not(String::isBlank))
                   .map(DoesNotHaveEmptyValues::removeLeadingDots)
                   .collect(Collectors.toSet());
    }

    private static String removeLeadingDots(String string) {
        return !string.isBlank() && '.' == string.charAt(0) ? string.substring(1) : string;
    }

    private static void iterateObject(Set<String> excludedFields,
                                      Set<Object> visited,
                                      Iterable<?> iterable,
                                      String fieldName,
                                      ArrayList<String> emptyFields) throws MissingFieldException {
        var iterator = iterable.iterator();
        if (!iterator.hasNext() && isNotExcludedField(excludedFields, fieldName)) {
            emptyFields.add(fieldName);
        }
        while (iterator.hasNext()) {
            var item = iterator.next();
            if (isNestedObject(item)) {
                checkForEmptyFields(item, excludedFields, fieldName, visited);
            }
        }
    }

    private static void checkForNullObject(Object object) throws MissingFieldException {
        if (isNull(object)) {
            throw new MissingFieldException("Object is null");
        }
    }

    private static boolean isMissingField(Set<String> excludedFields, Object value, String fieldName) {
        return (isNull(value) && isNotExcludedField(excludedFields, fieldName))
               || (value instanceof String string && string.isEmpty() && isNotExcludedField(excludedFields, fieldName));
    }

    private static boolean isNotExcludedField(Set<String> excludedFields, String fieldName) {
        return !excludedFields.contains(fieldName) && excludedFields.stream().noneMatch(fieldName::startsWith);
    }

    private static String createFieldName(String prefix, Field field) {
        var name = extractFieldName(field);
        return prefix.isEmpty() ? name : prefix + PATH_DELIMITER + name;
    }

    private static String extractFieldName(Field field) {
        return field.isAnnotationPresent(JsonSetter.class)
                   ? field.getAnnotation(JsonSetter.class).value()
                   : field.getName();
    }

    private static boolean isNestedObject(Object item) {
        return nonNull(item)
               && !item.getClass().isEnum()
               && !item.getClass().isPrimitive()
               && !(item instanceof String)
               && !(item instanceof Number)
               && !item.getClass().getName().startsWith(JAVA_NAMESPACE)
               && !item.getClass().getName().startsWith(JAVAX_NAMESPACE);
    }

    public static class MissingFieldException extends Exception {
        public MissingFieldException(String message) {
            super(message);
        }
    }
}