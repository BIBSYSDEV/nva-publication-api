package no.unit.nva.publication.file.text;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

final class TikaSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(TikaSupport.class);
  private static final AutoDetectParser PARSER = new AutoDetectParser();
  private static final int MAX_EXTRACTED_CHARACTERS = 100_000_000;

  private TikaSupport() {
    // NO-OP
  }

  static String extractText(Path file, ParseContext context)
      throws TikaException, IOException, SAXException {
    return extractText(file, context, MAX_EXTRACTED_CHARACTERS);
  }

  static String extractText(Path file, ParseContext context, int characterLimit)
      throws TikaException, IOException, SAXException {
    var textBuffer = new StringWriter();
    var handler = new BodyContentHandler(new WriteOutContentHandler(textBuffer, characterLimit));
    try (var stream = TikaInputStream.get(file)) {
      PARSER.parse(stream, handler, new Metadata(), context);
    } catch (TikaException | SAXException exception) {
      if (WriteLimitReachedException.isWriteLimitReached(exception)) {
        LOGGER.warn("Extracted text truncated at {} characters", characterLimit);
      } else {
        throw exception;
      }
    }
    return textBuffer.toString();
  }
}
