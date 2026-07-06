package no.unit.nva.publication.file.text;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class TextExtractionHandler implements RequestHandler<SQSEvent, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TextExtractionHandler.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String TEXT_KEY_SUFFIX = ".txt";

  private final S3Client s3Client;
  private final ObjectMetadataSource metadataSource;
  private final TextExtractionConfig config;
  private final List<TextExtractor> extractors;

  @JacocoGenerated
  public TextExtractionHandler() {
    this(S3Driver.defaultS3Client().build(), TextExtractionConfig.fromEnvironment());
  }

  @JacocoGenerated
  TextExtractionHandler(S3Client s3Client, TextExtractionConfig config) {
    this(
        s3Client,
        new S3ObjectMetadataSource(s3Client),
        config,
        List.of(
            new PdfTextExtractor(new S3FileDownloadSource(s3Client)),
            new WordTextExtractor(new S3FileDownloadSource(s3Client)),
            new LatexTextExtractor(new S3FileDownloadSource(s3Client)),
            new FallbackTextExtractor()));
  }

  @JacocoGenerated
  public TextExtractionHandler(
      S3Client s3Client, TextExtractionConfig config, List<TextExtractor> extractors) {
    this(s3Client, new S3ObjectMetadataSource(s3Client), config, extractors);
  }

  public TextExtractionHandler(
      S3Client s3Client,
      ObjectMetadataSource metadataSource,
      TextExtractionConfig config,
      List<TextExtractor> extractors) {
    this.s3Client = s3Client;
    this.metadataSource = metadataSource;
    this.config = config;
    this.extractors = extractors;
  }

  @Override
  public Void handleRequest(SQSEvent event, Context context) {
    event.getRecords().forEach(this::processMessage);
    return null;
  }

  private void processMessage(SQSMessage message) {
    var request = parseRequest(message.getBody());
    var metadata = metadataSource.fetchMetadata(request.bucket(), request.key());
    var input =
        new ExtractionInput(
            request.bucket(), request.key(), metadata.etag(), metadata.contentType());
    handleResult(dispatch(input));
  }

  private TextExtractionRequest parseRequest(String body) {
    try {
      return OBJECT_MAPPER.readValue(body, TextExtractionRequest.class);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unparseable SQS message body", exception);
    }
  }

  private ExtractionResult dispatch(ExtractionInput input) {
    return extractors.stream()
        .filter(extractor -> extractor.supports(input.contentType()))
        .findFirst()
        .orElseThrow()
        .extract(input);
  }

  private void handleResult(ExtractionResult result) {
    switch (result) {
      case ExtractionResult.Extracted extracted -> storeText(extracted);
      case ExtractionResult.Flagged flagged -> logFlag(flagged);
    }
  }

  private void storeText(ExtractionResult.Extracted extracted) {
    var textKey = extracted.source().sourceKey() + TEXT_KEY_SUFFIX;
    try {
      new S3Driver(s3Client, config.textBucketName())
          .insertFile(UnixPath.of(textKey), extracted.text());
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    LOGGER.info("Stored extracted text: key={}", textKey);
  }

  private void logFlag(ExtractionResult.Flagged flagged) {
    LOGGER.warn(
        "Extraction flagged: bucket={} key={} etag={} reason={} detail={}",
        flagged.source().sourceBucket(),
        flagged.source().sourceKey(),
        flagged.source().sourceEtag(),
        flagged.reason(),
        flagged.detail());
  }
}
