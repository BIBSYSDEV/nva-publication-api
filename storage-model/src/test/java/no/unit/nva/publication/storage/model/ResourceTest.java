package no.unit.nva.publication.storage.model;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.File;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

public class ResourceTest {

    public static final String SOME_TITLE = "SomeTitle";
    public static final URI SAMPLE_ORG_URI = URI.create("https://www.example.com/123");
    public static final Organization SAMPLE_ORG = new Organization.Builder().withId(SAMPLE_ORG_URI).build();
    public static final String SOME_OWNER = "some@owner.no";
    public static final String SOME_LINK = "https://example.org/somelink";
    private static final Instant RESOURCE_CREATION_TIME = Instant.parse("1900-12-03T10:15:30.00Z");
    private static final Instant RESOURCE_MODIFICATION_TIME = Instant.parse("2000-01-03T00:00:18.00Z");
    private static final Instant RESOURCE_SECOND_MODIFICATION_TIME = Instant.parse("2010-01-03T02:00:25.00Z");

    @Test
    public void builderContainsAllFields() {
        Resource resource = sampleResource();
        assertThat(resource, doesNotHaveNullOrEmptyFields());
    }

    @Test
    public void copyContainsAllFields() {
        Resource resource = sampleResource();
        Resource copy = resource.copy().build();
        JsonNode resourceJson = JsonUtils.objectMapper.convertValue(resource, JsonNode.class);
        JsonNode copyJson = JsonUtils.objectMapper.convertValue(copy, JsonNode.class);
        assertThat(resource, doesNotHaveNullOrEmptyFields());
        assertThat(copy, is(equalTo(resource)));
        assertThat(resourceJson, is(equalTo(copyJson)));
    }

    private Resource sampleResource() {

        FileSet files = new FileSet();
        File file = new File.Builder().withIdentifier(UUID.randomUUID()).build();
        files.setFiles(List.of(file));
        return Resource.builder()
            .withIdentifier(SortableIdentifier.next())
            .withTitle(SOME_TITLE)
            .withStatus(PublicationStatus.DRAFT)
            .withOwner(SOME_OWNER)
            .withCreatedDate(RESOURCE_CREATION_TIME)
            .withModifiedDate(RESOURCE_SECOND_MODIFICATION_TIME)
            .withPublishedDate(RESOURCE_MODIFICATION_TIME)
            .withPublisher(SAMPLE_ORG)
            .withFiles(files)
            .withLink(URI.create(SOME_LINK))
            .build();
    }
}