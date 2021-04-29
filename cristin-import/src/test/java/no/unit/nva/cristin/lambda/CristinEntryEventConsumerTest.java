package no.unit.nva.cristin.lambda;

import static java.util.Objects.nonNull;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.Publication;
import no.unit.nva.testutils.IoUtils;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CristinEntryEventConsumerTest {

    public static final Path CRISTIN_ENTRY_EVENT = Path.of("cristinentryevent.json");
    public static final Context CONTEXT = mock(Context.class);
    public static final String DETAIL_FIEL = "detail";
    private final CristinEntryEventConsumer handler = new CristinEntryEventConsumer();
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handlerReturnsAnNvaPublicationEntryWhenInputIsEventWithCristinResult() throws JsonProcessingException {
        String input = IoUtils.stringFromResources(CRISTIN_ENTRY_EVENT);
        handler.handleRequest(stringToStream(input), outputStream, CONTEXT);
        String json = outputStream.toString();
        Publication publication = JsonUtils.objectMapperNoEmpty.readValue(json, Publication.class);

        Publication expectedPublication = generatePublicationFromResource(input);

        assertThat(publication, is(equalTo(expectedPublication)));
    }

    private Publication generatePublicationFromResource(String input) throws JsonProcessingException {
        JsonNode jsonNode = JsonUtils.objectMapperNoEmpty.readTree(input);
        JsonNode detail = jsonNode.get(DETAIL_FIEL);
        CristinObject object = JsonUtils.objectMapperNoEmpty.convertValue(detail, CristinObject.class);
        assert nonNull(object.getId()); //java assertion produces Error not exception
        return object.toPublication();
    }
}