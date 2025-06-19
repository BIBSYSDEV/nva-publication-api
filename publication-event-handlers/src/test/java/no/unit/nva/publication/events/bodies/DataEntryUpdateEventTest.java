package no.unit.nva.publication.events.bodies;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomHiddenFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;
import no.unit.nva.publication.model.business.publicationchannel.ChannelType;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.Constraint;
import no.unit.nva.publication.model.business.publicationchannel.NonClaimedPublicationChannel;
import nva.commons.core.Environment;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DataEntryUpdateEventTest {
    
    public static Stream<Class<?>> dataEntryTypeProvider() {
        var types = fetchDirectSubtypes(Entity.class);
        var nestedTypes = new Stack<Type>();
        var result = new ArrayList<Class<?>>();
        nestedTypes.addAll(types);
        while (!nestedTypes.isEmpty()) {
            var currentType = nestedTypes.pop();
            if (isTypeWithSubtypes(currentType)) {
                var subTypes = fetchDirectSubtypes(currentType.value());
                nestedTypes.addAll(subTypes);
            } else {
                result.add(currentType.value());
            }
        }
        return result.stream();
    }
    
    @ParameterizedTest(name = "should provide event topic for data entry instance type: {0}")
    @MethodSource("dataEntryTypeProvider")
    void shouldProduceEventTopicForAllDataEntryTypes(Class<?> type)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var dataEntry = createDataEntry(type);
        var updateEvent = new DataEntryUpdateEvent(randomString(), dataEntry, dataEntry);
        assertThat(updateEvent.getTopic(), is(not(nullValue())));
    }

    @Test
    public void shouldNotProduceEventTopicForEntityWithUnchangedModifiedDateAndWhenIgnoringBatchScan() {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication);
        var dataEntryUpdateEvent = new DataEntryUpdateEvent(randomString(), resource, resource);
        var environment = mock(Environment.class);
        when(environment.readEnvOpt("SHOULD_IGNORE_BATCH_SCAN")).thenReturn(Optional.of("true"));

        assertFalse(dataEntryUpdateEvent.shouldProcessUpdate(environment));
    }
    
    private static boolean isTypeWithSubtypes(Type type) {
        return type.value().getAnnotationsByType(JsonSubTypes.class).length > 0;
    }
    
    private static List<Type> fetchDirectSubtypes(Class<?> type) {
        var annotations = type.getAnnotationsByType(JsonSubTypes.class);
        return Arrays.asList(annotations[0].value());
    }

    private Entity createDataEntry(Class<?> type)
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (type.getSimpleName().equals(FileEntry.class.getSimpleName())) {
            return randomFileEntry();
        } else if (type.getSimpleName().equals(ClaimedPublicationChannel.class.getSimpleName())) {
            return randomClaimedPublicationChannel();
        } else if (type.getSimpleName().equals(NonClaimedPublicationChannel.class.getSimpleName())) {
            return randomNonClaimedPublicationChannel();
        } else {
            return (Entity) type.getDeclaredConstructor().newInstance();
        }
    }

    private FileEntry randomFileEntry() {
        return FileEntry.create(randomHiddenFile(), SortableIdentifier.next(),
                                UserInstance.fromPublication(randomPublication()));
    }

    private ClaimedPublicationChannel randomClaimedPublicationChannel() {
        var randomConstraint = new Constraint(ChannelPolicy.EVERYONE, ChannelPolicy.OWNER_ONLY, List.of());
        return new ClaimedPublicationChannel(randomUri(), randomUri(), randomUri(), randomConstraint,
                                             ChannelType.PUBLISHER, SortableIdentifier.next(),
                                             SortableIdentifier.next(), Instant.now(), Instant.now());
    }

    private NonClaimedPublicationChannel randomNonClaimedPublicationChannel() {
        return new NonClaimedPublicationChannel(randomUri(), ChannelType.PUBLISHER, SortableIdentifier.next(),
                                                SortableIdentifier.next(), Instant.now(), Instant.now());
    }
}