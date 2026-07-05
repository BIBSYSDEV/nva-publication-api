package no.unit.nva.publication.file.text;

/**
 * Identifies the source object to extract text from. {@code contentType} is the MIME type declared
 * by the sender of the extraction request; extractors use it for dispatch.
 */
public record ExtractionInput(
    String sourceBucket, String sourceKey, String sourceEtag, String contentType) {}
