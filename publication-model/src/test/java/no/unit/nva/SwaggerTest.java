package no.unit.nva;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.core.util.Yaml;
import java.io.File;
import java.io.IOException;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import org.junit.jupiter.api.Test;

class SwaggerTest {

    public static final String SCHEMA_YAML = "../documentation/schema.yaml";

    @Test
    void writePublicationSchemaToFile() throws IOException {
        Publication publication = PublicationGenerator.randomPublication();
        ResolvedSchema map = ModelConverters.getInstance().readAllAsResolvedSchema(publication.getClass());

        File file = new File(SCHEMA_YAML);
        Yaml.pretty().writeValue(file, map);
    }
}
