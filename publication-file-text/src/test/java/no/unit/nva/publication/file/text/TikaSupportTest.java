package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ParseContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

class TikaSupportTest {

  private static final int TINY_CHARACTER_LIMIT = 5;
  private static final String LONG_CONTENT = "abcdefghijklmnopqrstuvwxyz";

  @TempDir Path tempDir;

  @Test
  void shouldTruncateExtractedTextWhenContentExceedsCharacterLimit()
      throws TikaException, IOException, SAXException {
    var file = tempDir.resolve("large.txt");
    Files.writeString(file, LONG_CONTENT);

    var text = TikaSupport.extractText(file, new ParseContext(), TINY_CHARACTER_LIMIT);

    assertThat(text).hasSize(TINY_CHARACTER_LIMIT);
    assertThat(LONG_CONTENT).contains(text.strip());
  }

  @Test
  void shouldReturnFullTextWhenContentIsWithinCharacterLimit()
      throws TikaException, IOException, SAXException {
    var file = tempDir.resolve("small.txt");
    Files.writeString(file, LONG_CONTENT);

    var text = TikaSupport.extractText(file, new ParseContext());

    assertThat(text).contains(LONG_CONTENT);
  }
}
