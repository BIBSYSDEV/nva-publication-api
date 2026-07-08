package no.unit.nva.publication.file.text;

/**
 * Port for extracting plain text from a stored file. Implementations are responsible for specific
 * content types and declare their support via {@link #supports(String)}, which must accept a null
 * argument and return {@code false} for it. The dispatcher routes each input to the first
 * supporting implementation; inputs no implementation supports are flagged {@link
 * ExtractionFailureReason#UNSUPPORTED_FORMAT} by the dispatcher itself.
 */
public interface TextExtractor {

  boolean supports(String contentType);

  ExtractionResult extract(ExtractionInput input);
}
