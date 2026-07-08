package no.unit.nva.publication.file.text;

/**
 * Identifies the source object to extract text from. {@code contentType} is the normalized media
 * type (lowercase, parameter-free, never null) used for extractor dispatch. {@code sourceEtag} is
 * the object version the metadata was resolved from; downloads are conditional on it, so text is
 * never extracted from a different version than the one dispatched on.
 */
public record ExtractionInput(
    String sourceBucket, String sourceKey, String sourceEtag, String contentType) {}
