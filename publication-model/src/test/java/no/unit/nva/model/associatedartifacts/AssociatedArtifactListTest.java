package no.unit.nva.model.associatedartifacts;

import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssociatedArtifactListTest {

    @Test
    void shouldRemoveNullAssociatedArtifactFromListWhenListContainsNotOnlyNullAssociatedArtifact() {
        var nullAssociatedArtifact = new NullAssociatedArtifact();
        var file = randomOpenFile();

        var associatedArtifactList = new AssociatedArtifactList(List.of(file, nullAssociatedArtifact));

        assertFalse(associatedArtifactList.contains(nullAssociatedArtifact));
        assertTrue(associatedArtifactList.contains(file));
    }
}