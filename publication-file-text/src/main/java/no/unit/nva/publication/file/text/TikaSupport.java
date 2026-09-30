package no.unit.nva.publication.file.text;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.xml.sax.SAXException;

final class TikaSupport {

  static final int MAX_EXTRACTED_CHARACTERS = 100_000_000;
  private static final AutoDetectParser PARSER = new AutoDetectParser();

  private TikaSupport() {
    // NO-OP
  }

  record ExtractedText(String text, boolean truncated) {}

  static String detectContentType(Path file, String filename) {
    var metadata = new Metadata();
    metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
    try (var stream = TikaInputStream.get(file)) {
      return PARSER.getDetector().detect(stream, metadata).toString();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  static ExtractedText extractText(Path file, ParseContext context)
      throws TikaException, IOException, SAXException {
    return extractText(file, context, MAX_EXTRACTED_CHARACTERS);
  }

  static ExtractedText extractText(Path file, ParseContext context, int characterLimit)
      throws TikaException, IOException, SAXException {
    var textBuffer = new StringWriter();
    var handler = new BodyContentHandler(new WriteOutContentHandler(textBuffer, characterLimit));
    var truncated = false;
    try (var stream = TikaInputStream.get(file)) {
      PARSER.parse(stream, handler, new Metadata(), context);
    } catch (TikaException | SAXException exception) {
      if (WriteLimitReachedException.isWriteLimitReached(exception)) {
        truncated = true;
      } else {
        throw exception;
      }
    }
    return new ExtractedText(textBuffer.toString(), truncated);
  }
}
