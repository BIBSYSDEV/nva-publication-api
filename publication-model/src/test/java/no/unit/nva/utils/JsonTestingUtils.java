package no.unit.nva.utils;

import java.util.regex.Pattern;

public final class JsonTestingUtils {

    private JsonTestingUtils() {

    }

    public static <T> Pattern typeFieldString(Class<T> expectedType) {
        String patternString = String.format(".*\"type\"\\s*:\\s*\"%s\".*",expectedType.getSimpleName());
        return Pattern.compile(patternString);
    }
}
