package no.unit.nva.publication.file.text;

import nva.commons.core.Environment;

public record SeedTextExtractionConfig(String sourceBucketName, String queueUrl) {

  private static final String SOURCE_BUCKET_ENV = "NVA_PERSISTED_STORAGE_BUCKET_NAME";
  private static final String QUEUE_URL_ENV = "TEXT_EXTRACTION_QUEUE_URL";

  public static SeedTextExtractionConfig fromEnvironment() {
    var environment = new Environment();
    return new SeedTextExtractionConfig(
        environment.readEnv(SOURCE_BUCKET_ENV), environment.readEnv(QUEUE_URL_ENV));
  }
}
