package no.unit.nva.publication.storage.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.StorageModelTestUtils.randomString;
import static no.unit.nva.publication.storage.model.ResourceUpdate.nextRowVersion;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.storage.model.daos.Dao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceUpdateTest {

    static Stream<Tuple> resourceProvider() {
        var publication = PublicationGenerator.randomPublication();
        final var leftResource = Resource.fromPublication(publication);
        final var rightResource = leftResource.copy().withRowVersion(nextRowVersion()).build();
        final var leftDoiRequest = DoiRequest.newDoiRequestForResource(leftResource, Instant.now());
        final var rightDoiRequest = leftDoiRequest.copy().withRowVersion(nextRowVersion()).build();
        final var leftMessage = randomMessage(publication);
        final var rightMessage = leftMessage.copy().withRowVersion(nextRowVersion()).build();
        return Stream.of(new Tuple(leftResource, rightResource),
                         new Tuple(leftDoiRequest, rightDoiRequest),
                         new Tuple(leftMessage, rightMessage));
    }

    //This test guarantees backwards compatibility and the requirements can change when optimistic concurrency control
    // is implemented.
    @ParameterizedTest(name = "should return equals true when two resources differ only in their row version")
    @MethodSource("resourceProvider")
    void shouldReturnEqualsTrueWhenTwoResourcesDifferOnlyInTheirRowVersion(Tuple tuple) {
        assertThat(tuple.left, doesNotHaveEmptyValues());
        assertThat(tuple.right.getRowVersion(), is(not(equalTo(tuple.left.getRowVersion()))));
        assertThat(tuple.right, is(equalTo(tuple.left)));
    }

    @Test
    void shouldCreateNewRowVersionWhenRefreshed() {
        var publication = PublicationGenerator.randomPublication();
        var resource = Resource.fromPublication(publication);
        var oldRowVersion = resource.getRowVersion();
        var newRowVersion = resource.refreshRowVersion().getRowVersion();
        assertThat(newRowVersion, is(not(equalTo(oldRowVersion))));
    }

    @ParameterizedTest(name= "should return Dao object:{0}")
    @MethodSource("resourceProvider")
    void shouldReturnDaoObject(Tuple resourceUpdate){
        assertThat(resourceUpdate.left.toDao(),is(instanceOf(Dao.class)));

    }

    private static Message randomMessage(Publication publication) {
        var user = new UserInstance(randomString(), randomUri());
        var clock = Clock.systemDefaultZone();
        return Message.supportMessage(user, publication, randomString(), SortableIdentifier.next(), clock);
    }

    private static class Tuple {

        private final ResourceUpdate left;
        private final ResourceUpdate right;

        public Tuple(ResourceUpdate left, ResourceUpdate right) {

            this.left = left;
            this.right = right;
        }
    }
}