package no.unit.nva.dataimport;

import static no.unit.nva.dataimport.S3IonReader.FILE_NOT_FOUND_ERROR_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import no.unit.nva.publication.StubS3Driver;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.s3.UnixPath;
import nva.commons.core.SingletonCollector;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class S3IonReaderTest {

    public static final String SOME_BUCKET = "someBucket";
    public static final UnixPath RESOURCE_FILE = UnixPath.of("input1.ion.gz");
    public static final String UNIQUE_FIELD_FOR_EVERY_ENTRY = "SK0";
    public static final int CAPTURED_GROUP = 1;
    public static final Pattern UNIQUE_STRING_PATTERN = Pattern.compile(".*SK0:\"(.*?)\",.*");
    public static final UnixPath EMPTY_FILE = UnixPath.of("empty.ion.gz");
    public static final UnixPath NON_EXISTENT_FILE = UnixPath.of("non_existent_file");

    @Test
    public void extractJsonNodesFromS3FileReturnsListOfJsonNodesWhenInputFileisNonEmpty() throws IOException {
        S3Driver s3Driver = new StubS3Driver(SOME_BUCKET, List.of(RESOURCE_FILE));
        S3IonReader ionReader = new S3IonReader(s3Driver);
        List<JsonNode> result = ionReader.extractJsonNodesFromS3File(RESOURCE_FILE);
        List<String> actualIds = result.stream()
                                     .map(node -> node.get(UNIQUE_FIELD_FOR_EVERY_ENTRY))
                                     .map(JsonNode::textValue)
                                     .collect(Collectors.toList());
        List<String> expectedIds = extractIdsFromFile();
        assertThat(actualIds, containsInAnyOrder(expectedIds.toArray(String[]::new)));
    }

    @Test
    public void extractJsonNodesFromS3FileReturnsEmptyListWhenInputFileIsEmpty() throws IOException {
        S3Driver s3Driver = new StubS3Driver(SOME_BUCKET, List.of(EMPTY_FILE));
        S3IonReader ionReader = new S3IonReader(s3Driver);
        List<JsonNode> result = ionReader.extractJsonNodesFromS3File(EMPTY_FILE);
        assertThat(result, is(empty()));
    }

    @Test
    public void extractJsonNodesFromS3FileThrowsIllegalArgumentExceptionWhenInputFileDoesNotExist() {
        S3Driver s3Driver = new StubS3Driver(SOME_BUCKET, Collections.emptyList());
        S3IonReader ionReader = new S3IonReader(s3Driver);
        Executable action = () -> ionReader.extractJsonNodesFromS3File(NON_EXISTENT_FILE);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), is(equalTo(FILE_NOT_FOUND_ERROR_MESSAGE + NON_EXISTENT_FILE)));
    }

    private static List<String> extractIdsFromFile() throws IOException {
        var inputStream = new GZIPInputStream(IoUtils.inputStreamFromResources(RESOURCE_FILE.toString()));
        var bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        return bufferedReader.lines().map(S3IonReaderTest::getFieldFromString).collect(Collectors.toList());
    }

    private static String getFieldFromString(String input) {
        Matcher matcher = UNIQUE_STRING_PATTERN.matcher(input);
        return matcher.results().map(result -> result.group(CAPTURED_GROUP)).collect(SingletonCollector.collect());
    }
}