package no.unit.nva.publication.migration;

import static nva.commons.core.JsonUtils.objectMapperWithEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataMigrationHandlerTest extends AbstractDataMigrationTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final String SOME_S_3_LOCATION = "someS3Location";
    private DataMigrationHandler handler;

    @BeforeEach
    public void initialize() {
        super.init();
        FakeS3Driver s3Driver = new FakeS3Driver();
        handler = new DataMigrationHandler(client, s3Driver);
    }

    @Test
    public void handleRequestReturnsListOfUpdatesWhenInputRequestContainsS3ResourceFile() throws IOException {
        String importRequest = new DataMigrationRequest(SOME_S_3_LOCATION).toJsonString();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        handler.handleRequest(IoUtils.stringToStream(importRequest), outputStream, CONTEXT);
        List<ResourceUpdate> result = Arrays.asList(deserializeOutput(outputStream));

        assertThat(result, is(not(empty())));
    }

    private ResourceUpdate[] deserializeOutput(ByteArrayOutputStream outputStream)
        throws com.fasterxml.jackson.core.JsonProcessingException {
        return objectMapperWithEmpty.readValue(outputStream.toString(StandardCharsets.UTF_8), ResourceUpdate[].class);
    }
}