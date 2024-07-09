package no.unit.nva.model;

import static no.unit.nva.DatamodelConfig.dataModelObjectMapper;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;

import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import no.unit.nva.model.config.ResourcesBuildConfig;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.util.ContextUtil;
import nva.commons.core.ioutils.IoUtils;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Test;

public class PublicationTest {

    public static final String PUBLICATION_CONTEXT_JSON = Publication.getJsonLdContext(URI.create("https://localhost"));
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final String DOI_REQUEST_FIELD = "doiRequest";
    public static final String FRAME = IoUtils.stringFromResources(Path.of("publicationFrame.json")).replace(
        "__REPLACE__", "https://localhost");
    public static final String BOOK_REVISION_FIELD = ".entityDescription.reference.publicationContext.revision";
    public static final String ALLOWED_OPERATIONS_FIELD = "allowedOperations";
    public static final String IMPORT_DETAILS_FIELD = "importDetails";


    @Test
    void getModelVersionReturnsModelVersionDefinedByGradle() {
        Publication samplePublication = PublicationGenerator.randomPublication();
        assertThat(samplePublication.getModelVersion(), is(equalTo(ResourcesBuildConfig.RESOURCES_MODEL_VERSION)));
    }

    @Test
    void equalsReturnsTrueWhenTwoPublicationInstancesHaveEquivalentFields() {
        Publication samplePublication = PublicationGenerator.randomPublication();
        Publication copy = samplePublication.copy().build();

        assertThat(copy, doesNotHaveEmptyValuesIgnoringFields(Set.of(DOI_REQUEST_FIELD, BOOK_REVISION_FIELD,
                                                                     IMPORT_DETAILS_FIELD)));

        Diff diff = JAVERS.compare(samplePublication, copy);
        assertThat(copy, is(not(sameInstance(samplePublication))));
        assertThat(diff.prettyPrint(),copy, is(equalTo(samplePublication)));
    }

    @Test
    void objectMapperReturnsSerializationWithAllFieldsSerialized()
            throws JsonProcessingException {
        Publication samplePublication = PublicationGenerator.randomPublication();
        String jsonString = dataModelObjectMapper.writeValueAsString(samplePublication);
        Publication copy = dataModelObjectMapper.readValue(jsonString, Publication.class);
        assertThat(copy, is(equalTo(samplePublication)));
    }

    @Test
    void objectMapperShouldSerializeAndDeserializeNullAssociatedArtifact() throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(new NullAssociatedArtifact()));
        var serialized = dataModelObjectMapper.writeValueAsString(publication);
        var deserialized = dataModelObjectMapper.readValue(serialized, Publication.class);
        assertThat(deserialized, is(equalTo(publication)));
    }

    protected JsonNode toPublicationWithContext(Publication publication) throws IOException {
        JsonNode document = dataModelObjectMapper.readTree(dataModelObjectMapper.writeValueAsString(publication));
        JsonNode context = dataModelObjectMapper.readTree(PUBLICATION_CONTEXT_JSON);
        ContextUtil.injectContext(document, context);
        return document;
    }

    protected Object produceFramedPublication(JsonNode publicationWithContext) throws IOException {
        var input = JsonUtils.fromString(dataModelObjectMapper.writeValueAsString(publicationWithContext));
        var frame = JsonUtils.fromString(FRAME);
        var options = new JsonLdOptions();
        options.setOmitGraph(true);
        options.setPruneBlankNodeIdentifiers(true);
        return JsonLdProcessor.frame(input, frame, options);
    }
}
