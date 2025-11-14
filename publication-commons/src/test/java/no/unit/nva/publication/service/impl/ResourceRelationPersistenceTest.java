package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.testing.EntityDescriptionBuilder.randomReference;
import static no.unit.nva.model.testing.PublicationGenerator.buildRandomPublicationFromInstance;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResourceRelationPersistenceTest extends ResourcesLocalTest {

    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        resourceService = getResourceService(client);
    }

    @Test
    void shouldPersistResourceRelationshipWhenPersistingAnthologyWithParentResource() throws BadRequestException {
        var anthology = persist(randomPublication(BookAnthology.class));
        var chapter = persist(randomChapterWithAnthology(toPublicationId(anthology.getIdentifier())));

        var anthologyWithNewRelation = Resource.fromPublication(anthology).fetch(resourceService).orElseThrow();

        assertEquals(chapter.getIdentifier(), anthologyWithNewRelation.getRelatedResources().getFirst());
    }

    @Test
    void shouldRemoveResourceRelationshipWhenUpdatingResourceToTypeWithoutAnthology() throws BadRequestException {
        var anthology = persist(randomPublication(BookAnthology.class));
        var chapter = persist(randomChapterWithAnthology(toPublicationId(anthology.getIdentifier())));

        updateToNonChapter(chapter);

        var anthologyWithNewRelation = Resource.fromPublication(anthology).fetch(resourceService).orElseThrow();

        assertTrue(anthologyWithNewRelation.getRelatedResources().isEmpty());
    }

    @Test
    void shouldRemoveOldRelationWhenUpdatingAnthologyId() throws BadRequestException {
        var anthology = persist(randomPublication(BookAnthology.class));
        var chapter = persist(randomChapterWithAnthology(toPublicationId(anthology.getIdentifier())));
        var newAnthology = persist(randomPublication(BookAnthology.class));

        moveChapterToAnthology(chapter, newAnthology.getIdentifier());

        var anthologyWithRemovedRelation = Resource.fromPublication(anthology).fetch(resourceService).orElseThrow();

        assertTrue(anthologyWithRemovedRelation.getRelatedResources().isEmpty());
    }

    @Test
    void shouldAddNewRelationAndWhenUpdatingAnthologyId() throws BadRequestException {
        var anthology = persist(randomPublication(BookAnthology.class));
        var chapter = persist(randomChapterWithAnthology(toPublicationId(anthology.getIdentifier())));
        var newAnthology = persist(randomPublication(BookAnthology.class));

        moveChapterToAnthology(chapter, newAnthology.getIdentifier());

        var anthologyWithNewRelation = Resource.fromPublication(newAnthology).fetch(resourceService).orElseThrow();

        assertEquals(chapter.getIdentifier(), anthologyWithNewRelation.getRelatedResources().getFirst());
    }

    @Test
    void shouldNotCreateRelationFromAnthologyWithoutId() throws BadRequestException {
        var anthology = persist(randomPublication(BookAnthology.class));
        persist(randomChapterWithAnthology(randomUri()));

        var anthologyWithoutRelation = Resource.fromPublication(anthology).fetch(resourceService).orElseThrow();

        assertTrue(anthologyWithoutRelation.getRelatedResources().isEmpty());
    }

    private static Publication randomChapterWithAnthology(URI anthologyId) {
        var chapter = buildRandomPublicationFromInstance(AcademicChapter.class);
        chapter.getEntityDescription()
            .getReference()
            .setPublicationContext(new Anthology.Builder().withId(anthologyId).build());
        return chapter;
    }

    private static URI toPublicationId(SortableIdentifier anthology) {
        return UriWrapper.fromUri(randomUri()).addChild("publication").addChild(anthology.toString()).getUri();
    }

    private void updateToNonChapter(Publication publication) {
        publication.getEntityDescription().setReference(randomReference(JournalArticle.class));
        Resource.fromPublication(publication).update(resourceService, UserInstance.fromPublication(publication));
    }

    private void moveChapterToAnthology(Publication publication, SortableIdentifier identifier) {
        var anthologyId = UriWrapper.fromUri(randomUri())
                              .addChild("publication")
                              .addChild(identifier.toString())
                              .getUri();
        publication.getEntityDescription()
            .getReference()
            .setPublicationContext(new Anthology.Builder().withId(anthologyId).build());
        Resource.fromPublication(publication).update(resourceService, UserInstance.fromPublication(publication));
    }

    private Publication persist(Publication publication) throws BadRequestException {
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }
}

