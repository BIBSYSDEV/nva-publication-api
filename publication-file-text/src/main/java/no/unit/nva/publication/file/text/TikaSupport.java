package no.unit.nva.publication.file.text;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

final class TikaSupport {

  static final AutoDetectParser PARSER = new AutoDetectParser();
  static final int UNLIMITED_CONTENT = -1;

  private TikaSupport() {
    // NO-OP
  }

  static String extractText(Path file, ParseContext context)
      throws TikaException, IOException, SAXException {
    var handler = new BodyContentHandler(UNLIMITED_CONTENT);
    try (var stream = TikaInputStream.get(file)) {
      PARSER.parse(stream, handler, new Metadata(), context);
    }
    return handler.toString();
  }
}
