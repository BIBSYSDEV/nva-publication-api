package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.bodies.DeleteResourceEvent;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DeletePublicaitonHandlerTest extends ResourcesLocalTest {

    private ByteArrayOutputStream outputStream;
    private Context context;
    private ResourceService resourceService;

    private DeletePublicationHandler handler;

    @BeforeEach
    public void setUp() {
        super.init();
        resourceService = new ResourceService(client, Clock.systemDefaultZone());
        handler = new DeletePublicationHandler(resourceService);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    void shouldReturnNullForNotDeletedPublication() throws JsonProcessingException {
        var oldImage = randomPublication().copy()
                           .withDoi(null)
                           .withStatus(PublicationStatus.PUBLISHED).build();
        var newImage = oldImage.copy()
                           .withStatus(PublicationStatus.PUBLISHED)
                           .build();
        var eventBody = new DataEntryUpdateEvent(randomString(),
                                                 Resource.fromPublication(oldImage),
                                                 Resource.fromPublication(newImage));
        var event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventBody);
        handler.handleRequest(event, outputStream, context);

        var response =
            objectMapper.readValue(outputStream.toString(), DeleteResourceEvent.class);
        assertThat(response, nullValue());
    }

    @Test
    void shouldReturnDeleteResourceEventWhenAPublicationWithStatusDeletedIsUpdated() throws JsonProcessingException {
        var oldImage = randomPublication().copy()
                           .withDoi(null)
                           .withStatus(PublicationStatus.PUBLISHED).build();
        var newImage = oldImage.copy()
                           .withStatus(PublicationStatus.DELETED)
                           .build();
        var eventBody = new DataEntryUpdateEvent(randomString(),
                                                 Resource.fromPublication(oldImage),
                                                 Resource.fromPublication(newImage));

        var event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventBody);
        handler.handleRequest(event, outputStream, context);
        var response =
            objectMapper.readValue(outputStream.toString(), DeleteResourceEvent.class);
        assertThat(response.getIdentifier(), notNullValue());
        assertThat(response.getStatus(), is(equalTo(PublicationStatus.DELETED.getValue())));
        assertThat(response.getTopic(), is(equalTo(DeleteResourceEvent.EVENT_TOPIC)));
    }

}
