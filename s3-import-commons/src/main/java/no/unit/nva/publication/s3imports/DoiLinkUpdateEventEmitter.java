package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultS3Client;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class DoiLinkUpdateEventEmitter implements RequestStreamHandler {

    public static final String NEW_LINE_REGEX = "\n";
    private static final Logger logger = LoggerFactory.getLogger(DoiLinkUpdateEventEmitter.class);
    private final S3Client s3Client;
    private final ResourceService resourceService;

    @JacocoGenerated
    public DoiLinkUpdateEventEmitter() {
        this(defaultS3Client(), ResourceService.defaultService());
    }

    public DoiLinkUpdateEventEmitter(S3Client s3Client, ResourceService resourceService) {
        this.s3Client = s3Client;
        this.resourceService = resourceService;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        attempt(() -> parseInput(input))
            .map(this::getFileContainingIdentifiers)
            .map(this::extractIdentifiers).get()
            .forEach(this::moveDoiToLinkFieldForPublicationWithCorrespondingIdentifier);
    }

    private void moveDoiToLinkFieldForPublicationWithCorrespondingIdentifier(String identifier) {
        attempt(() -> identifier.replaceAll(" ", StringUtils.EMPTY_STRING))
            .map(SortableIdentifier::new)
            .map(resourceService::getPublicationByIdentifier)
            .map(this::moveDoiToLinkField)
            .map(this::logUpdatedPublication)
            .map(resourceService::updatePublication);
    }

    private Publication logUpdatedPublication(Publication publication) {
        logger.info("Updated publication is: {}", publication.toString());
        return publication;
    }

    private Publication moveDoiToLinkField(Publication publication) {
        return publication.copy()
                   .withDoi(null)
                   .withLink(publication.getDoi())
                   .build();
    }

    private List<String> extractIdentifiers(String string) {
        logger.info(string);
        return Arrays.asList(string.split(NEW_LINE_REGEX));
    }

    private String getFileContainingIdentifiers(EventReference eventReference) {
        S3Driver s3Driver = new S3Driver(s3Client, eventReference.extractBucketName());
        var fileLocation = UriWrapper.fromUri(eventReference.getUri()).toS3bucketPath();
        logger.info(fileLocation.toString());
        return s3Driver.getFile(fileLocation);
    }

    private EventReference parseInput(InputStream input) {
        String inputString = IoUtils.streamToString(input);
        return attempt(() -> EventReference.fromJson(inputString))
                   .orElseThrow();
    }
}
