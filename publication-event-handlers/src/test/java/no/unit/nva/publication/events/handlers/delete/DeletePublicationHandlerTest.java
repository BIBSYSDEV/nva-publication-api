package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Clock;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.events.DeleteEntryEvent;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeletePublicationHandlerTest extends ResourcesLocalTest {

    public static final Context context = mock(Context.class);
    private DeletePublicationHandler handler;

    private ResourceService resourceService;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        super.init();
        outputStream = new ByteArrayOutputStream();
        resourceService = new ResourceService(client, Clock.systemDefaultZone());
        handler = new DeletePublicationHandler(resourceService);
    }

    @Test
    void shouldDeleteImportedPublicationWhenS3UriIsSupplied() throws ApiGatewayException {
        var publication = createPublishedResource();
        var expectedPublication =
            publication.copy().withStatus(PublicationStatus.DELETED).withPublishedDate(null).build();
        handler.handleRequest(createDeleteEntryEventInputStream(publication), outputStream, context);
        var actualPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThatActualPublicationIsEqualToExpectedPublicationIgnoringModifiedDate(actualPublication,
                                                                                    expectedPublication);
    }

    private void assertThatActualPublicationIsEqualToExpectedPublicationIgnoringModifiedDate(
        Publication actualPublication, Publication expectedPublication) {
        actualPublication.setModifiedDate(null);
        expectedPublication.setModifiedDate(null);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    private InputStream createDeleteEntryEventInputStream(Publication publication) {
        var deleteEntryEvent = new DeleteEntryEvent(DeleteEntryEvent.EVENT_TOPIC, publication.getIdentifier());
        return EventBridgeEventBuilder.sampleEvent(deleteEntryEvent);
    }

    private Publication createPublishedResource() throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithoutDoi();
        publishResource(resource);
        return resourceService.getPublication(resource);
    }

    private Publication publishResource(Publication resource) throws ApiGatewayException {
        resourceService.publishPublication(UserInstance.fromPublication(resource), resource.getIdentifier());
        return resourceService.getPublication(resource);
    }

    private Publication createPersistedPublicationWithoutDoi() {
        var publication = randomPublication().copy().withDoi(null).build();
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }
}
