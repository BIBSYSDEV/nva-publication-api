package no.unit.nva.publication.utils;

import static java.util.Objects.isNull;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class DoesNotHaveEmptyValues {

    private static final String GET_PREFIX = "get";
    private static final ArrayList<String> emptyFields = new ArrayList<>();
    private static final String DELIMITER = ", ";
    private static final String JAVA_PACKAGE = "java.";
    private static final String JAVAX_PACKAGE = "javax.";
    private static final int GET_PREFIX_ENDS = 3;
    private static final String EMPTY_PATH = "";
    private static final String PATH_DELIMITER = ".";

    private DoesNotHaveEmptyValues() {
        // NO-OP
    }

    public static void checkForEmptyFields(Object obj, Set<String> excludeFields) throws Exception {
        checkForEmptyFields(obj, excludeFields, EMPTY_PATH, new IdentityHashMap<>());
    }

    private static void checkForEmptyFields(Object object,
                                            Set<String> excludeFields,
                                            String currentPath,
                                            Map<Object, Boolean> visited) throws Exception {
        if (isNull(object) || visited.containsKey(object)) {
            return;
        }
        visited.put(object, Boolean.TRUE);

        var objectClass = object.getClass();
        var isRecord = objectClass.isRecord();

        for (var method : objectClass.getDeclaredMethods()) {
            if (isGetter(method, isRecord)) {
                var fullPath = constructFullPath(currentPath, extractFieldName(method, isRecord));
                if (excludeFields.contains(fullPath)) {
                    continue;
                }
                var value = method.invoke(object);
                if (isEmpty(value)) {
                    emptyFields.add(fullPath);
                } else if (isNestedObject(value)) {
                    checkForEmptyFields(value, excludeFields, fullPath, visited);
                }
            }
        }

        if (!emptyFields.isEmpty()) {
            throw new MissingFieldException("Empty fields found: " + String.join(DELIMITER, emptyFields));
        }
    }

    private static String constructFullPath(String currentPath, String fieldName) {
        return currentPath.isEmpty() ? fieldName : currentPath + PATH_DELIMITER + fieldName;
    }

    private static boolean isGetter(Method method, boolean isRecord) {
        if (isRecord) {
            return method.getParameterCount() == 0;
        } else {
            return method.getName().startsWith(GET_PREFIX) && method.getParameterCount() == 0;
        }
    }

    private static boolean isEmpty(Object value) {
        return switch (value) {
            case null -> true;
            case String string when string.isEmpty() -> true;
            case Collection<?> collection when collection.isEmpty() -> true;
            default -> value.getClass().isArray() && value instanceof Object[] objects && objects.length == 0;
        };
    }

    /**
     * This will fail if the getter does not have the exact name 'getFieldName'.
     *
     * @param method The method to check.
     * @param isRecord Whether the origin class is a record.
     * @return A string of the field name.
     */
    private static String extractFieldName(Method method, boolean isRecord) {
        if (isRecord) {
            return method.getName();
        } else {
            var fieldName = method.getName().substring(GET_PREFIX_ENDS);
            fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
            return fieldName;
        }
    }

    private static boolean isNestedObject(Object value) {
        return !(value instanceof String)
               && !(value instanceof Collection)
               && !value.getClass().isArray()
               && !value.getClass().isPrimitive()
               && !isJavaNativePackage(value);
    }

    private static boolean isJavaNativePackage(Object value) {
        var packageName = value.getClass().getPackageName();
        return packageName.startsWith(JAVA_PACKAGE) && packageName.startsWith(JAVAX_PACKAGE);
    }

    public static class MissingFieldException extends RuntimeException {

        public MissingFieldException(String string) {
            super(string);
        }
    }
}