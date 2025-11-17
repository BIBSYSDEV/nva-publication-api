package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.testing.EntityDescriptionBuilder.randomReference;
import static no.unit.nva.model.testing.PublicationGenerator.buildRandomPublicationFromInstance;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.ResourceRelationship;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.ResourceRelationshipDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ResourceRelationPersistenceTest extends ResourcesLocalTest {

    public static final String PUBLICATION_PATH = "publication";
    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        resourceService = getResourceService(client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.com", "https://example.com/publication/123",
        "https://example.com/something/0198cc8f7d15-3bfde61e-71c3-4253-8662-714a460886f1"})
    void shouldNotCreateRelationFromAnthologyWithIdWhichIsNotPublicationId(String value) {
        var anthology = persist(randomPublication(BookAnthology.class));
        persist(randomChapterWithAnthology(URI.create(value)));

        var anthologyWithoutRelation = Resource.fromPublication(anthology).fetch(resourceService).orElseThrow();

        assertTrue(anthologyWithoutRelation.getRelatedResources().isEmpty());
    }

    @Test
    void shouldListMultipleRelatedResources() {
        var anthology = persist(randomPublication(BookAnthology.class));
        var chapters = persistChaptersWithAnthology(anthology, 5);

        var anthologyWithRelations = Resource.fromPublication(anthology).fetch(resourceService).orElseThrow();

        var expectedRelations = chapters.stream().map(Publication::getIdentifier).toList();

        assertThat(expectedRelations,
                   containsInAnyOrder(anthologyWithRelations.getRelatedResources().toArray(SortableIdentifier[]::new)));
    }

    @Test
    void shouldPersistResourceRelationshipWhenPersistingAnthologyWithParentResource() {
        var anthology = persist(randomPublication(BookAnthology.class));
        var chapter = persistChaptersWithAnthology(anthology, 1).getFirst();

        var anthologyWithNewRelation = Resource.fromPublication(anthology).fetch(resourceService).orElseThrow();

        assertEquals(chapter.getIdentifier(), anthologyWithNewRelation.getRelatedResources().getFirst());
    }

    @Test
    void shouldRemoveResourceRelationshipWhenUpdatingResourceToTypeWithoutAnthology() {
        var anthology = persist(randomPublication(BookAnthology.class));
        var chapter = persistChaptersWithAnthology(anthology, 1).getFirst();

        updateToNonChapter(chapter);

        var anthologyWithNewRelation = Resource.fromPublication(anthology).fetch(resourceService).orElseThrow();

        assertTrue(anthologyWithNewRelation.getRelatedResources().isEmpty());
    }

    @Test
    void shouldRemoveOldRelationWhenUpdatingAnthologyId() {
        var anthology = persist(randomPublication(BookAnthology.class));
        var chapter = persistChaptersWithAnthology(anthology, 1).getFirst();
        var newAnthology = persist(randomPublication(BookAnthology.class));

        moveChapterToAnthology(chapter, newAnthology.getIdentifier());

        var anthologyWithRemovedRelation = Resource.fromPublication(anthology).fetch(resourceService).orElseThrow();

        assertTrue(anthologyWithRemovedRelation.getRelatedResources().isEmpty());
    }

    @Test
    void shouldAddNewRelationWhenUpdatingAnthologyId() {
        var anthology = persist(randomPublication(BookAnthology.class));
        var chapter = persistChaptersWithAnthology(anthology, 1).getFirst();
        var newAnthology = persist(randomPublication(BookAnthology.class));

        moveChapterToAnthology(chapter, newAnthology.getIdentifier());

        var anthologyWithNewRelation = Resource.fromPublication(newAnthology).fetch(resourceService).orElseThrow();

        assertEquals(chapter.getIdentifier(), anthologyWithNewRelation.getRelatedResources().getFirst());
    }

    @Test
    void databaseShouldHandleMultiplePublicationsSharingTheSameRelatedResource() {
        var anthology1 = persist(randomPublication(BookAnthology.class));
        var anthology2 = persist(randomPublication(BookAnthology.class));
        var childIdentifier = SortableIdentifier.next();

        insertRelationBetween(anthology1.getIdentifier(), childIdentifier);
        insertRelationBetween(anthology2.getIdentifier(), childIdentifier);

        assertDoesNotThrow(() -> Resource.fromPublication(anthology2).fetch(resourceService).orElseThrow());
        assertDoesNotThrow(() -> Resource.fromPublication(anthology1).fetch(resourceService).orElseThrow());
    }

    @Test
    void shouldPersistResourceRelationshipForDatabaseEntryWhenMigratingResourceWithAnthology() {
        var anthology = persist(randomPublication(BookAnthology.class));
        var chapter = super.persistResource(Resource.fromPublication(randomChapterWithAnthology(toPublicationId(anthology.getIdentifier()))));

        resourceService.refreshResourcesByKeys(List.of(chapter.toDao().primaryKey()));

        var persistedRelation = Resource.fromPublication(anthology).fetch(resourceService).orElseThrow();

        assertEquals(chapter.getIdentifier(), persistedRelation.getRelatedResources().getFirst());
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.com", "https://example.com/publication/123",
        "https://example.com/something/0198cc8f7d15-3bfde61e-71c3-4253-8662-714a460886f1"})
    void shouldNotPersistResourceRelationshipWhenMigratingResourceWithAnthologyWhenAnthologyIdIsNotPublicationId(String value) {
        var anthology = persist(randomPublication(BookAnthology.class));
        var chapter = super.persistResource(Resource.fromPublication(randomChapterWithAnthology(URI.create(value))));

        resourceService.refreshResourcesByKeys(List.of(chapter.toDao().primaryKey()));

        var persistedRelation = Resource.fromPublication(anthology).fetch(resourceService).orElseThrow();

        assertTrue(persistedRelation.getRelatedResources().isEmpty());
    }

    private void insertRelationBetween(SortableIdentifier parentIdentifier, SortableIdentifier childIdentifier) {
        var relationship = new ResourceRelationship(parentIdentifier, childIdentifier);
        client.putItem(new PutItemRequest(RESOURCES_TABLE_NAME, ResourceRelationshipDao.from(relationship).toDynamoFormat()));
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

    private List<Publication> persistChaptersWithAnthology(Publication anthology, int numberOfChapters) {
        return IntStream.of(numberOfChapters)
                   .mapToObj(i -> randomChapterWithAnthology(toPublicationId(anthology.getIdentifier())))
                   .map(this::persist)
                   .toList();
    }

    private void updateToNonChapter(Publication publication) {
        publication.getEntityDescription().setReference(randomReference(JournalArticle.class));
        Resource.fromPublication(publication).update(resourceService, UserInstance.fromPublication(publication));
    }

    private void moveChapterToAnthology(Publication publication, SortableIdentifier identifier) {
        var anthologyId = UriWrapper.fromUri(randomUri())
                              .addChild(PUBLICATION_PATH)
                              .addChild(identifier.toString())
                              .getUri();
        publication.getEntityDescription()
            .getReference()
            .setPublicationContext(new Anthology.Builder().withId(anthologyId).build());
        Resource.fromPublication(publication).update(resourceService, UserInstance.fromPublication(publication));
    }

    private Publication persist(Publication publication) {
        return attempt(() -> Resource.fromPublication(publication)
                                 .persistNew(resourceService, UserInstance.fromPublication(publication))).orElseThrow();
    }
}

