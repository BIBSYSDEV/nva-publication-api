package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.publication.model.business.Resource;
import org.junit.jupiter.api.Test;

public class EntityExpansionResolverRegistryTest {

    @Test
    void shouldThrowExceptionWhenEntityExpanderNotFound() {
        var publication = randomPublication(AcademicArticle.class);
        var registry = new EntityExpansionResolverRegistry();
        var entity = Resource.fromPublication(publication);

        assertThrows(NoEntityExpansionResolverException.class, () -> registry.resolveEntityToExpand(entity, entity));
    }
}
