package no.unit.nva.publication.storage.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.publication.storage.model.daos.ResourceDao.CRISTIN_SOURCE;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Approval;
import no.unit.nva.model.ApprovalStatus;
import no.unit.nva.model.ApprovalsBody;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.DoiRequest;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.File;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Grant;
import no.unit.nva.model.Identity;
import no.unit.nva.model.License;
import no.unit.nva.model.NameType;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResearchProject.Builder;
import no.unit.nva.model.Role;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticleContentType;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JsonUtils;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Test;

public class ResourceTest {
    
    public static final URI SAMPLE_ORG_URI = URI.create("https://www.example.com/123");
    public static final Organization SAMPLE_ORG = sampleOrganization();
    public static final URI SAMPLE_DOI = URI.create("http://doi.org/123-456");
    public static final Instant SAMPLE_APPROVAL_DATE = Instant.parse("2020-05-03T12:22:22.00Z");
    public static final String SOME_OWNER = "some@owner.no";
    public static final URI SOME_LINK = URI.create("https://example.org/somelink");
    public static final Instant EMBARGO_DATE = Instant.parse("2021-01-01T12:00:22.23Z");
    public static final URI SAMPLE_ID = URI.create("https://example.com/some/id");
    
    public static final Instant RESOURCE_CREATION_TIME = Instant.parse("1900-12-03T10:15:30.00Z");
    public static final Instant RESOURCE_MODIFICATION_TIME = Instant.parse("2000-01-03T00:00:18.00Z");
    public static final Instant RESOURCE_PUBLISHED_DATE = Instant.parse("2012-04-03T06:12:35.00Z");
    public static final Instant RESOURCE_INDEXED_TIME = Instant.parse("2013-05-03T12:22:22.00Z");
    public static final URI SAMPLE_LANGUAGE = URI.create("https://some.com/language");
    public static final String SAMPLE_ISSN = "2049-3630";
    public static final URI SOME_HOST = URI.create("https://example.org/");
    public static final String DOI_REQUEST_FIELD = "doiRequest";

    public static final DoiRequest EMPTY_DOI_REQUEST = null;
    public static final boolean NON_DEFAULT_BOOLEAN_VALUE = true;
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private final FileSet sampleFileSet = sampleFileSet();
    private final List<ResearchProject> sampleProjects = sampleProjects();
    private final Javers javers = JaversBuilder.javers().build();
    private final SortableIdentifier sampleIdentifier = SortableIdentifier.next();

    @Test
    public void builderContainsAllFields() {
        Resource resource = sampleResource();
        assertThat(resource, doesNotHaveEmptyValues());
    }

    @Test
    public void copyContainsAllFields() {
        Resource resource = sampleResource();
        Resource copy = resource.copy().build();
        JsonNode resourceJson = JsonUtils.objectMapper.convertValue(resource, JsonNode.class);
        JsonNode copyJson = JsonUtils.objectMapper.convertValue(copy, JsonNode.class);
        assertThat(resource, doesNotHaveEmptyValues());
        assertThat(copy, is(equalTo(resource)));
        assertThat(resourceJson, is(equalTo(copyJson)));
    }
    
    @Test
    public void toDtoReturnsDtoWithoutLossOfInformation() {
        Resource resource = sampleResource();
        assertThat(resource, doesNotHaveEmptyValues());
        Publication publication = resource.toPublication();
        Resource fromPublication = Resource.fromPublication(publication);
        Diff diff = javers.compare(resource, fromPublication);
        assertThat(diff.prettyPrint(), diff.getChanges().size(), is(0));
    }
    
    @Test
    public void fromDtoToDaoToDtoReturnsDtoWithoutLossOfInformation()
        throws MalformedURLException, InvalidIssnException {
        Publication expected = samplePublication(sampleJournalArticleReference());
        assertThat(expected, doesNotHaveEmptyValuesIgnoringFields(Set.of(DOI_REQUEST_FIELD)));
        
        Publication transformed = Resource.fromPublication(expected).toPublication();
        
        Diff diff = javers.compare(expected, transformed);
        assertThat(diff.prettyPrint(), diff.getChanges().size(), is(0));
        
        assertThat(transformed, is(equalTo(expected)));
    }
    
    @Test
    public void emptyResourceReturnsResourceWithTheMinimumNecessaryFieldsNotNull() {

        Resource emptyResource = Resource.emptyResource(SOME_OWNER, SOME_HOST, sampleIdentifier);
        assertThat(emptyResource.getIdentifier(), is(equalTo(sampleIdentifier)));
        assertThat(emptyResource.getPublisher().getId(), is(equalTo(SOME_HOST)));
        assertThat(emptyResource.getOwner(), is(equalTo(SOME_OWNER)));
    }
    
    @Test
    public void queryObjectReturnsResourceWithIdentifier() {
        Resource resource = Resource.resourceQueryObject(sampleIdentifier);
        assertThat(resource.getIdentifier(), is(equalTo(sampleIdentifier)));
    }
    
    @Test
    public void queryObjectReturnsResourceWithIdentifierAndPublisher() {
        UserInstance userInstance = new UserInstance(SOME_OWNER, SOME_HOST);
        Resource resource = Resource.resourceQueryObject(userInstance, sampleIdentifier);
        assertThat(resource.getIdentifier(), is(equalTo(sampleIdentifier)));
        assertThat(resource.getPublisher().getId(), is(equalTo(SOME_HOST)));
        assertThat(resource.getOwner(), is(equalTo(SOME_OWNER)));
    }
    
