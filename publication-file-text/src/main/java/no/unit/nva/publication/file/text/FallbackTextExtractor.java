package no.unit.nva.publication.file.text;

/**
 * Terminal extractor that flags every input as unsupported. Registered last in the extractor list
 * so that it catches any content type not handled by a specialized implementation. Its {@link
 * #supports} always returns {@code true}.
 */
public final class FallbackTextExtractor implements TextExtractor {

  @Override
  public boolean supports(String contentType) {
    return true;
  }

  @Override
  public ExtractionResult extract(ExtractionInput input) {
    return new ExtractionResult.Flagged(
        input, ExtractionFailureReason.UNSUPPORTED_FORMAT, input.contentType());
  }
}
