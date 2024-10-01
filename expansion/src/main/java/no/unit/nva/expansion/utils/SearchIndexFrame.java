package no.unit.nva.expansion.utils;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import nva.commons.core.ioutils.IoUtils;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class SearchIndexFrame {
    
    public static final Document FRAME_SRC;

    static {
        try {
            FRAME_SRC = JsonDocument.of(IoUtils.inputStreamFromResources("frame.json"));
        } catch (JsonLdError e) {
            throw new RuntimeException(e);
        }
    }

    private SearchIndexFrame() {
    }
}
