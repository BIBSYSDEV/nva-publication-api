package no.unit.nva.publication.doi;

import java.net.URI;
import java.util.Locale;
import no.unit.nva.publication.doi.dto.Publication;
import no.unit.nva.publication.doi.dto.Publication.Builder;
import no.unit.nva.publication.doi.dto.PublicationDate;
import no.unit.nva.publication.doi.dto.PublicationType;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordDao;

public class PublicationMapper {

    public static final String ERROR_NAMESPACE_MUST_CONTAIN_SUFFIX_SLASH = "Namespace must end with /";
    private static final String NAMESPACE_PUBLICATION = "publication";

    protected String namespacePublication;

    /**
     * Construct a mapper to map between DAOs to DTOs.
     *
     * @param namespace Namespace to use for constructing ids from identifiers that are owned by Publication.
     */
    public PublicationMapper(String namespace) {
        if (namespace == null || !namespace.endsWith("/")) {
            throw new IllegalArgumentException(ERROR_NAMESPACE_MUST_CONTAIN_SUFFIX_SLASH);
        }

        var ns = namespace.toLowerCase(Locale.US);
        this.namespacePublication = ns + NAMESPACE_PUBLICATION;
    }

    private static URI transformIdentifierToId(String namespace, String identifier) {
        return URI.create(namespace + identifier);
    }

    /**
     * Map to doi.{@link Publication} from {@link DynamodbStreamRecordDao}.
     *
     * @param dao {@link DynamodbStreamRecordDao}
     * @return Publication doi.Publication
     */
    public Publication fromDynamodbStreamRecordDao(DynamodbStreamRecordDao dao) {
        return Builder.newBuilder()
            .withId(transformIdentifierToId(namespacePublication, dao.getIdentifier()))
            .withInstitutionOwner(URI.create(dao.getPublisherId()))
            .withMainTitle(dao.getMainTitle())
            .withType(PublicationType.findByName(dao.getPublicationInstanceType()))
            .withPublicationDate(new PublicationDate(dao.getPublicationReleaseDate()))
            .withDoi(URI.create(dao.getDoi()))
            .withContributor(ContributorMapper.fromIdentityDaos(dao.getContributorIdentities()))
            .build();
    }
}
