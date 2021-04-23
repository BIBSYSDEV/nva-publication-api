package no.unit.nva.cristin.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.CristinMapper;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.reader.CristinReader;
import no.unit.nva.model.Publication;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinImportHandler implements RequestStreamHandler {

    private final S3Client s3Client;

    public CristinImportHandler(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        ImportRequest request = ImportRequest.fromJson(IoUtils.streamToString(input));
        String bucket = request.extractBucketFromS3Location();
        String path = request.extractPathFromS3Location();
        Stream<Publication> publications = extractPublicationsFromS3Location(bucket, path);
        List<JsonNode> result = publications
                                    .map(this::serializeWithTypes)
                                    .collect(Collectors.toList());
        writeToDynamo();

        writeOutput(result, output);
    }

    @JacocoGenerated
    private void writeToDynamo() {
        //TODO: write toDynamo
    }

    // Workaround for the fact that Jackson serializer does not put types when serializing object collections.
    private JsonNode serializeWithTypes(Publication p) {
        return JsonUtils.objectMapperWithEmpty.convertValue(p, JsonNode.class);
    }

    private Stream<Publication> extractPublicationsFromS3Location(String bucket, String path) {
        S3Driver s3Driver = new S3Driver(s3Client, bucket);
        CristinReader reader = new CristinReader(s3Driver);
        List<CristinObject> resources = reader.readResources(Path.of(path)).collect(Collectors.toList());
        return resources.stream()
                   .map(CristinMapper::new)
                   .map(CristinMapper::generatePublication);
    }

    private <O> void writeOutput(O outputObject, OutputStream outputStream) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String outputJson = JsonUtils.objectMapperWithEmpty.writeValueAsString(outputObject);
            writer.write(outputJson);
        }
    }
}
