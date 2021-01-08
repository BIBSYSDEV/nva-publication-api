package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.PublicationGenerator.publicationWithoutIdentifier;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.Resource;
import nva.commons.exceptions.commonexceptions.ConflictException;
import nva.commons.exceptions.commonexceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class ResourceServiceTest extends ResourcesDynamoDbLocalTest {

    public static final String ANOTHER_OWNER = "another@owner.no";
    private static final String SOME_USER = "some@user.com";
    private static final URI SOME_ORG = URI.create("https://example.org/123-456");

    public static final UserInstance SAMPLE_USER = new UserInstance(SOME_USER, SOME_ORG);
    private static final URI SOME_OTHER_ORG = URI.create("https://example.org/789-ABC");
    public static final String SOME_OTHER_USER = "some_other@user.no";

    private  static final Instant RESOURCE_CREATION_TIME= Instant.parse("1900-12-03T10:15:30.00Z");
    private  static final Instant RESOURCE_UPDATE_TIME= Instant.parse("2000-01-03T00:00:18.00Z");
    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        Clock clock =mock(Clock.class);
        when(clock.instant()).thenReturn(RESOURCE_CREATION_TIME).thenReturn(RESOURCE_UPDATE_TIME);
        resourceService = new ResourceService(client,clock);
    }

    @Test
    public void createResourceCreatesResource() throws NotFoundException, ConflictException {
        Resource resource = Resource.fromPublication(publicationWithoutIdentifier());
        resourceService.createResource(resource);
        Resource savedResource = resourceService.getResource(resource);
        assertThat(savedResource, is(equalTo(resource)));
        assertThat(savedResource, is(not(sameInstance(resource))));
    }

    @Test
    public void createResourceThrowsConflictExceptionWhenResourceWithSameIdentifierExists() throws ConflictException {
        final Resource sampleResource = emptyResource();
        final Resource collidingResource = sampleResource.copy()
            .withPublisher(anotherPublisher())
            .withOwner(ANOTHER_OWNER)
            .build();

        resourceService.createResource(sampleResource);
        Executable action = () -> resourceService.createResource(collidingResource);
        assertThrows(ConflictException.class, action);

        assertThat(sampleResource.getIdentifier(), is(equalTo(collidingResource.getIdentifier())));
        assertThat(sampleResource.getOwner(), is(not(equalTo(collidingResource.getOwner()))));
        assertThat(sampleResource.getPublisher().getId(), is(not(equalTo(collidingResource.getPublisher().getId()))));
    }

    @Test
    public void createResourceSavesResourcesWithSameOwnerAndPublisherButDifferentIdentifier()
        throws ConflictException {
        final Resource sampleResource = emptyResource();
        final Resource anotherResource = emptyResource();

        resourceService.createResource(sampleResource);
        assertDoesNotThrow(()->resourceService.createResource(anotherResource));
    }

    @Test
    public void getResourceByIdentifierReturnsNotFoundWhenResourceDoesNotExist() {
        String nonExistingIdentifier = SortableIdentifier.next().toString();
        Executable action = () -> resourceService.getResource(SAMPLE_USER, nonExistingIdentifier);
        assertThrows(NotFoundException.class, action);
    }

    @Test
    public void getResourceByIdentifierReturnsResourceWhenResourceExists() throws NotFoundException, ConflictException {
        Resource sampleResource = emptyResource();
        resourceService.createResource(sampleResource);
        Resource savedResource = resourceService.getResource(SAMPLE_USER, sampleResource.getIdentifier());
        assertThat(savedResource, is(equalTo(sampleResource)));
    }

    @Test
    public void whenPublicationOwnerIsUpdatedTheResourceEntryBecomesUnavailableForPreviousOwner()
        throws ConflictException, NotFoundException {
        Resource sampleResource = emptyResource();
        UserInstance oldOwner = new UserInstance(sampleResource.getOwner(),sampleResource.getPublisher().getId());
        UserInstance newOwner = someOtherUser();

        resourceService.createResource(sampleResource);
        resourceService.updateOwner(sampleResource.getIdentifier().toString(),oldOwner,newOwner);

        assertThatResourceDoesNotExist(sampleResource);

        Resource newResource= resourceService.getResource(newOwner,sampleResource.getIdentifier());
        Resource expectedResource = sampleResource.copy()
            .withOwner(someOtherUser().getUserId())
            .withPublisher(userOrganization(someOtherUser()))
            .withModifiedDate(RESOURCE_UPDATE_TIME)
            .build();


        assertThat(newResource, is(equalTo(expectedResource)));
        assertThat(newResource.getOwner(), is(equalTo(newOwner.getUserId())));
        assertThat(newResource.getPublisher().getId(), is(equalTo(newOwner.getOrganizationUri())));

    }

    private void assertThatResourceDoesNotExist(Resource sampleResource) {
        assertThrows(NotFoundException.class,()->resourceService.getResource(sampleResource));
    }

    private UserInstance someOtherUser() {
        return new UserInstance(SOME_OTHER_USER,SOME_OTHER_ORG);
    }

    private Organization anotherPublisher() {
        return new Organization.Builder().withId(SOME_OTHER_ORG).build();
    }

    private Resource emptyResource() {
        Resource resource = Resource.emptyResource(SAMPLE_USER.getUserId(), SAMPLE_USER.getOrganizationUri());
        resource.setStatus(PublicationStatus.DRAFT);
        return resource;
    }
}