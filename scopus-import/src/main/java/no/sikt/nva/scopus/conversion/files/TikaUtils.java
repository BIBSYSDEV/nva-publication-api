package no.sikt.nva.scopus.conversion.files;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import nva.commons.core.JacocoGenerated;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;

@JacocoGenerated
public class TikaUtils {

    public TikaUtils() {
    }

    public TikaInputStream fetch(URI uri) {
        return TikaInputStream.get(uri.toString().getBytes());
    }

    public String getMimeType(TikaInputStream inputStream) throws IOException {
        return TikaConfig.getDefaultConfig().getDetector()
                   .detect(new BufferedInputStream(inputStream), new Metadata()).toString();
    }
}
