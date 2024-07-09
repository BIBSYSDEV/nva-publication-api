package no.unit.nva;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Deprecated
public class MigratePublicationStatusTest {

    @Disabled("Disabled since publication status DELETED has been migrated")
    @Test
    void shouldMigrateDeletedStatusToUnpublished() throws JsonProcessingException {
        var json = "{\n"
                   + "  \"type\": \"Publication\",\n"
                   + "  \"status\": \"DELETED\"\n"
                   + "}";
        var publication = JsonUtils.dtoObjectMapper.readValue(json, Publication.class);

        assertThat(publication.getStatus(), is(equalTo(PublicationStatus.UNPUBLISHED)));
    }

    @Test
    void shouldDoNothingWithExistingUnpublishedStatus() throws JsonProcessingException {
        var json = "{\n"
                   + "  \"type\": \"Publication\",\n"
                   + "  \"status\": \"UNPUBLISHED\"\n"
                   + "}";
        var publication = JsonUtils.dtoObjectMapper.readValue(json, Publication.class);

        assertThat(publication.getStatus(), is(equalTo(PublicationStatus.UNPUBLISHED)));
    }
}
