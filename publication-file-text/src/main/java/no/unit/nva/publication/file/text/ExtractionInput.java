package no.unit.nva.publication.file.text;

/**
 * Identifies the source object a downloaded file came from. {@code contentType} is the canonical
 * media type detected from the downloaded bytes (never null; unknown content is {@code
 * application/octet-stream}). {@code sourceEtag} is the version the single GetObject actually read,
 * so it is consistent with both the detected type and the extracted text by construction.
 */
public record ExtractionInput(
    String sourceBucket, String sourceKey, String sourceEtag, String contentType) {}
