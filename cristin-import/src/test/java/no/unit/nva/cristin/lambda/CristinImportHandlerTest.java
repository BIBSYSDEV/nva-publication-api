package no.unit.nva.cristin.lambda;

import static no.unit.nva.publication.PublicationGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.IoUtils;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinImportHandlerTest extends AbstractCristinImportTest {

    public static final String SOME_S3_LOCATION = "s3://some/location";
    public static final Context CONTEXT = mock(Context.class);
    public static final String RESOURCE_FILE = "input01";
    private CristinImportHandler handler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        super.init();
        AmazonDynamoDB dynamoDbClient = client;
        testingData = generateData();
        InputStream inputStream = IoUtils.stringToStream(testingData);
        S3Client s3Client = new FakeS3Client(Map.of(RESOURCE_FILE, inputStream));
        handler = new CristinImportHandler(s3Client, dynamoDbClient);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handlerWritesPublicationsToDynamoDbsFromCristinResourcesInSpecifiedS3Location() throws IOException {
        ImportRequest request = new ImportRequest(SOME_S3_LOCATION, randomString());

        handler.handleRequest(request.toInputStream(), outputStream, CONTEXT);

        List<String> expectedCristinIds = expectedCristinIds();
        List<String> actualCristinIds = extractActualIdsFromDatabase();

        assertThat(actualCristinIds, containsInAnyOrder(expectedCristinIds.toArray(String[]::new)));
    }

    private String generateData() {
        return attempt(CristinDataGenerator::new)
                   .map(CristinDataGenerator::randomDataAsString)
                   .orElseThrow();
    }

    private List<String> extractActualIdsFromDatabase() {
        ScanRequest scanRequest = new ScanRequest()
                                      .withTableName(DatabaseConstants.RESOURCES_TABLE_NAME)
                                      .withIndexName(DatabaseConstants.RESOURCES_BY_IDENTIFIER_INDEX_NAME);
        return client.scan(scanRequest)
                   .getItems()
                   .stream()
                   .map(ItemUtils::toItem)
                   .map(Item::toJSON)
                   .map(attempt(this::parseJson))
                   .map(Try::orElseThrow)
                   .map(Publication::getAdditionalIdentifiers)
                   .flatMap(Collection::stream)
                   .map(AdditionalIdentifier::getValue)
                   .collect(Collectors.toList());
    }

    private Publication parseJson(String json) throws com.fasterxml.jackson.core.JsonProcessingException {
        ResourceDao dao = JsonUtils.objectMapperWithEmpty.readValue(json, ResourceDao.class);
        return dao.getData().toPublication();
    }

    private List<String> expectedCristinIds() {
        return cristinObjects().map(CristinObject::getId).collect(Collectors.toList());
    }
}