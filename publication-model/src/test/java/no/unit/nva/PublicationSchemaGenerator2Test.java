package no.unit.nva;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import no.unit.nva.model.funding.Funding;
import org.junit.jupiter.api.Test;

class PublicationSchemaGenerator2Test {

    public static final String SCHEMA_YAML = "../documentation/schema-2.yaml";

    @Test
    void writePublicationSchemaToFile() throws IOException {
        ModelConverters.getInstance().addConverter(new CustomModelConverter());
        var model = ModelConverters.getInstance().readAll(Pub.class);
        model.forEach(this::removeDiscriminator);

        File file = new File(SCHEMA_YAML);
        Yaml.pretty().writeValue(file, model);
    }

    private void removeDiscriminator(String schemaName, Schema schema) {
        schema.setDiscriminator(null);
    }
    public class CustomModelConverter implements ModelConverter {

        @Override
        public boolean isOpenapi31() {
            return ModelConverter.super.isOpenapi31();
        }

        @Override
        public Schema resolve(AnnotatedType annotatedType, ModelConverterContext modelConverterContext,
                              Iterator<ModelConverter> iterator) {
            var type = annotatedType.getType();
            var _type = Json.mapper().constructType(type);

            if (Objects.nonNull(annotatedType.getCtxAnnotations())) {
                Arrays.stream(annotatedType.getCtxAnnotations()).forEach((a) -> {
                    if (a.annotationType().getName().equals("io.swagger.v3.oas.annotations.media.Schema")) {
                        System.out.println("Schema annotation");
                        var schema = (io.swagger.v3.oas.annotations.media.Schema) a;
                        var subtypes = ((io.swagger.v3.oas.annotations.media.Schema) a).subTypes();
                        if (Objects.nonNull(subtypes)) {
                            System.out.println("Subtypes:");
                            Arrays.stream(subtypes).forEach((c) -> {
                                System.out.println(c.getName());
                            });
                        }

                    }
                    System.out.println(a.annotationType().getName());
                });
            }


            if (_type.getRawClass().equals(Funding.class)) {

            }

            // It's needed to follow chain for unresolved types
            if (iterator.hasNext()) {
                return iterator.next().resolve(annotatedType, modelConverterContext, iterator);
            }
            return null;
        }
    }

    public class Pub {
        public List<Funds> getFunds() {
            return null;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(name = "UncomfirmedFunds", value = UncomfirmedFunds.class),
        @JsonSubTypes.Type(name = "ComfirmedFunds", value = ComfirmedFunds.class)
    })
    //@io.swagger.v3.oas.annotations.media.Schema(oneOf = {UncomfirmedFunds.class, ComfirmedFunds.class})
    public interface Funds {
        abstract String getSharedField();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public class UncomfirmedFunds implements Funds {

        @Override
        public String getSharedField() {
            return null;
        }

        public String getUnomfirmedField() {
            return null;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public class ComfirmedFunds implements Funds {

        @Override
        public String getSharedField() {
            return null;
        }

        public String getComfirmedField() {
            return null;
        }
    }

}