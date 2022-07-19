package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.ResourceDraftedForDeletionEvent;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DeletionProcessInitializationHandlerTest {
    
    public static final String NEW_PUBLICATION_DRAFT_FOR_DELETION_JSON =
        "delete/new_publication_draft_for_deletion.json";
    public static final String NEW_PUBLICATION_DRAFT_JSON = "delete/new_publication_draft.json";
    public static final String NEW_PUBLICATION_NULL_JSON = "delete/new_publication_null.json";
    private DeletionProcessInitializationHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    
    @BeforeEach
    public void setUp() {
        handler = new DeletionProcessInitializationHandler();
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }
    
    @Test
    public void handleRequestReturnsDeletePublicationEventOnDraftForDeletion() throws JsonProcessingException {
        InputStream inputStream = IoUtils.inputStreamFromResources(NEW_PUBLICATION_DRAFT_FOR_DELETION_JSON);
        
        handler.handleRequest(inputStream, outputStream, context);
        
        ResourceDraftedForDeletionEvent response = objectMapper.readValue(outputStream.toString(),
            ResourceDraftedForDeletionEvent.class);
        assertThat(response, notNullValue());
        assertThat(response.getDoi(), notNullValue());
        assertThat(response.getIdentifier(), notNullValue());
        assertThat(response.getStatus(), is(equalTo(PublicationStatus.DRAFT_FOR_DELETION.getValue())));
        assertThat(response.getTopic(), is(equalTo(ResourceDraftedForDeletionEvent.EVENT_TOPIC)));
    }
    
    @Test
    public void handleRequestReturnsNullOnDraft() throws JsonProcessingException {
        InputStream inputStream = IoUtils.inputStreamFromResources(NEW_PUBLICATION_DRAFT_JSON);
        
        handler.handleRequest(inputStream, outputStream, context);
        
        ResourceDraftedForDeletionEvent response =
            objectMapper.readValue(outputStream.toString(), ResourceDraftedForDeletionEvent.class);
        assertThat(response, nullValue());
    }
    
    @Test
    public void handleRequestReturnsNullOnMissingNewPublication() throws JsonProcessingException {
        InputStream inputStream = IoUtils.inputStreamFromResources(NEW_PUBLICATION_NULL_JSON);
        
        handler.handleRequest(inputStream, outputStream, context);
        
        ResourceDraftedForDeletionEvent response =
            objectMapper.readValue(outputStream.toString(), ResourceDraftedForDeletionEvent.class);
        assertThat(response, nullValue());
    }
}
