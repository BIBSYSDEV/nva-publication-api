package no.unit.nva.cristin.mapper;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;
import nva.commons.core.StringUtils;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public interface DescriptionExtractor {

    String ONE_DECIMAL = "#.0#####";
    String NORWEGIAN_BOKMAAL = "nb";

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
        var decimalFormat = new DecimalFormat(ONE_DECIMAL,
                                              DecimalFormatSymbols.getInstance(new Locale(NORWEGIAN_BOKMAAL)));
        return Objects.nonNull(field)
                   ? Optional.of(String.format(information, decimalFormat.format(field)))
                   : Optional.empty();
    }
}
