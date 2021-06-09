package no.unit.nva.publication.s3imports;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.s3.UnixPath;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;

public class UriWrapper {

    public static final String EMPTY_FRAGMENT = null;
    public static final String ROOT = "/";
    public static final String EMPTY_PATH = null;
    public static final String PATH_DELIMITER = "/";
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
        return attempt(() -> new URI(uri.getScheme(), uri.getHost(), EMPTY_PATH, EMPTY_FRAGMENT))
            .map(UriWrapper::new)
            .orElseThrow();
    }

    /**
     * Appends a path to the URI.
     *
     * @param childPath the path to be appended.
     * @return a UriWrapper containing the whole path.
     */
    public UriWrapper addChild(String... childPath) {
        List<String> thisPathArray = pathToArray(uri.getPath());
        List<String> childPathArray = pathToArray(UnixPath.of(childPath).toString());
        ArrayList<String> totalPathList = new ArrayList<>(thisPathArray);
        totalPathList.addAll(childPathArray);
        String[] totalPathArray = totalPathList.toArray(String[]::new);
        String totalPath = ROOT + String.join(PATH_DELIMITER, totalPathArray);
        return attempt(() -> new URI(uri.getScheme(), uri.getHost(), totalPath, EMPTY_FRAGMENT))
            .map(UriWrapper::new)
            .orElseThrow();
    }

    public String toS3bucketPath() {
        String path = uri.getPath();
        path = path.startsWith(ROOT) ? path.substring(1) : path;
        return path;
    }

    public String getPath() {
        String pathString = uri.getPath();
        return removePathDelimiterFromTheEnd(pathString);
    }

    public Optional<UriWrapper> getParent() {
        return Optional.of(uri)
            .map(URI::getPath)
            .flatMap(this::getParentPath)
            .map(attempt(p -> new URI(uri.getScheme(), uri.getHost(), p, EMPTY_FRAGMENT)))
            .map(Try::orElseThrow)
            .map(UriWrapper::new);
    }

    public Optional<String> getParentPath(String path) {
        return Optional.of(path)
            .map(this::removePathDelimiterFromTheEnd)
            .filter(StringUtils::isNotBlank)
            .map(this::pathToArray)
            .map(pathArray -> pathArray.subList(0, pathArray.size() - 1))
            .map(pathArraySublist -> String.join(PATH_DELIMITER, pathArraySublist))
            .map(pathWithoutRoot -> ROOT + pathWithoutRoot);
    }

    private String removePathDelimiterFromTheEnd(String pathString) {
        return pathString.endsWith(PATH_DELIMITER)
            ? pathString.substring(0, pathString.length() - 1)
            : pathString;
    }

    private List<String> pathToArray(String thisPath) {
        return Arrays.stream(thisPath.split(PATH_DELIMITER))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }
}
