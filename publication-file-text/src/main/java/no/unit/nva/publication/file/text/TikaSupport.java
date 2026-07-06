package no.unit.nva.publication.file.text;

import org.apache.tika.parser.AutoDetectParser;

final class TikaSupport {

  static final AutoDetectParser PARSER = new AutoDetectParser();
  static final int UNLIMITED_CONTENT = -1;

  private TikaSupport() {
    // NO-OP
  }
}
