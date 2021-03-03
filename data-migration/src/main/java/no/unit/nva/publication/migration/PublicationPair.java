package no.unit.nva.publication.migration;

import java.util.Objects;
import no.unit.nva.model.DoiRequest;
import no.unit.nva.model.Publication;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;

public class PublicationPair {

    private static final Javers JAVERS = JaversBuilder.javers().registerIgnoredClass(DoiRequest.class).build();
    private final Publication oldVersion;
    private final Publication newVersion;

    public PublicationPair(Publication oldVersion, Publication newVersion) {
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
    }

    public boolean versionsAreNotEqual() {
        return !Objects.equals(newVersion, oldVersion);
    }

    public void versionsAreEqualOrThrowException() {
        if (versionsAreNotEqual()) {
            assertThatVersionAreSemanticallyEquivalent();
        }
    }

    private boolean assertThatVersionAreSemanticallyEquivalent() {
        Diff diff = JAVERS.compare(oldVersion, newVersion);
        if (diff.hasChanges()) {
            throw new RuntimeException("Inserted object is different from old one: " + diff.prettyPrint());
        }
        return true;
    }
}
