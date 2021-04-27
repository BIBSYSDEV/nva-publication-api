package no.unit.nva.cristin.lambda;

import static no.unit.nva.testutils.IoUtils.stringToStream;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class FilenameEventEmitterTest {

    public static final String SOME_S3_LOCATION = "s3://some/location";
    private static final Context CONTEXT = mock(Context.class);
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private FilenameEventEmitter handler = new FilenameEventEmitter();

    @BeforeEach
    public void init() {
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handleRequestThrowsExceptionWhenInputIsInvalid() throws JsonProcessingException {
        String json = invalidBody();
        Executable action = () -> handler.handleRequest(stringToStream(json), outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(json));
    }

    @Test
    public void handleThrowsNoExceptionWhenInputIsValid() throws JsonProcessingException {
        ImportRequest importRequest = new ImportRequest(SOME_S3_LOCATION);
        String json = JsonUtils.objectMapperNoEmpty.writeValueAsString(importRequest);
        Executable action = () -> handler.handleRequest(stringToStream(json), outputStream, CONTEXT);
        assertDoesNotThrow(action);
    }

    private String invalidBody() throws JsonProcessingException {
        ObjectNode root = JsonUtils.objectMapperWithEmpty.createObjectNode();
        root.put("someField", "someValue");
        return JsonUtils.objectMapperWithEmpty.writeValueAsString(root);
    }
}