package no.sikt.nva.brage.migration.lambda.cleanup;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class ListImportedBragePublicationsHandler implements RequestHandler<String, List<String>> {

    public static final int MAX_KEYS = 10_000_000;
    private final S3Client s3Client;

    public ListImportedBragePublicationsHandler(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public List<String> handleRequest(String input, Context context) {
        var uri = getUri(input);
        var bucketName = extractBucketName(uri);
        var marker = extractMarker(uri);
        return attempt(() -> getIdentifiersStoredInS3(bucketName, marker)).orElseThrow();
    }

    private static String getUri(String input) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            var json = mapper.readValue(input, ObjectNode.class);
            return json.get("uri").toString().replaceAll("\"", "");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getIdentifier(String key) {
        return key.split("/")[key.split("/").length - 1];
    }

    private static ListObjectsRequest buildListObjectRequest(String bucketName, String marker) {
        return ListObjectsRequest.builder()
                   .bucket(bucketName)
                   .marker(marker)
                   .maxKeys(MAX_KEYS)
                   .build();
    }

    private static List<String> getIdentifiers(ListIterator<S3Object> iterator) {
        List<String> list = new ArrayList<>();
        while (iterator.hasNext()) {
            S3Object object = iterator.next();
            System.out.println(object.key() + " - " + object.size());
            list.add(getIdentifier(object.key()));
        }
        return list;
    }

    private String extractMarker(String input) {
        return URI.create(input).getPath();
    }

    private String extractBucketName(String input) {
        return URI.create(input).getHost();
    }

    private List<String> getIdentifiersStoredInS3(String bucketName, String marker) {
        var request = buildListObjectRequest(bucketName, marker);
        var iterator = s3Client.listObjects(request).contents().listIterator();
        return getIdentifiers(iterator);
    }
}
