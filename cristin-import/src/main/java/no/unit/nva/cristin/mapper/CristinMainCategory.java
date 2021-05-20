package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import nva.commons.core.SingletonCollector;

public enum CristinMainCategory {
    BOOK("BOK", "BOOK"), UNMAPPED;

    public static final int DEFAULT_VALUE = 0;
    private final List<String> aliases;

    CristinMainCategory(String... mapping) {
        aliases = Arrays.asList(mapping);
    }

    @JsonCreator
    public static CristinMainCategory fromString(String category) {
        return Arrays.stream(values())
                   .filter(item -> item.aliases.contains(category))
                   .collect(SingletonCollector.collectOrElse(UNMAPPED));
    }

    public String getValue() {
        if (Objects.nonNull(aliases) && !aliases.isEmpty()) {
            return aliases.get(DEFAULT_VALUE);
        }
        return this.name();
    }

    public boolean isUnknownCategory() {
        return UNMAPPED.equals(this);
    }
}
