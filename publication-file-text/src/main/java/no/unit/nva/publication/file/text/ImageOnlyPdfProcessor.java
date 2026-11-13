package no.unit.nva.publication.file.text;

import java.nio.file.Path;

/**
 * Driven port for handling a PDF proven to be an image-only scan (see {@link PdfScanFingerprint}).
 * Text extraction cannot yield anything for such documents, so the pipeline routes them here
 * instead of parsing them. Implementations decide the outcome: the default {@link
 * FlaggingImageOnlyPdfProcessor} records an {@link ExtractionFailureReason#IMAGE_ONLY_CONTENT}
 * flag, keeping OCR candidates enumerable in the text bucket; an OCR-backed implementation can
 * return {@link ExtractionResult.Extracted} instead, with no other pipeline change. The downloaded
 * file exists for the duration of the call; implementations must not delete it.
 */
@FunctionalInterface
public interface ImageOnlyPdfProcessor {

  ExtractionResult process(ExtractionInput input, Path file, PdfScanFingerprint fingerprint);
}
