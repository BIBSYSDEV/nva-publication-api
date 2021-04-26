package no.unit.nva.cristin.reader;

import static nva.commons.core.JsonUtils.objectMapperNoEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.IoUtils;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class CristinResourcesReaderTest extends AbstractCristinImportTest {

    public static final String SOME_BUCKET_NAME = "someBucketName";
    public static final Path SOME_FOLDER = Path.of("some/folder");
    private static final String RESOURCE_01 = "input01";
    private static final String RESOURCE_01_AS_JSON_ARRAY = "input01AsArray";
    private static final int NUMBER_OF_RECORDS_IN_RESOURCES = 100;
    private FakeS3Client s3Client;

    @BeforeEach
    public void init() {
        super.init();
        content = new CristinDataGenerator().randomDataAsString();
        s3Client = new FakeS3Client(Map.of(RESOURCE_01, IoUtils.stringToStream(content)));
    }

    @Test
    @Tag("RemoteTest")
    public void readCristinResourcesReturnsStreamOfCristinObjectsFromRealS3LocationWithCristinResources() {
        String bucket = new Environment().readEnv("AWS_BUCKET");
        S3Driver s3Driver = new S3Driver(bucket);
        S3CristinRecordsReader reader = new S3CristinRecordsReader(s3Driver);
        List<CristinObject> cristinObjects = reader.readResources(Path.of("")).collect(Collectors.toList());
        assertThat(cristinObjects.size(), is(greaterThan(0)));
    }

    @Test
    public void readCristinResourcesReturnsStreamOfCristinObjectsWhenInputContainsS3LocationWithCristinResources() {
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKET_NAME);
        S3CristinRecordsReader reader = new S3CristinRecordsReader(s3Driver);
        List<CristinObject> cristinObjects = reader.readResources(SOME_FOLDER).collect(Collectors.toList());
        assertThat(cristinObjects, hasSize(NUMBER_OF_RECORDS_IN_RESOURCES));
    }

    @Test
    public void readCristinResourcesReturnsStreamOfCristinObjectsWhenInputIsJsonArray() throws JsonProcessingException {
        FakeS3Client s3Client = CristinObjectsAsJsonArray();

        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKET_NAME);
        S3CristinRecordsReader reader = new S3CristinRecordsReader(s3Driver);
        List<CristinObject> cristinObjects = reader.readResources(SOME_FOLDER).collect(Collectors.toList());
        assertThat(cristinObjects, hasSize(NUMBER_OF_RECORDS_IN_RESOURCES));
    }

    private FakeS3Client CristinObjectsAsJsonArray() throws JsonProcessingException {
        List<JsonNode> jsonNodes = new CristinDataGenerator().randomObjects()
                                       .map(this::convertToJsonNode)
                                       .collect(Collectors.toList());
        ArrayNode arrayNode = objectMapperNoEmpty.createArrayNode();
        arrayNode.addAll(jsonNodes);
        String jsonArray = objectMapperNoEmpty.writeValueAsString(arrayNode);
        return new FakeS3Client(Map.of(RESOURCE_01_AS_JSON_ARRAY, IoUtils.stringToStream(jsonArray)));
    }

    private JsonNode convertToJsonNode(CristinObject cristinObject) {
        return objectMapperNoEmpty.convertValue(cristinObject, JsonNode.class);
    }
}
