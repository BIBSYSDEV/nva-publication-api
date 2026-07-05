package no.unit.nva.publication.file.text;

/** The content type and ETag of a stored object, resolved before extraction is dispatched. */
public record ObjectMetadata(String etag, String contentType) {}
