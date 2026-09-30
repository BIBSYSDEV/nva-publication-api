package no.unit.nva.publication.file.text;

/**
 * The outcome of a text extraction attempt over a downloaded file. {@link Extracted} carries the
 * text; {@code truncated} marks text cut off at the extraction character cap. {@link Flagged}
 * records why extraction was not possible; every flag is persisted as an {@link ExtractionFlag}
 * marker in the text bucket so affected files can be enumerated and redriven. Infrastructure
 * failures are not modeled here — they are thrown, retried, and eventually dead-lettered.
 */
public sealed interface ExtractionResult
    permits ExtractionResult.Extracted, ExtractionResult.Flagged {

  record Extracted(ExtractionInput source, String text, boolean truncated)
      implements ExtractionResult {}

  record Flagged(ExtractionInput source, ExtractionFailureReason reason, String detail)
      implements ExtractionResult {}
}
