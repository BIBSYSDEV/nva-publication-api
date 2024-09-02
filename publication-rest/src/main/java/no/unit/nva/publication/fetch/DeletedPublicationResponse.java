package no.unit.nva.publication.fetch;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import nva.commons.core.JacocoGenerated;

@JsonTypeName("Publication")
public final class DeletedPublicationResponse {

    @JacocoGenerated
    private DeletedPublicationResponse() {
    }

    public static Object fromPublication(Publication publication, Set<PublicationOperation> allowedOperations) {
        var publicationWithoutAssociatedArtifacts = publication.copy().withAssociatedArtifacts(List.of()).build();
        var tombstone = PublicationResponseElevatedUser.fromPublicationWithAllowedOperations(
            publicationWithoutAssociatedArtifacts, allowedOperations);
        return attempt(
            () -> JsonUtils.dtoObjectMapper.convertValue(tombstone, new TypeReference<Map<String, Object>>() {
            })).orElseThrow();
    }
}
