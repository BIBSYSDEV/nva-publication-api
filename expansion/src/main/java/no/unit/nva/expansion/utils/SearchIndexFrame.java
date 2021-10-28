package no.unit.nva.expansion.utils;

import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class SearchIndexFrame {

    public static final String FRAME_SRC = IoUtils.stringFromResources(Path.of("frame.json"));

    private SearchIndexFrame() {
    }
}
