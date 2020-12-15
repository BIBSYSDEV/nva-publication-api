package no.unit.nva.doi.event.producer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Path;
import no.unit.nva.publication.doi.dto.PublicationHolder;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordImageDao;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DynamoDbFanoutPublicationDtoProducerTest {

    public static final String EXAMPLE_NAMESPACE = "https://example.net/unittest/namespace/";
    public static final String DOI_PUBLICATION_TYPE = "doi.publication";
    public static final Path EVENT_PUBLICATION_WITH_DOI_IS_UPDATED = Path.of(
        "dynamodbevent_updated_metadata_with_existing_doi.json");
    private static final Path DYNAMODB_STREAM_EVENT_OLD_AND_NEW_PRESENT_DIFFERENT =
        Path.of("dynamodbevent_old_and_new_present_different.json");
    private static final Path DYNAMODB_STREAM_EVENT_OLD_AND_NEW_PRESENT_EQUAL =
        Path.of("dynamodbevent_old_and_new_present_equal.json");
    private static final Path DYNAMODB_STREAM_EVENT_OLD_ONLY = Path.of("dynamodbevent_old_only.json");
    private static final Path DYNAMODB_STREAM_EVENT_NEW_ONLY = Path.of("dynamodbevent_new_only.json");
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    private static final String PUBLICATION_WITHOUT_DOI_REQUEST = "dynamodbevent_publication_wiithout_doi_request.json";
    private static final String PUBLICATION_WITHOUT_IDENTIFIER = "dynamodbevent_publication_without_id.json";
    private static final String NULL_AS_STRING = "null";
    private DynamoDbFanoutPublicationDtoProducer handler;
    private Context context;
    private ByteArrayOutputStream outputStream;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        handler = new DynamoDbFanoutPublicationDtoProducer(EXAMPLE_NAMESPACE);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handleRequestReturnsEmptyInputWhenThereIsNoDoiRequest() {
        var eventInputStream = IoUtils.inputStreamFromResources(Path.of(PUBLICATION_WITHOUT_DOI_REQUEST));
        handler.handleRequest(eventInputStream, outputStream, context);
        assertThat(outputStream.toString(), is(NULL_AS_STRING));
    }

    @Test
    public void handleRequestThrowsExceptionWhenEventContainsPublicationWithoutIdentifier() {
        var eventInputStream = IoUtils.inputStreamFromResources(Path.of(PUBLICATION_WITHOUT_IDENTIFIER));
        Executable action = () -> handler.handleRequest(eventInputStream, outputStream, context);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getMessage(), is(equalTo(DynamodbStreamRecordImageDao.PUBLICATION_MISSING_IDENTIFIER)));
    }

    @Test
    void processInputCreatingDtosWhenOnlyNewImageIsPresentInDao() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(DYNAMODB_STREAM_EVENT_NEW_ONLY);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);
        assertThat(actual.getType(), is(equalTo(DOI_PUBLICATION_TYPE)));
        assertThat(actual.getItem(), notNullValue());
    }

    @Test
    void handlerCreatesOutputWithNonEmptyDoiWhenNewImageHasPublicationWithDoi() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(EVENT_PUBLICATION_WITH_DOI_IS_UPDATED);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);
        URI doiInResourceFile = URI.create("https://doi.org/10.1103/physrevd.100.085005");
        URI actualDoi = actual.getItem().getDoi();
        assertThat(actualDoi, is(equalTo(doiInResourceFile)));
    }

    @Test
    void processInputSkipsCreatingDtosWhenNoNewImageIsPresentInDao() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources((DYNAMODB_STREAM_EVENT_OLD_ONLY));
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);
        assertThat(actual, nullValue());
    }

    @Test
    void processInputCreatesDtosWhenOldAndNewImageAreDifferent() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(DYNAMODB_STREAM_EVENT_OLD_AND_NEW_PRESENT_DIFFERENT);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);

        assertThat(actual.getType(), is(equalTo(DOI_PUBLICATION_TYPE)));
        assertThat(actual.getItem(), notNullValue());
    }

    @Test
    void processInputSkipsCreatingDtosWhenOldAndNewImageAreEqual() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(DYNAMODB_STREAM_EVENT_OLD_AND_NEW_PRESENT_EQUAL);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);

        assertThat(actual, nullValue());
    }

    private PublicationHolder outputToPublicationHolder(ByteArrayOutputStream outputStream)
        throws JsonProcessingException {
        String outputString = outputStream.toString();
        return objectMapper.readValue(outputString, PublicationHolder.class);
    }
}