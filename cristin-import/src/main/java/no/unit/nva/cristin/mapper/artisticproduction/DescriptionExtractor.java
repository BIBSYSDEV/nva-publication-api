package no.unit.nva.cristin.mapper.artisticproduction;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;



public interface DescriptionExtractor {

     default String extractDescription(String... fields) {
         var validFields = Arrays.stream(fields)
             .filter(Objects::nonNull)
             .collect(Collectors.toList());
         return String.join(", ", validFields);

    }
}
