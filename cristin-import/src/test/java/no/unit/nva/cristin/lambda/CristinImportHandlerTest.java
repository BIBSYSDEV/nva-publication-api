package no.unit.nva.cristin.lambda;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinImportHandlerTest extends AbstractCristinImportTest {

    public static final String SOME_S3_LOCATION = "s3://some/location";
    public static final Context CONTEXT = mock(Context.class);
    public static final String RESOURCE_FILE = "input01.gz";
    private CristinImportHandler handler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        S3Client s3Client = new FakeS3Client(RESOURCE_FILE);
        handler = new CristinImportHandler(s3Client);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handlerReturnsPublicationsWhenInputIsS3LocationWithCristinResources() throws IOException {
        ImportRequest request = new ImportRequest(SOME_S3_LOCATION);

        handler.handleRequest(request.toInputStream(), outputStream, CONTEXT);
        String outputString = outputStream.toString(StandardCharsets.UTF_8);

        List<Publication> generatedPublications = attempt(
            () -> JsonUtils.objectMapperWithEmpty.readValue(outputString, Publication[].class))
                                                      .map(Arrays::asList)
                                                      .orElseThrow();

        List<String> actualCristinIds = extractCristinIds(generatedPublications);
        List<String> expectedCristinIds = expectedCristinIds();

        assertThat(actualCristinIds, containsInAnyOrder(expectedCristinIds.toArray(String[]::new)));
    }

    private List<String> extractCristinIds(List<Publication> generatedPublications) {
        return generatedPublications.stream()
                   .map(Publication::getAdditionalIdentifiers)
                   .flatMap(Collection::stream)
                   .map(AdditionalIdentifier::getValue)
                   .collect(Collectors.toList());
    }

    private List<String> expectedCristinIds() throws IOException {
        return cristinObjects().map(CristinObject::getId).collect(Collectors.toList());
    }
}