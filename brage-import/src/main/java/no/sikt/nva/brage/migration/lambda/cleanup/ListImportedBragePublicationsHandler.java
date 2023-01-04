package no.sikt.nva.brage.migration.lambda.cleanup;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultS3Client;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ListImportedBragePublicationsHandler implements RequestHandler<InputStream, List<String>> {

    public static final String EXPECTED_BODY_MESSAGE =
        "The expected json body contains only an s3Location.\nThe received body was: ";
    private static final Logger logger = LoggerFactory.getLogger(ListImportedBragePublicationsHandler.class);
    private final S3Client s3Client;

    public ListImportedBragePublicationsHandler(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @JacocoGenerated
    public ListImportedBragePublicationsHandler() {
        this(defaultS3Client());
    }

    @Override
    public List<String> handleRequest(InputStream input, Context context) {
        var importRequest = parseInput(input);
        return listFiles(importRequest).stream().map(uri -> getIdentifier(uri.toString())).collect(Collectors.toList());
    }

    private static String getIdentifier(String key) {
        return key.split("/")[key.split("/").length - 1];
    }

    private EventReference parseInput(InputStream input) {
        String inputString = IoUtils.streamToString(input);
        return attempt(() -> EventReference.fromJson(inputString))
                   .toOptional()
                   .filter(event -> nonNull(event.getUri()))
                   .orElseThrow(() -> new IllegalArgumentException(EXPECTED_BODY_MESSAGE + inputString));
    }

    private List<URI> listFiles(EventReference importRequest) {
        S3Driver s3Driver = new S3Driver(s3Client, importRequest.extractBucketName());
        List<UnixPath> filenames = s3Driver.listAllFiles(importRequest.getUri());
        logger.info(attempt(() -> s3ImportsMapper.writeValueAsString(filenames)).orElseThrow());
        return filenames.stream()
                   .map(filename -> createUri(importRequest.getUri(), filename))
                   .collect(Collectors.toList());
    }

    private URI createUri(URI s3Location, UnixPath filename) {
        return Try.of(s3Location)
                   .map(UriWrapper::fromUri)
                   .map(UriWrapper::getHost)
                   .map(uri -> uri.addChild(filename))
                   .map(UriWrapper::getUri)
                   .orElseThrow();
    }
}
