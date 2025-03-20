package no.unit.nva.model.associatedartifacts;

import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssociatedArtifactListTest {

    @Test
    void shouldRemoveNullAssociatedArtifactsFromListWhenListContainsNotOnlyNullAssociatedArtifact() {
        var nullAssociatedArtifact1 = new NullAssociatedArtifact();
        var nullAssociatedArtifact2 = new NullAssociatedArtifact();
        var file = randomOpenFile();

        var associatedArtifactList = new AssociatedArtifactList(List.of(file, nullAssociatedArtifact1, nullAssociatedArtifact2));

        assertFalse(associatedArtifactList.contains(nullAssociatedArtifact1));
        assertFalse(associatedArtifactList.contains(nullAssociatedArtifact2));
        assertTrue(associatedArtifactList.contains(file));
    }

    @Test
    void shouldKeepNullAssociatedArtifactWhenOnlyElementInList() {
        var nullAssociatedArtifact = new NullAssociatedArtifact();

        var associatedArtifactList = new AssociatedArtifactList(List.of(nullAssociatedArtifact));

        assertTrue(associatedArtifactList.contains(nullAssociatedArtifact));
    }

    @Test
    void shouldNotFailWhenAssociatedArtifactsAreNull() {
        var associatedArtifactList = new AssociatedArtifactList((List<AssociatedArtifact>) null);

        assertTrue(associatedArtifactList.isEmpty());
    }

    @Test
    void shouldKeepOnlyOneNullAssociatedArtifactWhenMultiple() {
        var nullAssociatedArtifact = new NullAssociatedArtifact();

        var associatedArtifactList = new AssociatedArtifactList(List.of(nullAssociatedArtifact, nullAssociatedArtifact));

        assertTrue(associatedArtifactList.contains(nullAssociatedArtifact));
        assertEquals(1, associatedArtifactList.size());
    }
}