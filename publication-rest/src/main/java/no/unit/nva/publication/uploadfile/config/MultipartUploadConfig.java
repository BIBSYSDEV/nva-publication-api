package no.unit.nva.publication.uploadfile.config;

import nva.commons.core.Environment;

public final class MultipartUploadConfig {

    public static final String BUCKET_NAME = new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");

    private MultipartUploadConfig() {
    }
}
