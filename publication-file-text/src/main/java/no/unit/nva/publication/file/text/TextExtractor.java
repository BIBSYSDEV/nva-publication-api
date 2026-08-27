package no.unit.nva.publication.file.text;

import java.nio.file.Path;

/**
 * Port for extracting plain text from an already-downloaded file. {@code contentType} is the
 * canonical media type detected from the file's bytes — never null, lowercase, parameter-free — so
 * implementations match against exact canonical types. The dispatcher routes each input to the
 * first supporting implementation; inputs no implementation supports are flagged {@link
 * ExtractionFailureReason#UNSUPPORTED_FORMAT} by the dispatcher itself. Implementations must not
 * delete the file; its lifecycle belongs to the caller.
 */
public interface TextExtractor {

  boolean supports(String contentType);

  ExtractionResult extract(ExtractionInput input, Path file);
}
