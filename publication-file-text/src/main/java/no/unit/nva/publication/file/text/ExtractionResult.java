package no.unit.nva.publication.file.text;

/**
 * The outcome of a text extraction attempt. {@link Extracted} carries the text. {@link Flagged}
 * records why extraction was not possible: {@link ExtractionFailureReason#EXTRACTION_ERROR} fails
 * the message so it is retried and eventually dead-lettered, while every other reason is persisted
 * as an {@link ExtractionFlag} marker in the text bucket so affected files can be enumerated and
 * redriven.
 */
public sealed interface ExtractionResult
    permits ExtractionResult.Extracted, ExtractionResult.Flagged {

  record Extracted(ExtractionInput source, String text) implements ExtractionResult {}

  record Flagged(ExtractionInput source, ExtractionFailureReason reason, String detail)
      implements ExtractionResult {}
}
