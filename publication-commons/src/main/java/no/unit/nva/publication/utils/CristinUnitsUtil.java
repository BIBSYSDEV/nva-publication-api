package no.unit.nva.publication.utils;

import java.net.URI;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.s3.S3Client;

public interface CristinUnitsUtil {

    String CRISTIN_UNITS_S3_URI_ENV = "CRISTIN_UNITS_S3_URI";

    URI getTopLevel(URI unitId);

    @JacocoGenerated
    static CristinUnitsUtil defaultInstance() {
        return new CristinUnitsUtilImpl(S3Client.create(), new Environment().readEnv(CRISTIN_UNITS_S3_URI_ENV));
    }
}
