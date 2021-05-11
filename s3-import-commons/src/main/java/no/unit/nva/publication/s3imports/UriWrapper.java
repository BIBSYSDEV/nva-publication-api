package no.unit.nva.publication.s3imports;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UriWrapper {

    public static final String EMPTY_FRAGEMENT = null;
    public static final String ROOT = "/";
    public static final String EMPTY_PATH = null;
    private final URI uri;

    public UriWrapper(URI uri) {
        assert Objects.nonNull(uri);
        this.uri = uri;
    }

    public UriWrapper(String uri) {
        this(URI.create(uri));
    }

    public URI getUri() {
        return uri;
    }

    public UriWrapper getHost() {
        return attempt(() -> new URI(uri.getScheme(), uri.getHost(), EMPTY_PATH, EMPTY_FRAGEMENT))
                   .map(UriWrapper::new)
                   .orElseThrow();
    }

    public UriWrapper addChild(Path childPath) {
        Path thisPath = Path.of(uri.getPath());
        List<String> thisPathArray = pathToArray(thisPath);
        List<String> childPathArray = pathToArray(childPath);
        ArrayList<String> totalPathList = new ArrayList<>(thisPathArray);
        totalPathList.addAll(childPathArray);
        String[] totalPathArray = totalPathList.toArray(String[]::new);
        Path totalPath = Path.of(ROOT, totalPathArray);
        return attempt(() -> new URI(uri.getScheme(), uri.getHost(), totalPath.toString(), EMPTY_FRAGEMENT))
                   .map(UriWrapper::new)
                   .orElseThrow();
    }

    public Path toS3bucketPath() {
        String path = uri.getPath();
        path = path.startsWith(ROOT)
                   ? path.substring(1)
                   : path;
        return Path.of(path);
    }

    private List<String> pathToArray(Path thisPath) {
        return StreamSupport.stream(thisPath.spliterator(), false)
                   .map(Path::toString)
                   .collect(Collectors.toList());
    }
}
