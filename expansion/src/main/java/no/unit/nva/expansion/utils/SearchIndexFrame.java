package no.unit.nva.expansion.utils;

import static nva.commons.core.attempt.Try.attempt;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.ExpansionConfig;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.utils.JsonLdFrameUtil;
import nva.commons.core.ioutils.IoUtils;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class SearchIndexFrame {

    public static JsonDocument getFrameWithContext(Path framePath) {
        return attempt(() -> JsonDocument.of(generateFrameWithContext(framePath))).orElseThrow();
    }

    private static InputStream generateFrameWithContext(Path framePath) {
        var frame = JsonLdFrameUtil.from(IoUtils.stringFromResources(framePath),
                                         Publication.getJsonLdContext(ExpansionConfig.getApiHost()));
        return asInputStream(frame);
    }

    private static ByteArrayInputStream asInputStream(JsonNode frame) {
        return new ByteArrayInputStream(attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsBytes(frame))
                                            .orElseThrow());
    }

    private SearchIndexFrame() {
        // NO-OP
    }
}
