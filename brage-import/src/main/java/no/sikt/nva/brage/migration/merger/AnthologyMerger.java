package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.nonNull;
import java.net.URI;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.PublicationContext;

public class AnthologyMerger extends PublicationContextMerger {

    private AnthologyMerger() {
    }

    public static Anthology merge(Anthology anthology, PublicationContext publicationContext) {
        if (publicationContext instanceof Anthology newAnthology) {
            return new Anthology.Builder()
                       .withId(getId(anthology.getId(), newAnthology.getId()))
                       .build();
        } else {
            return anthology;
        }
    }

    private static URI getId(URI oldUri, URI newUri) {
        return nonNull(oldUri) ? oldUri : newUri;
    }
}
