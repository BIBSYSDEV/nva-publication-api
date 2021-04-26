package no.unit.nva.cristin;

import static nva.commons.core.JsonUtils.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;

public class AbstractCristinImportTest extends ResourcesDynamoDbLocalTest {

    public static final String SAMPLE_INPUT_01 = "input01.gz";
    public static final Integer NUMBER_OF_LINES_IN_RESOURCES_FILE = 100;
    public static final CollectionType CRISTING_OBJECTS_LIST_JAVATYPE =
        objectMapperWithEmpty.getTypeFactory().constructCollectionType(List.class, CristinObject.class);

    public Stream<CristinObject> cristinObjects() throws IOException {
        return attempt(this::readJsonArray)
                   .orElse(fail -> readSeriesOfJsonObjects());
    }

    private Stream<CristinObject> readJsonArray() {
        try (BufferedReader reader = newReader()) {
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

    private Stream<CristinObject> readSeriesOfJsonObjects() throws IOException {
        return newReader().lines()
                   .map(attempt(line -> objectMapperWithEmpty.readValue(line, CristinObject.class)))
                   .map(Try::orElseThrow);
    }

    private BufferedReader newReader() throws IOException {
        InputStream inputStream = new GZIPInputStream(IoUtils.inputStreamFromResources(SAMPLE_INPUT_01));
        return new BufferedReader(new InputStreamReader(inputStream));
    }
}
