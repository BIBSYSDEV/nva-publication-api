package no.unit.nva.cristin.lambda;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.reader.S3CristinRecordsReader;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinImportHandler implements RequestStreamHandler {

    private final S3Client s3Client;
    private final AmazonDynamoDB dynamoDbClient;

    public CristinImportHandler(S3Client s3Client, AmazonDynamoDB dynamoDbClient) {
        this.s3Client = s3Client;

        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        ImportRequest request = ImportRequest.fromJson(IoUtils.streamToString(input));
        String bucket = request.extractBucketFromS3Location();
        String path = request.extractPathFromS3Location();
        extractPublicationsFromS3Location(bucket, path)
            .forEach(this::writeToDynamo);

        writeOutput(request, output);
    }

    private void writeToDynamo(Publication publication) {
        ResourceService resourceService = new ResourceService(dynamoDbClient, Clock.systemDefaultZone());
        attempt(() -> resourceService.createPublication(publication)).orElseThrow();
    }

    private Stream<Publication> extractPublicationsFromS3Location(String bucket, String path) {
        S3Driver s3Driver = new S3Driver(s3Client, bucket);
        S3CristinRecordsReader reader = new S3CristinRecordsReader(s3Driver);
        return reader.readResources(Path.of(path)).map(CristinObject::toPublication);
    }

    private <O> void writeOutput(O outputObject, OutputStream outputStream) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String outputJson = JsonUtils.objectMapperWithEmpty.writeValueAsString(outputObject);
            writer.write(outputJson);
        }
    }
}
