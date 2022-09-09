package no.unit.nva.publication.model.business;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import no.unit.nva.model.testing.PublicationGenerator;
import org.junit.jupiter.api.Test;

class EntityTest {
    
    @Test
    void shouldCreateNewRowVersionWhenRefreshed() {
        var publication = PublicationGenerator.randomPublication();
        var resource = Resource.fromPublication(publication);
        var oldRowVersion = resource.getVersion();
        var newRowVersion = resource.refreshVersion().getVersion();
        assertThat(newRowVersion, is(not(equalTo(oldRowVersion))));
    }
}
