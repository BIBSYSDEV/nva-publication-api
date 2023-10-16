package no.unit.nva.cristin.mapper;

import java.util.Objects;
import nva.commons.core.StringUtils;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public interface DescriptionExtractor {

    default String extractDescription(Stream<Optional<String>> fields) {
        return fields
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(System.lineSeparator()));
    }

    default Optional<String> createInformativeDescription(String information, String field) {
        return StringUtils.isNotEmpty(field)
            ? Optional.of(String.format(information, field))
            : Optional.empty();
    }

    default Optional<String> createInformativeDescription(String information, Integer field) {
        return Objects.nonNull(field)
                   ? Optional.of(String.format(information, field))
                   : Optional.empty();
    }

    default Optional<String> createInformativeDescription(String information, Double field) {
        return Objects.nonNull(field)
                   ? Optional.of(String.format(information, field))
                   : Optional.empty();
    }
}
