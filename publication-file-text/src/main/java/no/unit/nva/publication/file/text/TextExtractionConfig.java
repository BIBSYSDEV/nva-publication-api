package no.unit.nva.publication.file.text;

import nva.commons.core.Environment;

public record TextExtractionConfig(String sourceBucketName, String textBucketName) {

  private static final String SOURCE_BUCKET_ENV = "NVA_PERSISTED_STORAGE_BUCKET_NAME";
  private static final String TEXT_BUCKET_ENV = "TEXT_STORAGE_BUCKET_NAME";

  public static TextExtractionConfig fromEnvironment() {
    var environment = new Environment();
    return new TextExtractionConfig(
        environment.readEnv(SOURCE_BUCKET_ENV), environment.readEnv(TEXT_BUCKET_ENV));
  }
}
