package no.unit.nva.cristin;

import static nva.commons.core.JsonUtils.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.testutils.IoUtils;
import nva.commons.core.attempt.Try;

public class AbstractCristinImportTest extends ResourcesDynamoDbLocalTest {

    public static final Integer NUMBER_OF_LINES_IN_RESOURCES_FILE = 100;
    public static final CollectionType CRISTING_OBJECTS_LIST_JAVATYPE =
        objectMapperWithEmpty.getTypeFactory().constructCollectionType(List.class, CristinObject.class);
    protected String testingData;



    public Stream<CristinObject> cristinObjects() {
        return attempt(this::readJsonArray).orElse(fail -> readSeriesOfJsonObjects());
    }

    private Stream<CristinObject> readJsonArray() {
        try (BufferedReader reader = newContentReader()) {
            String jsonString = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            return parseCristinObjectsArray(jsonString).stream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<CristinObject> parseCristinObjectsArray(String jsonString) {
        return attempt(() -> objectMapperWithEmpty.<List<CristinObject>>readValue(jsonString,
                                                                                  CRISTING_OBJECTS_LIST_JAVATYPE))
                   .orElseThrow();
    }

    private Stream<CristinObject> readSeriesOfJsonObjects() {
        return newContentReader().lines()
                   .map(attempt(line -> objectMapperWithEmpty.readValue(line, CristinObject.class)))
                   .map(Try::orElseThrow);
    }

    private BufferedReader newContentReader() {
        return attempt(() -> new BufferedReader(new InputStreamReader(IoUtils.stringToStream(testingData))))
                   .orElseThrow();
    }
}
