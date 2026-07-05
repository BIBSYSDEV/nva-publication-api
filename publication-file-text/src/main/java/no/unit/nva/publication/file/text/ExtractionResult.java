package no.unit.nva.publication.file.text;

/**
 * The outcome of a text extraction attempt. {@link Extracted} carries the text; {@link Flagged}
 * records why extraction was not possible so the originating file can be found and the issue
 * diagnosed or redriven.
 */
public sealed interface ExtractionResult
    permits ExtractionResult.Extracted, ExtractionResult.Flagged {

  record Extracted(ExtractionInput source, String text) implements ExtractionResult {}

  record Flagged(ExtractionInput source, ExtractionFailureReason reason, String detail)
      implements ExtractionResult {}
}
