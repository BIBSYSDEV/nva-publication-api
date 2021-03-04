package no.unit.nva.publication.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;

public class DataMigration {

    private final S3Driver s3Driver;
    private final Path s3DataPath;
    private final DoiRequestService doiRequestService;
    private final PublicationImporter publicationImporter;
    private final MessageService messageService;
    private ReportGenerator reportGenerator;

    public DataMigration(S3Driver s3Driver, Path s3DataPath,
                         ResourceService resourceService,
                         DoiRequestService doiRequestService,
                         MessageService messageService) {

        this.s3Driver = s3Driver;
        this.s3DataPath = s3DataPath;
        this.doiRequestService = doiRequestService;
        this.messageService = messageService;
        this.publicationImporter = new PublicationImporter(s3Driver, s3DataPath, resourceService);
        this.reportGenerator = new ReportGenerator();
    }

    public List<ResourceUpdate> migrateData() throws IOException {
        List<ResourceUpdate> allUpdates = executeMigration();
        generateReport(allUpdates);
        return allUpdates;
    }

    @SuppressWarnings("unchecked")
    private List<ResourceUpdate> executeMigration() {
        List<Publication> publications = publicationImporter.getPublications();

        List<ResourceUpdate> publicationsUpdateResult = updateResources(publications);

        List<ResourceUpdate> doiRequestUpdatesResult = updateDoiRequests(publications);

        List<ResourceUpdate> messageUpdateResult = updateMessages();

        return mergeUpdateResults(publicationsUpdateResult,
            doiRequestUpdatesResult,
            messageUpdateResult
        );
    }

    private void generateReport(List<ResourceUpdate> publicationsUpdateResult) throws IOException {

        setupReportGenerator(publicationsUpdateResult);
        generateReports();
    }

    private void setupReportGenerator(List<ResourceUpdate> resourceUpdatesResults) {
        reportGenerator = new ReportGenerator(resourceUpdatesResults);
    }

    private void generateReports() throws IOException {
        reportGenerator.writeDifferences();
        reportGenerator.writeFailures();
    }

    private List<ResourceUpdate> updateMessages() {
        MessagesImporter messagesImporter = new MessagesImporter(s3Driver, s3DataPath, messageService);
        List<PublicationMessagePair> allMessages = messagesImporter.geDoiRequestMessages().collect(Collectors.toList());

        return messagesImporter.insertMessages(allMessages.stream());
    }

    private List<ResourceUpdate> mergeUpdateResults(List<ResourceUpdate>... updateResults) {
        return Arrays.stream(updateResults)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toList());
    }

    private List<ResourceUpdate> updateDoiRequests(List<Publication> publications) {
        DoiRequestImporter doiRequestImporter = new DoiRequestImporter(s3Driver, s3DataPath, doiRequestService);
        return doiRequestImporter.insertDoiRequests(publications);
    }

    private List<ResourceUpdate> updateResources(List<Publication> publications) {
        return publicationImporter.insertPublications(publications)
                   .stream()
                   .map(ResourceUpdate::compareVersions)
                   .collect(Collectors.toList());
    }
}
