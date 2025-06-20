package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.s3.S3Driver.GZIP_ENDING;
import static nva.commons.core.attempt.Try.attempt;
import java.io.IOException;
import java.net.URI;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.publication.events.handlers.persistence.PersistedDocument;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistedResourcesService {

    private static final Logger logger = LoggerFactory.getLogger(PersistedResourcesService.class);

    private final S3Driver s3Driver;

    public PersistedResourcesService(S3Driver s3Driver) {
        this.s3Driver = s3Driver;
    }

    public URI persist(ExpandedDataEntry expandedDataEntry) {
        return attempt(() -> PersistedDocument.createIndexDocument(expandedDataEntry))
                   .map(this::writeToPersistedResources)
                   .orElseThrow(failure -> throwError(failure, expandedDataEntry));
    }

    private PersistedResourcesException throwError(Failure<URI> failure, ExpandedDataEntry expandedDataEntry) {
        var logMessage = String.format("Failed to persist expanded representation of %s to S3",
                                       expandedDataEntry.toJsonString());
        logger.error(logMessage, failure.getException());
        return new PersistedResourcesException("Failed to persist expanded resource to S3.", failure.getException());
    }

    private URI writeToPersistedResources(PersistedDocument indexDocument) throws IOException {
        var filePath = createFilePath(indexDocument);
        return s3Driver.insertFile(filePath, indexDocument.toJsonString());
    }

    private UnixPath createFilePath(PersistedDocument indexDocument) {
        return UnixPath.of(createPathBasedOnIndexName(indexDocument))
                   .addChild(
                       indexDocument.getConsumptionAttributes().getDocumentIdentifier().toString() + GZIP_ENDING);
    }

    private String createPathBasedOnIndexName(PersistedDocument indexDocument) {
        return indexDocument.getConsumptionAttributes().getIndex();
    }
}
