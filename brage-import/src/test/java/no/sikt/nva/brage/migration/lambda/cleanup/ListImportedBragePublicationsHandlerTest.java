package no.sikt.nva.brage.migration.lambda.cleanup;

import static no.unit.nva.publication.s3imports.FilenameEventEmitter.FILENAME_EMISSION_EVENT_TOPIC;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ListImportedBragePublicationsHandlerTest {

    public static final Context context = mock(Context.class);
    public static final String HARDCODED_PATH = "reports/date/";
    public static final String EMPTY_SUBTOPIC = null;
    private static final Instant NOW = Instant.now();
    private final String bucketName = "some_bucket";
    private FakeS3Client s3Client;
    private ListImportedBragePublicationsHandler handler;

    @BeforeEach
    void init() {
        this.s3Client = new FakeS3Client();
        this.handler = new ListImportedBragePublicationsHandler(s3Client);
    }

    @Test
    void shouldReturnListOfImportedPublicationsFromS3() {
        var expectedIdentifiers = createRandomIdentifiers();
        putObjectsInBucket(expectedIdentifiers);
        var importRequest = new EventReference(FILENAME_EMISSION_EVENT_TOPIC, EMPTY_SUBTOPIC,
                                               URI.create("s3://brage-migration-reports-750639270376"), NOW);
        var actualList = handler.handleRequest(toJsonStream(importRequest), context);
        assertThat(actualList, is(equalTo(expectedIdentifiers)));
    }

    @Test
    void shouldThrowExceptionWhenFailsToParseUri() {
        var importRequest = new EventReference(FILENAME_EMISSION_EVENT_TOPIC, EMPTY_SUBTOPIC, null, NOW);
        assertThrows(RuntimeException.class, () -> handler.handleRequest(toJsonStream(importRequest), context));
    }

    private List<String> createRandomIdentifiers() {
        return IntStream.range(0, 2000).boxed().map(index -> randomString()).collect(Collectors.toList());
    }

    private void putObjectsInBucket(List<String> keys) {
        keys.forEach(object -> s3Client.putObject(
            PutObjectRequest.builder().bucket(bucketName).key(HARDCODED_PATH + object).build(),
            RequestBody.empty()));
    }

    private <T> InputStream toJsonStream(T importRequest) {
        return attempt(() -> s3ImportsMapper.writeValueAsString(importRequest))
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }
}
