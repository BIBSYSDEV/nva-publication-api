package no.unit.nva.cristin;

import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.EVENT_SUBTOPIC;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class Utf8Tester {

    @Test
    void shouldReturnBodyCorrectly() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject).toJsonString();
        var inputstream = stringCompressor(eventBody);
        var outputBody = readCompressedStream(inputstream);

        assertThat(eventBody, is(equalTo(outputBody)));
    }

    @Test
    void shouldReturnBodyFailing() throws IOException {
        var cristinObject = CristinDataGenerator.randomObject();
        var eventBody = createEventBody(cristinObject).toJsonString();
        var inputstream = stringCompressorUtf8(eventBody);
        var outputBody = readCompressedStream(inputstream);

        Assert.assertNotEquals(eventBody, is(equalTo(outputBody)));
    }

    private <T> FileContentsEvent<T> createEventBody(T cristinObject) {
        return new FileContentsEvent<>(randomString(), EVENT_SUBTOPIC, randomUri(), Instant.now(),
                                       cristinObject);
    }

    public InputStream stringCompressor(String input) {
        return Optional.of(new ByteArrayOutputStream())
                   .map(byteout -> {
                       try {
                           var gzip = new GZIPOutputStream(byteout, input.length());
                           gzip.write(input.getBytes());
                           gzip.finish();
                       } catch (IOException e) {
                           throw new RuntimeException(e);
                       }
                       return new ByteArrayInputStream(byteout.toByteArray());
                   }).get();
    }

    public InputStream stringCompressorUtf8(String input) {
        return Optional.of(new ByteArrayOutputStream())
                   .map(byteout -> {
                       try {
                           var gzip = new GZIPOutputStream(byteout, input.length());
                           gzip.write(input.getBytes(StandardCharsets.UTF_8));
                           gzip.finish();
                       } catch (IOException e) {
                           throw new RuntimeException(e);
                       }
                       return new ByteArrayInputStream(byteout.toByteArray());
                   }).get();
    }

    private String readCompressedStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(inputStream)))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    // add to gradle
    //    compileJava.options.encoding = 'utf-8'
    //
    //                                     tasks.withType(JavaCompile) {
    //        options.encoding = 'utf-8'
    //    }
}
