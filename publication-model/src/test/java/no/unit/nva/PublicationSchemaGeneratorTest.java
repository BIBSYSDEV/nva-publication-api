package no.unit.nva;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.io.IOException;
import no.unit.nva.model.Publication;
import org.junit.jupiter.api.Test;

class PublicationSchemaGeneratorTest {

    public static final String SCHEMA_YAML = "../documentation/schema.yaml";

    @Test
    void writePublicationSchemaToFile() throws IOException {
        var model = ModelConverters.getInstance().readAll(Publication.class);
        model.forEach(this::removeDiscriminator);

        File file = new File(SCHEMA_YAML);
        Yaml.pretty().writeValue(file, model);
    }

    private void removeDiscriminator(String schemaName, Schema schema) {
        schema.setDiscriminator(null);
    }
}