package no.unit.nva.doi.event.producer;

import static no.unit.nva.doi.event.producer.DynamoDbFanoutPublicationDtoProducer.EMPTY_EVENT;
import static no.unit.nva.doi.event.producer.DynamoDbFanoutPublicationDtoProducer.NO_RESOURCE_IDENTIFIER_ERROR;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import no.unit.nva.publication.doi.update.dto.PublicationHolder;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DynamoDbFanoutPublicationDtoProducerTest {

    public static final String EVENT_PUBLICATION_WITH_DOI_IS_UPDATED =
        "resource_update_event_updated_metadata_with_existing_doi.json";
    private static final String RESOURCE_UPDATE_EVENT_OLD_AND_NEW_PRESENT_DIFFERENT =
        "resource_update_event_old_and_new_present_different.json";
    private static final String RESOURCE_UPDATE_EVENT_OLD_AND_NEW_PRESENT_EQUAL =
        "resource_update_event_old_and_new_present_equal.json";
    private static final String RESOURCE_UPDATE_EVENT_OLD_ONLY = "resource_update_event_old_only.json";
    private static final String RESOURCE_UPDATE_EVENT_NEW_ONLY = "resource_update_event_new_only.json";
    private static final String PUBLICATION_WITHOUT_DOI_REQUEST =
        "resource_update_event_publication_without_doi_request.json";
    private static final String PUBLICATION_WITHOUT_IDENTIFIER = "resource_update_event_publication_without_id.json";
    private DynamoDbFanoutPublicationDtoProducer handler;
    private Context context;
    private ByteArrayOutputStream outputStream;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        handler = new DynamoDbFanoutPublicationDtoProducer();
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handleRequestReturnsEmptyEventWhenThereIsNoDoiRequest() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(PUBLICATION_WITHOUT_DOI_REQUEST);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder output = outputToPublicationHolder(outputStream);
        assertThat(output, is(equalTo(EMPTY_EVENT)));
    }

    @Test
    public void handleRequestThrowsExceptionWhenEventContainsPublicationWithoutIdentifier() {
        var eventInputStream = IoUtils.inputStreamFromResources(PUBLICATION_WITHOUT_IDENTIFIER);
        Executable action = () -> handler.handleRequest(eventInputStream, outputStream, context);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getMessage(), is(equalTo(NO_RESOURCE_IDENTIFIER_ERROR)));
    }

    @Test
    void processInputCreatingDtosWhenOnlyNewImageIsPresentInDao() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(RESOURCE_UPDATE_EVENT_NEW_ONLY);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);
        assertThat(actual.getType(), is(equalTo(DynamoDbFanoutPublicationDtoProducer.TYPE_DTO_DOI_PUBLICATION)));
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
        var eventInputStream = IoUtils.inputStreamFromResources((RESOURCE_UPDATE_EVENT_OLD_ONLY));
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);
        assertThat(actual, is(equalTo(EMPTY_EVENT)));
    }

    @Test
    void processInputCreatesDtosWhenOldAndNewImageAreDifferent() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(RESOURCE_UPDATE_EVENT_OLD_AND_NEW_PRESENT_DIFFERENT);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);

        assertThat(actual.getType(), is(equalTo(DynamoDbFanoutPublicationDtoProducer.TYPE_DTO_DOI_PUBLICATION)));
        assertThat(actual.getItem(), notNullValue());
    }

    @Test
    void processInputSkipsCreatingDtosWhenOldAndNewImageAreEqual() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(RESOURCE_UPDATE_EVENT_OLD_AND_NEW_PRESENT_EQUAL);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);

        assertThat(actual, is(equalTo(EMPTY_EVENT)));
    }

    private PublicationHolder outputToPublicationHolder(ByteArrayOutputStream outputStream)
        throws JsonProcessingException {
        String outputString = outputStream.toString();
        return objectMapper.readValue(outputString, PublicationHolder.class);
    }
}