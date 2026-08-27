package no.unit.nva.publication.file.text;

import nva.commons.core.Environment;

public record TextExtractionConfig(String textBucketName) {

  private static final String TEXT_BUCKET_ENV = "TEXT_STORAGE_BUCKET_NAME";

  public static TextExtractionConfig fromEnvironment() {
    return new TextExtractionConfig(new Environment().readEnv(TEXT_BUCKET_ENV));
  }
}
