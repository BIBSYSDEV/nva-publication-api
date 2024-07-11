package no.unit.nva.model.util;

import static java.util.Objects.nonNull;
import java.util.Collections;
import java.util.List;

public final class SerializationUtils {

    private SerializationUtils() {

    }

    public static <T> List<T> nullListAsEmpty(List<T> list) {
        return nonNull(list) ? list : Collections.emptyList();
    }
}
