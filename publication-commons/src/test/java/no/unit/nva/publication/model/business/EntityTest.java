package no.unit.nva.publication.model.business;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.model.business.Entity.nextVersion;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class EntityTest {
    
    static Stream<Tuple> resourceProvider() {
        var publication = PublicationGenerator.randomPublication();
        final var leftResource = Resource.fromPublication(publication);
        final var rightResource = leftResource.copy().withRowVersion(nextVersion()).build();
        
        final var leftDoiRequest = DoiRequest.newDoiRequestForResource(leftResource, Instant.now());
        final var rightDoiRequest = leftDoiRequest.copy();
        rightDoiRequest.setVersion(nextVersion());
        
        final var leftMessage = randomMessage(publication);
        final var rightMessage = leftMessage.copy();
        rightMessage.setVersion(nextVersion());
        
        final var leftPublishingRequestCase = randomPublishingRequest(publication);
        final var rightPublishingRequestCase = leftPublishingRequestCase.copy();
        rightPublishingRequestCase.setVersion(UUID.randomUUID());
        
        return Stream.of(new Tuple(leftResource, rightResource),
            new Tuple(leftDoiRequest, rightDoiRequest),
            new Tuple(leftMessage, rightMessage),
            new Tuple(leftPublishingRequestCase, rightPublishingRequestCase));
    }
    
    //This test guarantees backwards compatibility and the requirements can change when optimistic concurrency control
    // is implemented.
    @ParameterizedTest(name = "should return equals true when two resources differ only in their row version")
    @MethodSource("resourceProvider")
    void shouldReturnEqualsTrueWhenTwoResourcesDifferOnlyInTheirRowVersion(Tuple tuple) {
        assertThat(tuple.left, doesNotHaveEmptyValues());
        assertThat(tuple.right.getVersion(), is(not(equalTo(tuple.left.getVersion()))));
        assertThat(tuple.right, is(equalTo(tuple.left)));
    }
    
    //TODO: reconsider this test and whether we should inlcude all non-user controlled fields or not.
    //This test guarantees backwards compatibility and the requirements can change when optimistic concurrency control
    // is implemented.
    @ParameterizedTest(name = "should return the same hash code when two resources differ only in their row version")
    @MethodSource("resourceProvider")
    void shouldReturnTheSameHashCodeWhenTwoResourcesDifferOnlyInTheirRowVersion(Tuple tuple) {
        assertThat(tuple.left, doesNotHaveEmptyValues());
        assertThat(tuple.left.getVersion(), is(not(equalTo(tuple.right.getVersion()))));
        assertThat(tuple.left.hashCode(), is(equalTo(tuple.right.hashCode())));
    }
    
    @Test
    void shouldCreateNewRowVersionWhenRefreshed() {
        var publication = PublicationGenerator.randomPublication();
        var resource = Resource.fromPublication(publication);
        var oldRowVersion = resource.getVersion();
        var newRowVersion = resource.refreshVersion().getVersion();
        assertThat(newRowVersion, is(not(equalTo(oldRowVersion))));
    }
    
    private static PublishingRequestCase randomPublishingRequest(Publication publication) {
        var publishingRequest = new PublishingRequestCase();
        publishingRequest.setIdentifier(SortableIdentifier.next());
        publishingRequest.setCustomerId(publication.getPublisher().getId());
        publishingRequest.setResourceIdentifier(publication.getIdentifier());
        publishingRequest.setVersion(UUID.randomUUID());
        publishingRequest.setOwner(publication.getResourceOwner().getOwner());
        publishingRequest.setCreatedDate(randomInstant());
        publishingRequest.setModifiedDate(randomInstant());
        publishingRequest.setStatus(randomElement(TicketStatus.values()));
        return publishingRequest;
    }
    
    private static Message randomMessage(Publication publication) {
        var user = UserInstance.create(randomString(), randomUri());
        var clock = Clock.systemDefaultZone();
        return Message.create(user, publication, randomString(), SortableIdentifier.next(), clock, MessageType.SUPPORT);
    }
    
    private static class Tuple {
        
        private final Entity left;
        private final Entity right;
        
        public Tuple(Entity left, Entity right) {
            
            this.left = left;
            this.right = right;
        }
    }
}
