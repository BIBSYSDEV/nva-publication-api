package no.unit.nva.cristin;

import static no.unit.nva.cristin.CristinImportConfig.eventHandlerObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Failure;
import nva.commons.core.ioutils.IoUtils;

public class AbstractCristinImportTest extends ResourcesLocalTest {
    
    public static final Integer NUMBER_OF_LINES_IN_RESOURCES_FILE = 100;
    public static final CollectionType CRISTIN_OBJECTS_LIST_JAVATYPE =
        eventHandlerObjectMapper.getTypeFactory().constructCollectionType(List.class, CristinObject.class);
    public static final String TESTING_DATA_INITIALIZATION_ERROR =
        "Set the field testingData before calling this method";
    public static final String INVALID_DATA_ERROR_MESSAGE = "The 'testingData' field does not contain valid data";
    
    protected String testingData;
    
    /**
     * Class returning cristinObjects parsed from the field {@link AbstractCristinImportTest#testingData}. The method
     * expects that the class that extends {@link AbstractCristinImportTest} has set the
     * {@link AbstractCristinImportTest#testingData} field before.
     *
     * @return a stream of CristinObject instances.
     */
    public Stream<CristinObject> cristinObjects() {
        if (StringUtils.isBlank(testingData)) {
            throw new IllegalStateException(TESTING_DATA_INITIALIZATION_ERROR);
        }
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
        return attempt(() -> eventHandlerObjectMapper.<List<CristinObject>>readValue(jsonString,
            CRISTIN_OBJECTS_LIST_JAVATYPE))
                   .orElseThrow();
    }
    
    private Stream<CristinObject> readSeriesOfJsonObjects() {
        return newContentReader().lines()
                   .map(attempt(line -> eventHandlerObjectMapper.readValue(line, CristinObject.class)))
                   .map(attempt -> attempt.orElseThrow(this::handleError));
    }
    
    private RuntimeException handleError(Failure<CristinObject> fail) {
        return new RuntimeException(INVALID_DATA_ERROR_MESSAGE, fail.getException());
    }
    
    private BufferedReader newContentReader() {
        return attempt(() -> new BufferedReader(new InputStreamReader(IoUtils.stringToStream(testingData))))
                   .orElseThrow();
    }
}
