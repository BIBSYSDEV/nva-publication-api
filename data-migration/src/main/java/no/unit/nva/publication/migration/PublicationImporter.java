package no.unit.nva.publication.migration;

import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.model.Publication;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.attempt.Try;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonWriter;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

public class PublicationImporter {

    public static final String CONSECUTIVE_JSON_OBJECTS = "}\\s*\\{";
    public static final String SUCCESSIVE_ELEMENTS_IN_ARRAY = "},{";
    public static final String BEGIN_ARRAY_DELIMITER = "[";
    public static final String END_ARRAY_DELIMITER = "]";
    public static final String ION_ITEM = "Item";
    private final S3Driver s3Client;
    private final Path dataPath;

    public PublicationImporter(S3Driver s3Client, Path dataPath) {
        this.s3Client = s3Client;
        this.dataPath = dataPath;
    }

    public List<Publication> getPublications() {
        var content = s3Client.getFiles(dataPath);
        return mapIonObjectsToPublications(content.stream()).collect(Collectors.toList());
    }

    private Stream<Publication> mapIonObjectsToPublications(Stream<String> content) {
        return content
                   .map(attempt(this::toJsonObjects))
                   .map(attempt -> attempt.map(this::transformMultipleJsonObjectsToJsonArrayWithObjects))
                   .map(Try::orElseThrow)
                   .map(this::parseJson)
                   .flatMap(Collection::stream);
    }

    private String transformMultipleJsonObjectsToJsonArrayWithObjects(String jsonObjects) {
        String arrayElements = makeConscutiveJsonObjectsElementsOfJsonArray(jsonObjects);
        return addArrayDelimiters(arrayElements);
    }

    private String addArrayDelimiters(String arrayElements) {
        return BEGIN_ARRAY_DELIMITER + arrayElements + END_ARRAY_DELIMITER;
    }

    private String makeConscutiveJsonObjectsElementsOfJsonArray(String jsonObjects) {
        return jsonObjects.replaceAll(CONSECUTIVE_JSON_OBJECTS, SUCCESSIVE_ELEMENTS_IN_ARRAY);
    }

    private List<Publication> parseJson(String json) {
        JsonNode root = attempt(() -> objectMapper.readTree(json)).orElseThrow();
        if (root.isArray()) {
            ArrayNode rootArray = (ArrayNode) root;
            return StreamSupport.stream(rootArray.spliterator(), false)
                       .map(jsonNode -> jsonNode.get(ION_ITEM))
                       .map(item -> objectMapper.convertValue(item, Publication.class))
                       .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private String toJsonObjects(String ion) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        try (IonWriter writer = IonTextWriterBuilder.json().build(stringBuilder)) {
            rewrite(ion, writer);
        }

        return stringBuilder.toString();
    }

    private void rewrite(String textIon, IonWriter writer) throws IOException {
        IonReader reader = IonReaderBuilder.standard().build(textIon);
        writer.writeValues(reader);
    }
}
