package no.unit.nva.publication.migration;

import static nva.commons.core.JsonUtils.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.dataimport.S3IonReader;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.attempt.Try;

/**
 * Reads data from an S3 bucket and maps it into POJOs. It expects the data to be a DynamoDb export is S3 in AWS-ION
 * format.
 */
public class DataImporter {

    private final S3Driver s3Client;
    private final Path dataPath;
    private final S3IonReader ionReader;

    public DataImporter(S3Driver s3Client, Path dataPath) {
        this.s3Client = s3Client;
        this.ionReader = new S3IonReader(s3Client);
        this.dataPath = dataPath;
    }

    public List<Publication> getPublications() {
        List<String> filenames = s3Client.listFiles(dataPath);
        Stream<Publication> publicationsWithDuplicates = fetchPublications(filenames.stream());
        return removeDuplicates(publicationsWithDuplicates);
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

    private static Publication convertToPublication(JsonNode item) {
        return objectMapperWithEmpty.convertValue(item, Publication.class);
    }

    private Stream<Publication> fetchPublications(Stream<String> filenames) {
        return filenames
                   .map(attempt(ionReader::extractJsonNodeStreamFromS3File))
                   .flatMap(Try::stream)
                   .flatMap(Function.identity())
                   .map(DataImporter::convertToPublication);
    }
}
