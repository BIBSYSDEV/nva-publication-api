package no.unit.nva.publication.utils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Set;

public class DoesNotHaveEmptyValues {

    public static final String GET_PREFIX = "get";
    private static final ArrayList<String> emptyFields = new ArrayList<>();
    public static final String DELIMITER = ", ";

    private DoesNotHaveEmptyValues() {
        // NO-OP
    }

    public static void checkForEmptyFields(Object object, Set<String> excludeFields) {
        var classToTest = object.getClass();
        var fields = classToTest.isRecord() ? classToTest.getRecordComponents() : classToTest.getDeclaredFields();
        for (var field : fields) {
            var name = extractName(field);
            if (excludeFields.contains(name) || isNotVisibleField(field, name)) {
                continue;
            }
            checkForEmptiness(object, excludeFields, name);
        }

        if (!emptyFields.isEmpty()) {
            throw new IllegalArgumentException("Empty fields found: " + String.join(DELIMITER, emptyFields));
        }
    }

    private static boolean isNotVisibleField(AnnotatedElement element, String name) {
        return element instanceof Field field && isNotGetterField(field, name);
    }

    private static String extractName(AnnotatedElement element) {
        return switch (element) {
            case RecordComponent component -> component.getName();
            case Field field -> field.getName();
            default -> throw new IllegalArgumentException();
        };
    }

    private static void checkForEmptiness(Object object, Set<String> excludeFields, String name) {
        var value = getValueUsingGetter(object, name);
        checkValue(value, name);
        if (value != null) {
            checkForEmptyFields(value, excludeFields);
        }
    }

    private static void checkValue(Object value, String component) {
        if (value == null || (value instanceof String string && string.isEmpty())) {
            emptyFields.add(component);
        }
    }

    private static boolean isNotGetterField(Object object, String fieldName) {
        try {
            object.getClass().getMethod(formatGetterName(fieldName));
            return false;
        } catch (NoSuchMethodException e) {
            return true;
        }
    }

    private static Object getValueUsingGetter(Object object, String fieldName) {
        try {
            var getterName = object.getClass().isRecord() ? fieldName : formatGetterName(fieldName);
            var getterMethod = object.getClass().getMethod(getterName);
            return getterMethod.invoke(object);
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatGetterName(String fieldName) {
        return GET_PREFIX + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }
}