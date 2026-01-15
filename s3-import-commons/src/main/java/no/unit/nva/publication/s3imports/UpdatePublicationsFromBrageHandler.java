package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.UpdatePublicationsFromBrageRequest.UpdateType.ABSTRACT;
import static no.unit.nva.publication.s3imports.UpdatePublicationsFromBrageRequest.UpdateType.AFFILIATION;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class UpdatePublicationsFromBrageHandler implements RequestStreamHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePublicationsFromBrageHandler.class);
    private static final String DUBLIN_CORE_XML = "dublin_core.xml";
    private static final String PERSISTED_STORAGE_BUCKET_NAME = "NVA_PERSISTED_STORAGE_BUCKET_NAME";
    private final S3Client s3Client;
    private final ResourceService resourceService;
    private final Environment environment;
    private final List<SortableIdentifier> updatedPublicationIdentifiers = new ArrayList<>();
    private boolean dryRun = true;

    @JacocoGenerated
    public UpdatePublicationsFromBrageHandler() {
        this(ResourceService.defaultService(), S3Driver.defaultS3Client().build(), new Environment());
    }

    public UpdatePublicationsFromBrageHandler(ResourceService resourceService, S3Client s3Client,
                                              Environment environment) {
        this.resourceService = resourceService;
        this.s3Client = s3Client;
        this.environment = environment;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream outputStream, Context context) throws IOException {
        var request = UpdatePublicationsFromBrageRequest.fromInputStream(input);
        this.dryRun = request.dryRun();
        updatedPublicationIdentifiers.clear();
        var file = s3DriverForBucket(request.uri().getHost()).readFile(request.uri());
        var publicationIdentifiers = getPublicationIdentifiersFromCsv(file);

        publicationIdentifiers.forEach(identifier -> process(identifier, request));

        writeResultsToFile(request);
    }

    private static List<SortableIdentifier> getPublicationIdentifiersFromCsv(String value) {
        var csvToBean = new CsvToBeanBuilder<PublicationCsvRow>(new StringReader(value)).withType(
            PublicationCsvRow.class).withIgnoreLeadingWhiteSpace(true).withThrowExceptions(true).build();
        try {
            var rows = csvToBean.parse();
            rows.stream().map(PublicationCsvRow::getIdentifier).findAny().orElseThrow();
            return extractIdentifiers(rows);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed to parse CSV: " + e.getMessage(), e);
        }
    }

    private static List<SortableIdentifier> extractIdentifiers(List<PublicationCsvRow> rows) {
        return rows.stream()
                   .filter(Objects::nonNull)
                   .map(PublicationCsvRow::getIdentifier)
                   .filter(StringUtils::isNotBlank)
                   .map(SortableIdentifier::new)
                   .toList();
    }

    private static Optional<String> getAbstract(Resource resource) {
        return Optional.ofNullable(resource.getEntityDescription())
                   .map(EntityDescription::getAbstract)
                   .filter(StringUtils::isNotBlank);
    }

    private void writeResultsToFile(UpdatePublicationsFromBrageRequest request) throws IOException {
        if (!dryRun) {
            var value = updatedPublicationIdentifiers.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(System.lineSeparator()));
            s3DriverForBucket(request.uri().getHost()).insertFile(
                UnixPath.of("%s-%s".formatted(request.archive(), Instant.now())), value);
        }
    }

    private S3Driver s3DriverForBucket(String request) {
        return new S3Driver(s3Client, request);
    }

    private String fetchFile(File file) {
        return s3DriverForBucket(environment.readEnv(PERSISTED_STORAGE_BUCKET_NAME)).getFile(
            UnixPath.of(file.getIdentifier().toString()));
    }

    private void process(SortableIdentifier sortableIdentifier, UpdatePublicationsFromBrageRequest request) {
        var resource = fetchResource(sortableIdentifier);

        if (resource.isPresent()) {
            updateResource(request, resource.get());
        } else {
            LOGGER.info(String.format("Publication does not exist %s, nothing to update!", sortableIdentifier));
        }
    }

    private void updateResource(UpdatePublicationsFromBrageRequest request, Resource resource) {
        if (ABSTRACT.equals(request.type())) {
            updateAbstract(request, resource);
        }
        if (AFFILIATION.equals(request.type())) {
            updateAffiliation(request, resource);
        }
    }

    private void updateAffiliation(UpdatePublicationsFromBrageRequest request, Resource resource) {
        var contributors = resource.getEntityDescription()
            .getContributors()
            .stream()
            .map(contributor -> updateContributor(contributor, request))
            .toList();
        resource.getEntityDescription().setContributors(contributors);
        updateResource(resource);
    }

    private Contributor updateContributor(Contributor contributor, UpdatePublicationsFromBrageRequest request) {
        return hasAffiliations(contributor)
                   ? contributor
                   : contributor.copy()
                         .withAffiliations(List.of(Organization.fromUri(URI.create(request.value()))))
                         .build();
    }

    private static boolean hasAffiliations(Contributor contributor) {
        return !contributor.getAffiliations().isEmpty();
    }

    private void updateAbstract(UpdatePublicationsFromBrageRequest request, Resource resource) {
        var dublinCore = readDublinCoreFromResource(resource, request.archive());
        if (dublinCore.isPresent()) {
            processResourceWithDublinCore(resource, dublinCore.get());
        } else {
            LOGGER.info(String.format("Dublin core does not exist at publication %s", resource.getIdentifier()));
        }
    }

    private void processResourceWithDublinCore(Resource resource, File file) {
        var dublinCore = DublinCore.fromString(fetchFile(file));
        updateAbstractsWithAbstractFromDublinCore(resource, dublinCore);
    }

    private void updateAbstractsWithAbstractFromDublinCore(Resource resource, DublinCore dublinCore) {
        var resourceAbstract = getAbstract(resource).orElse(null);
        var currentAlternativeAbstracts = resource.getEntityDescription().getAlternativeAbstracts();
        var updatedAlternativeAbstracts = DublinCoreAbstractMerger.mergeAbstracts(dublinCore.getAbstracts(),
                                                                                  resourceAbstract,
                                                                                  currentAlternativeAbstracts);

        if (!updatedAlternativeAbstracts.equals(currentAlternativeAbstracts)) {
            resource.getEntityDescription().setAlternativeAbstracts(updatedAlternativeAbstracts);
            updateResource(resource);
        }
    }

    private void updateResource(Resource resource) {
        if (!dryRun) {
            resourceService.updatePublication(resource.toPublication());
            updatedPublicationIdentifiers.add(resource.getIdentifier());
            LOGGER.info(String.format("Publication updated %s", resource.getIdentifier()));
        }
    }

    private Optional<File> readDublinCoreFromResource(Resource resource, String archive) {
        return resource.getFiles()
                   .stream()
                   .filter(HiddenFile.class::isInstance)
                   .filter(file -> DUBLIN_CORE_XML.equals(file.getName()))
                   .filter(file -> isUploadedFrom(file, archive))
                   .findFirst();
    }

    private boolean isUploadedFrom(File file, String archive) {
        return file.getUploadDetails() instanceof ImportUploadDetails importDetails && archive.equals(
            importDetails.archive());
    }

    private Optional<Resource> fetchResource(SortableIdentifier identifier) {
        return Resource.resourceQueryObject(identifier).fetch(resourceService);
    }
}
