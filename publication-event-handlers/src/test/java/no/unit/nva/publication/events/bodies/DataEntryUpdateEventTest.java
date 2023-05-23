package no.unit.nva.publication.events.bodies;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.model.business.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DataEntryUpdateEventTest {
    
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
    
    private static boolean isTypeWithSubtypes(Type type) {
        return type.value().getAnnotationsByType(JsonSubTypes.class).length > 0;
    }
    
    private static List<Type> fetchDirectSubtypes(Class<?> type) {
        var annotations = type.getAnnotationsByType(JsonSubTypes.class);
        return Arrays.asList(annotations[0].value());
    }

    private Entity createDataEntry(Class<?> type)
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return (Entity) type.getDeclaredConstructor().newInstance();
    }
}