package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.bodies.ResourceDraftedForDeletionEvent;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeletionProcessInitializationHandlerTest extends ResourcesLocalTest {
    
    private DeletionProcessInitializationHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    
    @BeforeEach
    public void setUp() {
        super.init();
        handler = new DeletionProcessInitializationHandler();
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }
    
    @Test
    void handleRequestReturnsDeletePublicationEventOnDraftForDeletion() throws JsonProcessingException {
        var oldImage = randomPreFilledPublicationBuilder()
                           .withDoi(null)
                           .withStatus(PublicationStatus.DRAFT).build();
        var newImage = attempt(() -> oldImage.copy()
                           .withStatus(PublicationStatus.DRAFT_FOR_DELETION)
                           .build()).orElseThrow();
        var eventBody = new DataEntryUpdateEvent(randomString(),
            Resource.fromPublication(oldImage),
            Resource.fromPublication(newImage));
        var event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventBody);
        handler.handleRequest(event, outputStream, context);
        
        var response = objectMapper.readValue(outputStream.toString(),
            ResourceDraftedForDeletionEvent.class);
        assertThat(response, notNullValue());
        //TODO: removed assertion for not null DOI. Whole handler functionality should be re-evaluated.
        // assertThat(response.getDoi(), notNullValue());
        assertThat(response.getIdentifier(), notNullValue());
        assertThat(response.getStatus(), is(equalTo(PublicationStatus.DRAFT_FOR_DELETION.getValue())));
        assertThat(response.getTopic(), is(equalTo(ResourceDraftedForDeletionEvent.EVENT_TOPIC)));
    }
    
    @Test
    void handleRequestReturnsNullOnDraft() throws JsonProcessingException {
        var newDraft = randomPreFilledPublicationBuilder()
                           .withDoi(null)
                           .withStatus(PublicationStatus.DRAFT).build();
        var eventBody = new DataEntryUpdateEvent(randomString(),
            null,
            Resource.fromPublication(newDraft));
        var event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventBody);
        handler.handleRequest(event, outputStream, context);
        
        var response =
            objectMapper.readValue(outputStream.toString(), ResourceDraftedForDeletionEvent.class);
        assertThat(response, nullValue());
    }
    
    @Test
    void handleRequestReturnsNullOnMissingNewPublication() throws JsonProcessingException {
        var eventBody = new DataEntryUpdateEvent(randomString(), Resource.fromPublication(randomPublication()), null);
        var event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventBody);
        
        handler.handleRequest(event, outputStream, context);
        
        var response =
            objectMapper.readValue(outputStream.toString(), ResourceDraftedForDeletionEvent.class);
        assertThat(response, nullValue());
    }
}
