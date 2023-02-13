package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultS3Client;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class DoiToReferenceDoiUpdateEventEmitter implements RequestStreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(DoiToReferenceDoiUpdateEventEmitter.class);
    private final S3Client s3Client;
    private final ResourceService resourceService;

    @JacocoGenerated
    public DoiToReferenceDoiUpdateEventEmitter() {
        this(defaultS3Client(), ResourceService.defaultService());
    }

    public DoiToReferenceDoiUpdateEventEmitter(S3Client s3Client, ResourceService resourceService) {
        this.s3Client = s3Client;
        this.resourceService = resourceService;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        fetchIdentifiersFromInputEvent(input)
            .map(this::fetchPublication)
            .forEach(this::updatePublication);
    }

    private Publication fetchPublication(SortableIdentifier identifier) {
        return attempt(() -> resourceService.getPublicationByIdentifier(identifier)).orElseThrow();
    }

    private void updatePublication(Publication publication) {
        var update = moveDoiToReferenceDoi(publication);
        resourceService.updatePublication(update);
    }

    private Publication moveDoiToReferenceDoi(Publication publication) {
        publication.getEntityDescription().getReference().setDoi(publication.getDoi());
        publication.setDoi(null);
        return publication;
    }

    private Stream<SortableIdentifier> fetchIdentifiersFromInputEvent(InputStream inputStream) {
        var eventReference = parseInput(inputStream);
        var s3Driver = new S3Driver(s3Client, eventReference.extractBucketName());
        var fileLocation = UriWrapper.fromUri(eventReference.getUri()).toS3bucketPath();
        logger.info(fileLocation.toString());
        return s3Driver.getFile(fileLocation)
                   .lines()
                   .map(String::trim)
                   .map(SortableIdentifier::new);
    }

    private EventReference parseInput(InputStream input) {
        var inputString = IoUtils.streamToString(input);
        return attempt(() -> EventReference.fromJson(inputString)).orElseThrow();
    }
}
