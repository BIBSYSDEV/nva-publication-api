package no.unit.nva.expansion.model;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.uriretriever.FakeUriRetriever;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpandedParentPublicationTest extends ResourcesLocalTest {

    public static final String PUBLICATION_PATH = "publication";
    private FakeUriRetriever fakeUriRetriever;
    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        super.init();
        this.fakeUriRetriever = FakeUriRetriever.newInstance();
        this.resourceService = getResourceServiceBuilder().build();
    }

    @Test
    void shouldThrowRuntimeExceptionWhenParentPublicationDoesNotExists() {
        var publicationId = randomPublicationId();
        var parent = new ExpandedParentPublication(fakeUriRetriever, resourceService);

        assertThrows(RuntimeException.class,
                     () -> parent.getExpandedParentPublication(publicationId),
                     "Parent publication not found " + publicationId);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenParentPublicationHasInvalidExternalReferences() throws BadRequestException {
        var publication = persistedParentPublication();
        var publicationId = toPublicationId(publication.getIdentifier());
        var parent = new ExpandedParentPublication(fakeUriRetriever, resourceService);

        assertThrows(RuntimeException.class, () -> parent.getExpandedParentPublication(publicationId));
    }

    private static URI randomPublicationId() {
        return UriWrapper.fromUri(randomUri())
                   .addChild(PUBLICATION_PATH)
                   .addChild(SortableIdentifier.next().toString())
                   .getUri();
    }

    private static URI toPublicationId(SortableIdentifier identifier) {
        return UriWrapper.fromUri(randomUri()).addChild(PUBLICATION_PATH).addChild(identifier.toString()).getUri();
    }

    private Publication persistedParentPublication() throws BadRequestException {
        var publication = randomPublication(BookAnthology.class);
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }
}