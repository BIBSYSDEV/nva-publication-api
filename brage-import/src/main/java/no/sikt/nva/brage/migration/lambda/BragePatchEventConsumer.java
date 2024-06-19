package no.sikt.nva.brage.migration.lambda;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.s3.S3Client;

public class BragePatchEventConsumer implements RequestHandler<SQSEvent, Void> {

    public static final String SEARCH = "search";
    public static final String RESOURCES = "resources";
    public static final String CATEGORY = "category";
    public static final String ISBN = "isbn";
    public static final String AGGREGATION = "aggregation";
    public static final String NONE = "none";
    public static final String BOOK_ANTHOLOGY = "BookAnthology";
    public static final String CONTENT_TYPE = "application/json";
    public static final String PUBLICATION = "publication";
    public static final Environment ENVIRONMENT = new Environment();
    public static final String BRAGE_MIGRATION_REPORT_BUCKET = ENVIRONMENT.readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME");
    private static final String API_HOST = ENVIRONMENT.readEnv("API_HOST");
    public static final String DIFFERENT_ISBN_ERROR_MESSAGE =
        "Fetched publication does not contain the same isbn: %s as its child %s";
    public static final String MULTIPLE_PARENTS_ERROR_MESSAGE = "Multiple parents fetched for publication : ";
    public static final String PART_OF_ERROR = "PART_OF_ERROR";
    private final UriRetriever uriRetriever;
    private final ResourceService resourceService;
    private final S3Client s3Client;

    @JacocoGenerated
    public BragePatchEventConsumer() {
    this(ResourceService.defaultService(), S3Driver.defaultS3Client().build(), new UriRetriever());
    }
    public BragePatchEventConsumer(ResourceService resourceService, S3Client s3Client, UriRetriever uriRetriever) {
        this.resourceService = resourceService;
        this.s3Client = s3Client;
        this.uriRetriever = uriRetriever;
    }

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        sqsEvent.getRecords().forEach(this::processMessage);
        return null;
    }

    private static PartOfReport parseFile(String value) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(value, PartOfReport.class);
    }

    private static EventReference parseMessageBody(SQSMessage sqsMessage) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(sqsMessage.getBody(), EventReference.class);
    }

    private static boolean containsSingleParent(List<Publication> parentPublicationList) {
        return parentPublicationList.size() == 1;
    }

    private static URI toPublicationId(SortableIdentifier identifier) {
        return UriWrapper.fromHost(API_HOST).addChild(PUBLICATION).addChild(identifier.toString()).getUri();
    }

    private PartOfReport parseBody(SQSMessage sqsMessage) {
        return attempt(() -> parseMessageBody(sqsMessage)).map(EventReference::getUri)
                   .map(uri -> UriWrapper.fromUri(uri).toS3bucketPath())
                   .map(this::fetchFile)
                   .map(BragePatchEventConsumer::parseFile)
                   .orElseThrow();
    }

    private String fetchFile(UnixPath s3Path) {
        return new S3Driver(s3Client, BRAGE_MIGRATION_REPORT_BUCKET).getFile(s3Path);
    }

    private void processMessage(SQSMessage sqsMessage) {
        var partOfReport = parseBody(sqsMessage);
        try {
            var isbnList = partOfReport.getRecord().getPublication().getIsbnList();
            var parentPublicationList = fetchParentPublications(isbnList);

            if (containsSingleParent(parentPublicationList)) {
                var childPublication = fetchPublication(partOfReport.getPublication().getIdentifier());
                injectParent(childPublication, parentPublicationList.getFirst());
                resourceService.updatePublication(childPublication);
            } else {
                var parentIdentifiers = getPublicationIdentifiers(parentPublicationList);
                throw new RuntimeException(MULTIPLE_PARENTS_ERROR_MESSAGE + parentIdentifiers);
            }
        } catch (Exception exception) {
            persistError(exception, partOfReport);
        }
    }

    private static @NotNull List<String> getPublicationIdentifiers(List<Publication> parentPublicationList) {
        return parentPublicationList.stream()
                   .map(Publication::getIdentifier)
                   .map(SortableIdentifier::toString)
                   .toList();
    }

    private void persistError(Exception exception, PartOfReport partOfReport) {
        var s3Driver = new S3Driver(s3Client, BRAGE_MIGRATION_REPORT_BUCKET);
        var errorReportUri = UriWrapper.fromUri(PART_OF_ERROR)
                                 .addChild(partOfReport.getPublication().getIdentifier().toString())
                                 .toS3bucketPath();
        attempt(() -> s3Driver.insertFile(errorReportUri, exception.getMessage())).orElseThrow();
    }

    private void injectParent(Publication childPublication, Publication parentPublication) {
        var context = (Anthology) childPublication.getEntityDescription().getReference().getPublicationContext();
        var parentIdentifier = toPublicationId(parentPublication.getIdentifier());
        context.setId(parentIdentifier);
    }

    private List<Publication> fetchParentPublications(List<String> isbnList) {
        return isbnList.stream()
                   .map(this::fetchPublicationByIsbn)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .filter(publication -> containsIsbn(publication, isbnList))
                   .toList();
    }

    private boolean containsIsbn(Publication publication, List<String> isbnList) {
        var context = (Book) publication.getEntityDescription().getReference().getPublicationContext();
        var publicationIsbnList = context.getIsbnList();
        if (isbnList.stream().noneMatch(publicationIsbnList::contains)) {
            throw new RuntimeException(String.format(DIFFERENT_ISBN_ERROR_MESSAGE, publicationIsbnList, isbnList));
        } else {
            return true;
        }
    }

    private Optional<Publication> fetchPublicationByIsbn(String isbn) {
        return uriRetriever.getRawContent(constructSearchUri(isbn), CONTENT_TYPE)
                   .map(this::toSearchApiResponse)
                   .filter(SearchResourceApiResponse::containsSingleHit)
                   .map(SearchResourceApiResponse::hits)
                   .map(List::getFirst)
                   .map(ResourceWithId::getIdentifier)
                   .map(this::fetchPublication);
    }

    private Publication fetchPublication(SortableIdentifier identifier) {
        return attempt(() -> resourceService.getPublicationByIdentifier(identifier)).orElseThrow();
    }

    private SearchResourceApiResponse toSearchApiResponse(String value) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, SearchResourceApiResponse.class)).orElseThrow();
    }

    private URI constructSearchUri(String isbn) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(SEARCH)
                   .addChild(RESOURCES)
                   .addQueryParameter(CATEGORY, BOOK_ANTHOLOGY)
                   .addQueryParameter(ISBN, isbn)
                   .addQueryParameter(AGGREGATION, NONE)
                   .getUri();
    }
}
