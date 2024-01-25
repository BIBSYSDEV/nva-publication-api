package no.unit.nva.publication.fetch;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

public final class DeletedPublicationResponse {

    @JacocoGenerated
    private DeletedPublicationResponse() {

    }

    public static Object craftDeletedPublicationResponse(Publication publication) {
        var publicationWithoutAssociatedArtifacts = publication.copy()
                                                        .withAssociatedArtifacts(List.of())
                                                        .build();
        return attempt(() -> JsonUtils.dtoObjectMapper
                                 .convertValue(publicationWithoutAssociatedArtifacts,
                                               new TypeReference<Map<String, Object>>() {
                                               })).orElseThrow();
    }
}
