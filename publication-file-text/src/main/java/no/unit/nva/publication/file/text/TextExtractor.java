package no.unit.nva.publication.file.text;

/**
 * Port for extracting plain text from a stored file. Implementations are responsible for a specific
 * content type and declare their support via {@link #supports(String)}. A registry dispatches to
 * the first supporting implementation; {@link FallbackTextExtractor} acts as the terminal entry
 * that flags any unrecognized format.
 */
public interface TextExtractor {

  boolean supports(String contentType);

  ExtractionResult extract(ExtractionInput input);
}
