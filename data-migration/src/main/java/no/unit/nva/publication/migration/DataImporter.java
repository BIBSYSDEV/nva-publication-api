package no.unit.nva.publication.migration;

import static nva.commons.core.JsonUtils.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.attempt.Try;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonWriter;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

public class DataImporter {

    public static final String CONSECUTIVE_JSON_OBJECTS = "}\\s*\\{";
    public static final String SUCCESSIVE_ELEMENTS_IN_ARRAY = "},{";
    public static final String BEGIN_ARRAY_DELIMITER = "[";
    public static final String END_ARRAY_DELIMITER = "]";
    public static final String ION_ITEM = "Item";

    private final S3Driver s3Client;
    private final Path dataPath;

    public DataImporter(S3Driver s3Client, Path dataPath) {
        this.s3Client = s3Client;
        this.dataPath = dataPath;
    }

    public List<Publication> getPublications() {
        var content = s3Client.getFiles(dataPath);
        var publicationsWithDuplicates = mapIonObjectsToPublications(content.stream())
                                             .collect(Collectors.toList());

        return removeDuplicates(publicationsWithDuplicates.stream());
    }

    protected static List<Publication> removeDuplicates(Stream<Publication> publicationsWithDuplicates) {
        Map<SortableIdentifier, List<Publication>> groupedByIdentifier =
            groupPublicationsByIdentifier(publicationsWithDuplicates);
        return selectLatestVersionForEachIdentifier(groupedByIdentifier);
    }

    private static List<Publication> selectLatestVersionForEachIdentifier(
        Map<SortableIdentifier, List<Publication>> groupedByIdentifier) {

        return groupedByIdentifier.values().stream()
                   .map(DataImporter::selectMostRecentVersion)
                   .collect(Collectors.toList());
    }

    private static Publication selectMostRecentVersion(List<Publication> duplicates) {
        return duplicates.stream().reduce(DataImporter::mostRecent).orElseThrow();
    }

    private static Publication mostRecent(Publication left, Publication right) {
        return left.getModifiedDate().isAfter(right.getModifiedDate()) ? left : right;
    }

    private static Map<SortableIdentifier, List<Publication>> groupPublicationsByIdentifier(
        Stream<Publication> publicationsWithDuplicates) {
        return publicationsWithDuplicates.collect(Collectors.groupingBy(Publication::getIdentifier));
    }

    private static String toJsonObjects(String ion) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (IonWriter writer = IonTextWriterBuilder.json().build(stringBuilder)) {
            rewrite(ion, writer);
        }
        return stringBuilder.toString();
    }

    private static List<Publication> parseJson(String json) {

        JsonNode root = attempt(() -> objectMapperWithEmpty.readTree(json)).orElseThrow();
        if (root.isArray()) {
            ArrayNode rootArray = (ArrayNode) root;
            return StreamSupport.stream(rootArray.spliterator(), false)
                       .map(jsonNode -> jsonNode.get(ION_ITEM))
                       .map(DataImporter::convertToPublication)
                       .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private static Publication convertToPublication(JsonNode item) {
        return objectMapperWithEmpty.convertValue(item, Publication.class);
    }

    private static void rewrite(String textIon, IonWriter writer) throws IOException {
        try (IonReader reader = IonReaderBuilder.standard().build(textIon)) {
            writer.writeValues(reader);
        }
    }

    private static String addArrayDelimiters(String arrayElements) {
        return BEGIN_ARRAY_DELIMITER + arrayElements + END_ARRAY_DELIMITER;
    }

    private static String makeConsecutiveJsonObjectsElementsOfJsonArray(String jsonObjects) {
        return jsonObjects.replaceAll(CONSECUTIVE_JSON_OBJECTS, SUCCESSIVE_ELEMENTS_IN_ARRAY);
    }

    private Stream<Publication> mapIonObjectsToPublications(Stream<String> content) {
        return content
                   .map(attempt(DataImporter::toJsonObjects))
                   .map(attempt -> attempt.map(this::transformMultipleJsonObjectsToJsonArrayWithObjects))
                   .map(Try::orElseThrow)
                   .map(DataImporter::parseJson)
                   .flatMap(Collection::stream);
    }

    private String transformMultipleJsonObjectsToJsonArrayWithObjects(String jsonObjects) {
        String arrayElements = makeConsecutiveJsonObjectsElementsOfJsonArray(jsonObjects);
        return addArrayDelimiters(arrayElements);
    }
}
