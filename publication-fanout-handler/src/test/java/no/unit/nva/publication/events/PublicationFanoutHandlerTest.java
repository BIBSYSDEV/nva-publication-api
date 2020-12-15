package no.unit.nva.publication.events;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JsonUtils;
import nva.commons.utils.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PublicationFanoutHandlerTest {

    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    public static final String DYNAMODBEVENT_NEW_IMAGE_JSON = "dynamodbevent_new_image.json";
    public static final String DYNAMODBEVENT_INVALID_IMAGE_JSON = "dynamodbevent_invalid_image.json";
    public static final String DYNAMODBEVENT_NEW_AND_OLD_IMAGES_JSON = "dynamodbevent_new_and_old_images.json";
    public static final String DYNAMODBEVENT_OLD_IMAGE_JSON = "dynamodbevent_old_image.json";

    private OutputStream outputStream;
    private Context context;

    @BeforeEach
    public void setUp() {
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    public void handleRequestReturnsPublicationUpdateEventWhenEventContainsOnlyNewImage() {
        PublicationFanoutHandler handler = new PublicationFanoutHandler();

        InputStream inputStream = IoUtils.inputStreamFromResources(
                DYNAMODBEVENT_NEW_IMAGE_JSON);

        handler.handleRequest(inputStream, outputStream, context);

        PublicationUpdateEvent response = parseResponse();

        assertThat(response.getOldPublication(), is(nullValue()));
        assertThat(response.getNewPublication(), is(notNullValue()));
    }

    @Test
    public void handleRequestReturnsPublicationUpdateEventWhenEventContainsOnlyOldImage() {
        PublicationFanoutHandler handler = new PublicationFanoutHandler();

        InputStream inputStream = IoUtils.inputStreamFromResources(
                DYNAMODBEVENT_OLD_IMAGE_JSON);

        handler.handleRequest(inputStream, outputStream, context);

        PublicationUpdateEvent response = parseResponse();

        assertThat(response.getOldPublication(), is(notNullValue()));
        assertThat(response.getNewPublication(), is(nullValue()));
    }

    @Test
    public void handleRequestReturnsPublicationUpdateEventWhenEventContainsNewAndOldImage() {
        PublicationFanoutHandler handler = new PublicationFanoutHandler();

        InputStream inputStream = IoUtils.inputStreamFromResources(
                DYNAMODBEVENT_NEW_AND_OLD_IMAGES_JSON);

        handler.handleRequest(inputStream, outputStream, context);

        PublicationUpdateEvent response = parseResponse();

        assertThat(response.getOldPublication(), is(notNullValue()));
        assertThat(response.getNewPublication(), is(notNullValue()));
    }

    @Test
    public void handleRequestThrowsRuntimeExceptionWhenImageIsInvalid() {
        PublicationFanoutHandler handler = new PublicationFanoutHandler();

        InputStream inputStream = IoUtils.inputStreamFromResources(
            DYNAMODBEVENT_INVALID_IMAGE_JSON);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> handler.handleRequest(inputStream, outputStream, context));

        assertThat(exception.getMessage(), containsString(PublicationFanoutHandler.MAPPING_ERROR));
    }

    private PublicationUpdateEvent parseResponse() {
        return Try.attempt(() -> objectMapper.readValue(outputStream.toString(), PublicationUpdateEvent.class))
                .orElseThrow();
    }
}