    public Publication samplePublication(Reference reference) {
        return new Publication.Builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withCreatedDate(RESOURCE_CREATION_TIME)
                   .withModifiedDate(RESOURCE_MODIFICATION_TIME)
                   .withIndexedDate(RESOURCE_INDEXED_TIME)
                   .withPublishedDate(RESOURCE_PUBLISHED_DATE)
                   .withOwner(SOME_OWNER)
                   .withPublisher(SAMPLE_ORG)
                   .withDoi(SAMPLE_DOI)
                   .withFileSet(sampleFileSet)
                   .withHandle(randomUri())
                   .withStatus(PublicationStatus.PUBLISHED)
                   .withLink(SOME_LINK)
                   .withProjects(sampleProjects)
                   .withDoiRequest(EMPTY_DOI_REQUEST)
                   .withEntityDescription(sampleEntityDescription(reference))
                   .withAdditionalIdentifiers(sampleAdditionalIdentifiers())
                   .withSubjects(List.of(randomUri()))
                   .build();
    }

    private Set<AdditionalIdentifier> sampleAdditionalIdentifiers() {
        AdditionalIdentifier cristinIdentifier = new AdditionalIdentifier(CRISTIN_SOURCE, randomString());
        AdditionalIdentifier otherRandomIdentifier = new AdditionalIdentifier(randomString(), randomString());
        return Set.of(cristinIdentifier, otherRandomIdentifier);
    }

    public Reference sampleJournalArticleReference() throws InvalidIssnException, MalformedURLException {
        return new Reference.Builder()
                   .withDoi(randomUri())
                   .withPublishingContext(sampleUnconfirmedJournalInstance())
                   .withPublicationInstance(sampleJournalArticle())
                   .build();
    }

    private static Identity sampleIdentity() {
        return new Identity.Builder()
                   .withId(SAMPLE_ID)
                   .withName(randomString())
                   .withArpId(randomString())
                   .withNameType(NameType.PERSONAL)
                   .withOrcId(randomString())
                   .build();
    }
    
    private static Organization sampleOrganization() {
        return new Organization.Builder()
                   .withId(SAMPLE_ORG_URI)
                   .withLabels(Map.of(randomString(), randomString()))
                   .build();
    }
    
    private static String randomString() {
        return UUID.randomUUID().toString();
    }
    
    private EntityDescription sampleEntityDescription(Reference reference) {
        Map<String, String> alternativeTitles = Map.of(randomString(), randomString());
        return new EntityDescription.Builder()
                   .withDate(randomPublicationDate())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withAlternativeTitles(alternativeTitles)
                   .withContributors(sampleContributor())
                   .withLanguage(SAMPLE_LANGUAGE)
                   .withMainTitle(randomString())
                   .withMetadataSource(randomUri())
                   .withNpiSubjectHeading(randomString())
                   .withTags(List.of(randomString()))
                   .withReference(reference)
                   .build();
    }
    
    private JournalArticle sampleJournalArticle() {
        return new JournalArticle.Builder()
                   .withPeerReviewed(NON_DEFAULT_BOOLEAN_VALUE)
                   .withArticleNumber(randomString())
                   .withIssue(randomString())
                   .withPages(new Range.Builder().withBegin(randomString()).withEnd(randomString()).build())
                   .withVolume(randomString())
                   .withContent(randomArrayElement(JournalArticleContentType.values()))
                   .build();
    }
    
    private UnconfirmedJournal sampleUnconfirmedJournalInstance() throws InvalidIssnException {
        return new UnconfirmedJournal(randomString(), SAMPLE_ISSN, SAMPLE_ISSN);
    }
    
    private PublicationDate randomPublicationDate() {
        return new PublicationDate.Builder().withDay(randomString())
                   .withMonth(randomString())
                   .withYear(randomString())
                   .build();
    }
    
    private List<Contributor> sampleContributor() {
        Contributor contributor = new Contributor.Builder()
                                                    .withIdentity(sampleIdentity())
                                                    .withAffiliations(List.of(SAMPLE_ORG))
                                                    .withRole(Role.CREATOR)
                                                    .withSequence(1)
                                                    .build();
        return List.of(contributor);
    }
    
    private List<ResearchProject> sampleProjects() {
        Approval approval = new Approval.Builder()
                                .withApprovalStatus(ApprovalStatus.APPLIED)
                                .withApplicationCode(randomString())
                                .withApprovedBy(ApprovalsBody.NMA)
                                .withDate(SAMPLE_APPROVAL_DATE)
                                .build();
        
        Grant grant = new Grant.Builder()
                          .withId(randomString())
                          .withSource(randomString())
                          .build();
        ResearchProject researchProject = new Builder().withId(randomUri())
                                              .withApprovals(List.of(approval))
                                              .withGrants(List.of(grant))
                                              .withName(randomString())
                                              .build();
        
        return List.of(researchProject);
    }
    
    private FileSet sampleFileSet() {
        FileSet files = new FileSet();
        License license = new License.Builder()
                              .withIdentifier(randomString())
                              .withLabels(Map.of(randomString(), randomString()))
                              .withLink(randomUri())
                              .build();
        File file = new File.Builder()
                        .withIdentifier(UUID.randomUUID())
                        .withAdministrativeAgreement(NON_DEFAULT_BOOLEAN_VALUE)
                        .withIdentifier(UUID.randomUUID())
                        .withEmbargoDate(EMBARGO_DATE)
                        .withMimeType(randomString())
                        .withSize(100L)
                        .withLicense(license)
                        .withName(randomString())
                        .build();
        files.setFiles(List.of(file));
        return files;
    }

    private Resource sampleResource() {
        return attempt(() -> Resource.fromPublication(samplePublication(sampleJournalArticleReference())))
                   .orElseThrow();
    }

    private URI randomUri() {
        return URI.create(SOME_HOST + UUID.randomUUID().toString());
    }

    private <T> T randomArrayElement(T... array) {
        int randomIndex = RANDOM.nextInt(array.length);
        return array[randomIndex];
    }
}