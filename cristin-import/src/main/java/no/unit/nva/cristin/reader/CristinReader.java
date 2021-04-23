package no.unit.nva.cristin.reader;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Try;

/**
 * Reads specially tailored records of Cristing results from an S3 bucket and converts them to NVA Publications.
 */
public class CristinReader {

    public static final ObjectMapper OBJECT_MAPPER = JsonUtils.objectMapperWithEmpty;
    public static final CollectionType CRISTIN_OBJECTS_ARRAY =
        OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, CristinObject.class);

    private final S3Driver s3Driver;

    public CristinReader(S3Driver s3Driver) {
        this.s3Driver = s3Driver;
    }

    public Stream<CristinObject> readResources(Path folder) {
        List<String> filenames = s3Driver.listFiles(folder);
        return fetchAllObjectsInFolder(filenames);
    }

    public Stream<CristinObject> fetchFileContent(String filePath) {
        String fileContent = s3Driver.getFile(filePath);
        return attemptToReadFileAsJsonArray(fileContent)
                   .orElse(fail -> attemptToReadFileAsJsonObjectsList(fileContent));
    }

    private Stream<CristinObject> fetchAllObjectsInFolder(List<String> filenames) {
        return filenames.stream().flatMap(this::fetchFileContent);
    }

    private Try<Stream<CristinObject>> attemptToReadFileAsJsonArray(String fileContent) {
        return attempt(() -> parseAsJsonArray(fileContent)).map(Collection::stream);
    }

    private List<CristinObject> parseAsJsonArray(String fileContent) throws JsonProcessingException {
        return JsonUtils.objectMapperWithEmpty.readValue(fileContent, CRISTIN_OBJECTS_ARRAY);
    }

    private Stream<CristinObject> attemptToReadFileAsJsonObjectsList(String fileContent) {
        return Arrays.stream(fileContent.split(System.lineSeparator())).map(this::parseLine);
    }

    private CristinObject parseLine(String line) {
        return attempt(() -> JsonUtils.objectMapperWithEmpty.readValue(line, CristinObject.class)).orElseThrow();
    }
}
