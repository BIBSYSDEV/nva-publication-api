package no.unit.nva.publication.migration;

import static java.util.Objects.isNull;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import software.amazon.awssdk.regions.Region;

public class DataMigrationHandler implements RequestStreamHandler {

    private final ResourceService resourceService;
    private final DoiRequestService doiRequestService;
    private final MessageService messageService;
    private S3Driver s3Driver;

    @JacocoGenerated
    public DataMigrationHandler() {
        this(defaultDynamoDbClient());
    }

    @JacocoGenerated
    public DataMigrationHandler(AmazonDynamoDB dynamoDbClient) {
        resourceService = new ResourceService(dynamoDbClient, Clock.systemDefaultZone());
        doiRequestService = new DoiRequestService(dynamoDbClient, Clock.systemDefaultZone());
        messageService = new MessageService(dynamoDbClient, Clock.systemDefaultZone());
    }

    public DataMigrationHandler(AmazonDynamoDB dynamoDbClient, S3Driver s3Driver) {
        resourceService = new ResourceService(dynamoDbClient, Clock.systemDefaultZone());
        doiRequestService = new DoiRequestService(dynamoDbClient, Clock.systemDefaultZone());
        messageService = new MessageService(dynamoDbClient, Clock.systemDefaultZone());
        this.s3Driver = s3Driver;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        DataMigrationRequest request = JsonUtils.objectMapper.readValue(input, DataMigrationRequest.class);

        setupS3Driver(request);
        Path folderPath = Path.of(request.extractPathFromS3Location());
        DataMigration dataMigration = new DataMigration(s3Driver,
                                                        folderPath,
                                                        resourceService,
                                                        doiRequestService,
                                                        messageService);
        List<ResourceUpdate> result = dataMigration.migrateData();
        writeOutput(output, result);
    }

    protected <O> void writeOutput(OutputStream outputStream, O outputObject)
        throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String outputJson = JsonUtils.objectMapperWithEmpty.writeValueAsString(outputObject);
            writer.write(outputJson);
        }
    }

    @JacocoGenerated
    private static AmazonDynamoDB defaultDynamoDbClient() {
        Environment environment = new Environment();
        Region region = environment.readEnvOpt("AWS_REGION").map(Region::of).orElse(defaultRegion());

        return AmazonDynamoDBClientBuilder
                   .standard()
                   .withRegion(region.toString())
                   .build();
    }

    @JacocoGenerated
    private static Region defaultRegion() {
        return Region.of(Regions.EU_WEST_1.getName());
    }

    private void setupS3Driver(DataMigrationRequest request) {
        if (isNull(s3Driver)) {
            s3Driver = new S3Driver(request.extractBucketFromS3Location());
        }
    }
}
