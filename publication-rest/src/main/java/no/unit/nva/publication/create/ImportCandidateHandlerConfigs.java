package no.unit.nva.publication.create;

import no.unit.nva.publication.create.pia.PiaClientConfig;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.s3.S3Client;
import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_S3_CLIENT;

public record ImportCandidateHandlerConfigs(String persistedStorageBucket,
                                            String importCandidateStorageBucket,
                                            ResourceService importCandidateService,
                                            ResourceService publicationService,
                                            TicketService ticketService,
                                            S3Client s3Client,
                                            PiaClientConfig piaClientConfig) {

    public static final String NVA_PERSISTED_STORAGE_BUCKET_NAME = "NVA_PERSISTED_STORAGE_BUCKET_NAME";
    public static final String IMPORT_CANDIDATES_STORAGE_BUCKET = "IMPORT_CANDIDATES_STORAGE_BUCKET";
    public static final String IMPORT_CANDIDATES_TABLE_NAME = "IMPORT_CANDIDATES_TABLE_NAME";
    public static final String RESOURCE_TABLE_NAME = "RESOURCE_TABLE_NAME";

    @JacocoGenerated
    public static ImportCandidateHandlerConfigs getDefaultsConfigs() {
        var environment = new Environment();
        return new ImportCandidateHandlerConfigs(
            environment.readEnv(NVA_PERSISTED_STORAGE_BUCKET_NAME),
            environment.readEnv(IMPORT_CANDIDATES_STORAGE_BUCKET),
            ResourceService.defaultService(environment.readEnv(IMPORT_CANDIDATES_TABLE_NAME)),
            ResourceService.defaultService(environment.readEnv(RESOURCE_TABLE_NAME)),
            TicketService.defaultService(),
            DEFAULT_S3_CLIENT,
            PiaClientConfig.getDefaultConfig()
        );
    }
}
