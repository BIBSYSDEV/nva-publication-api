package no.sikt.nva.brage.migration;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class BrageLocation {

    public static final String ORIGIN_INFORMATION_STRING_TEMPLATE = "Bundle location: %s, Handle: %s";
    public static final String ORIGIN_INFORMATION = "Bundle location: %s, title: \"%s\"";
    private final Path brageBundlePath;
    private URI handle;

    private String title;

    public BrageLocation(Path brageBundlePath) {
        this.brageBundlePath = brageBundlePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Path getBrageBundlePath() {
        return brageBundlePath;
    }

    public URI getHandle() {
        return handle;
    }

    public void setHandle(URI handle) {
        this.handle = handle;
    }

    public String getOriginInformation() {
        return Objects.nonNull(handle)
                   ? String.format(ORIGIN_INFORMATION_STRING_TEMPLATE, getBrageBundlePath(), getHandle())
                   : String.format(ORIGIN_INFORMATION, getBrageBundlePath(), getTitle());
    }
}
