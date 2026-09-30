package no.unit.nva.publication.file.text;

import java.nio.file.Path;

/**
 * A source object downloaded to a local temp file. {@code etag} identifies the object version the
 * bytes were read from; because detection, extraction, and the etag all come from this single
 * download, they are consistent with one another by construction. The caller owns the temp file and
 * must delete it when processing completes.
 */
public record DownloadedObject(Path path, String etag) {}
