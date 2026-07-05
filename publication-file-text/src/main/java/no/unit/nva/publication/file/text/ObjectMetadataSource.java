package no.unit.nva.publication.file.text;

/**
 * Port for resolving the content type and ETag of a stored object before extraction is dispatched.
 * The production adapter issues a HeadObject call; test implementations may supply fixed values.
 */
@FunctionalInterface
public interface ObjectMetadataSource {

  ObjectMetadata fetchMetadata(String bucket, String key);
}
