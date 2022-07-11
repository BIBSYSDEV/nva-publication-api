package no.unit.nva.publication.publishingrequest;

import static nva.commons.core.attempt.Try.attempt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Consumer;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class PublishingRequestTestUtils {

    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");

    public static Clock setupMockClock() {
        var mockClock = mock(Clock.class);
        when(mockClock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(PUBLICATION_UPDATE_TIME);
        return mockClock;
    }

    public static Publication createAndPersistDraftPublication(ResourceService resourceService)
        throws ApiGatewayException {
        return createAndPersistPublicationAndThenActOnIt(resourceService, publication -> {
        });
    }

    public static Publication createPersistAndPublishPublication(ResourceService resourceService)
        throws ApiGatewayException {
        return createAndPersistPublicationAndThenActOnIt(resourceService,
                                                         publication -> publish(resourceService, publication));
    }

    public static Publication createAndPersistPublicationAndMarkForDeletion(ResourceService resourceService)
        throws ApiGatewayException {
        return createAndPersistPublicationAndThenActOnIt(resourceService,
                                                         publication -> markForDeletion(resourceService, publication));
    }

    private static void publish(ResourceService resourceService, Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        attempt(() -> resourceService.publishPublication(userInstance, publication.getIdentifier()))
            .orElseThrow();
    }

    private static void markForDeletion(ResourceService resourceService, Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        attempt(() -> resourceService.markPublicationForDeletion(userInstance, publication.getIdentifier()))
            .orElseThrow();
    }

    private static Publication createAndPersistPublicationAndThenActOnIt(ResourceService resourceService,
                                                                         Consumer<Publication> action)
        throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var storedResult = resourceService.createPublication(userInstance, publication);
        action.accept(storedResult);
        return resourceService.getPublication(storedResult);
    }
}
