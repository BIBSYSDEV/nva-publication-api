package no.unit.nva.publication.file.text;

import java.nio.file.Path;

/**
 * Port for determining the media type of a downloaded file from its bytes. Implementations return
 * the canonical, lowercase, parameter-free media type and never return null; content that cannot be
 * identified is reported as {@code application/octet-stream}. The {@code filename} is a detection
 * hint (extension globs) for formats whose bytes alone are ambiguous.
 */
@FunctionalInterface
public interface ContentTypeDetector {

  String detectContentType(Path file, String filename);
}
