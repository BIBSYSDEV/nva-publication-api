package no.unit.nva.cristin.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.publication.StubS3Driver;
import no.unit.nva.s3.S3Driver;
import org.junit.jupiter.api.Test;

public class CristinResourcesReaderTest {

    public static final String SOME_BUCKET_NAME = "someBucketName";
    public static final Path SOME_FOLDER = Path.of("some/folder");
    private static final String RESOURCE_01 = "input01.gz";
    public static final List<String> RESOURCES_LIST = List.of(RESOURCE_01);
    private static final String RESOURCE_01_AS_JSON_ARRAY = "input01AsArray.json.gz";
    private static final int NUMBER_OF_RECORDS_IN_RESOURCES = 100;

    @Test
    public void readCristinResourcesReturnsStreamOfCristinObjects() {
        S3Driver s3Driver = new StubS3Driver(SOME_BUCKET_NAME, List.of(RESOURCE_01));
        CristinReader reader = new CristinReader(s3Driver);
        List<CristinObject> cristinObjects = reader.readResources(SOME_FOLDER).collect(Collectors.toList());
        assertThat(cristinObjects, hasSize(NUMBER_OF_RECORDS_IN_RESOURCES));
    }

    @Test
    public void readCristinResourcesReturnsStreamOfCristinObjectsWhenInputIsJsonArray() {
        S3Driver s3Driver = new StubS3Driver(SOME_BUCKET_NAME, List.of(RESOURCE_01_AS_JSON_ARRAY));
        CristinReader reader = new CristinReader(s3Driver);
        List<CristinObject> cristinObjects = reader.readResources(SOME_FOLDER).collect(Collectors.toList());
        assertThat(cristinObjects, hasSize(NUMBER_OF_RECORDS_IN_RESOURCES));
    }
}
