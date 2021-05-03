package no.unit.nva.cristin.mapper;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import no.unit.nva.cristin.CristinDataGenerator;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Test;

public class CristinObjectTest {

    public static final Javers JAVERS = JaversBuilder.javers().build();

    @Test
    public void copyReturnsAnObjectThatIsEqualButNotTheSame() {
        CristinObject sampleObject = new CristinDataGenerator().randomBookAnthology();
        assertThat(sampleObject, doesNotHaveEmptyValues());
        CristinObject copy = sampleObject.copy().build();
        Diff diff = JAVERS.compare(sampleObject, copy);
        assertThat(diff.hasChanges(), is(false));
        assertThat(copy, is(equalTo(sampleObject)));
        assertThat(copy, is(not(sameInstance(sampleObject))));
    }
}