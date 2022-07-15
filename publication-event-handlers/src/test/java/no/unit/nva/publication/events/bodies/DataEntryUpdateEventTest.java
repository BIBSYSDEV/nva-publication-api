package no.unit.nva.publication.events.bodies;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Stream;
import no.unit.nva.publication.storage.model.DataEntry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DataEntryUpdateEventTest {
    
    public static Stream<Class<?>> dataEntryTypeProvider() {
        JsonSubTypes[] annotations = DataEntry.class.getAnnotationsByType(JsonSubTypes.class);
        Type[] types = annotations[0].value();
        return Arrays.stream(types).map(Type::value);
    }
    
    @ParameterizedTest(name = "should provide event topic for data entry instance type: {0}")
    @MethodSource("dataEntryTypeProvider")
    void shouldProduceEventTopicForAllDataEntryTypes(Class<?> type)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var dataEntry = createDataEntry(type);
        var updateEvent = new DataEntryUpdateEvent(randomString(), dataEntry, dataEntry);
        assertThat(updateEvent.getTopic(), is(not(nullValue())));
    }
    
    private DataEntry createDataEntry(Class<?> type)
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return (DataEntry) type.getDeclaredConstructor().newInstance();
    }
}