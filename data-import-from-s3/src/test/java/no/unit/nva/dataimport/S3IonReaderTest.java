package no.unit.nva.dataimport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import nva.commons.core.SingletonCollector;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

class S3IonReaderTest {

    public static final String SOME_BUCKET = "someBucket";
    public static final String RESOURCE_FILE = "input1.ion.gz";
    public static final String UNIQUE_FIELD_FOR_EVERY_ENTRY = "SK0";
    public static final int CAPTURED_GROUP = 1;
    public static final Pattern PATTERN = Pattern.compile(".*SK0:\"(.*?)\",.*");
    public static final int MATCHING_GROUP = 1;

    @Test
    public void extractJsonNodesFromS3FileReturnsListOfJsonNodesFromIonFile() throws IOException {
        StubS3Driver s3Driver = new StubS3Driver(SOME_BUCKET, List.of(RESOURCE_FILE));
        S3IonReader ionReader = new S3IonReader(s3Driver);
        List<JsonNode> result = ionReader.extractJsonNodesFromS3File(RESOURCE_FILE);
        List<String> actualIds = result.stream()
                                     .map(node -> node.get(UNIQUE_FIELD_FOR_EVERY_ENTRY))
                                     .map(JsonNode::textValue)
                                     .collect(Collectors.toList());
        List<String> expectedIds = extractIdsFromFile(RESOURCE_FILE);
        assertThat(actualIds, containsInAnyOrder(expectedIds.toArray(String[]::new)));
    }

    private static List<String> extractIdsFromFile(String filename) throws IOException {
        GZIPInputStream inputStream = new GZIPInputStream(IoUtils.inputStreamFromResources(RESOURCE_FILE));
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        List<String> ids = bufferedReader.lines().map(S3IonReaderTest::getFieldFromString).collect(Collectors.toList());
        return ids;
    }

    private static String getFieldFromString(String input) {
        Pattern pattern = PATTERN;
        Matcher matcher = pattern.matcher(input);
        return matcher.results().map(result -> result.group(MATCHING_GROUP)).collect(SingletonCollector.collect());
    }
}