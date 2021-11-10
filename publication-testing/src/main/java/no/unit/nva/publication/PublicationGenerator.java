package no.unit.nva.publication;

import static no.unit.nva.publication.PublicationInstanceBuilder.randomPublicationInstanceType;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import com.github.javafaker.Faker;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import no.unit.nva.file.model.File;
import no.unit.nva.file.model.FileSet;
import no.unit.nva.file.model.License;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Approval;
import no.unit.nva.model.ApprovalStatus;
import no.unit.nva.model.ApprovalsBody;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Grant;
import no.unit.nva.model.Identity;
import no.unit.nva.model.NameType;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.Role;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticleContentType;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class PublicationGenerator {

    public static final String OWNER = "owner@example.org";
    public static final String PUBLISHER_ID = "http://example.org/123";
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final Faker FAKER = Faker.instance();

    @JacocoGenerated
    private PublicationGenerator() {

    }

    @JacocoGenerated
    public static Publication publicationWithIdentifier() {
        return generatePublication(SortableIdentifier.next());
    }

    @JacocoGenerated
    public static Publication publicationWithoutIdentifier() {
        return generatePublication(null);
    }

    /**
     * Generate a minimal Publication for testing.
     *
     * @param identifier Sortable identifier
     * @return publication
     */
    @JacocoGenerated
    public static Publication generatePublication(SortableIdentifier identifier) {

        EntityDescription entityDescription = createSampleEntityDescription();

        Instant oneMinuteInThePast = Instant.now().minusSeconds(60L);

        return new Publication.Builder()
            .withIdentifier(identifier)
            .withCreatedDate(oneMinuteInThePast)
            .withModifiedDate(oneMinuteInThePast)
            .withOwner(OWNER)
            .withStatus(PublicationStatus.DRAFT)
            .withPublisher(samplePublisher())
            .withEntityDescription(entityDescription)
            .withFileSet(sampleFileSet())
            .withSubjects(Collections.emptyList())
            .build();
    }

    public static Contributor sampleContributor() {
        return new Contributor.Builder()
            .withIdentity(sampleIdentity())
            .withAffiliations(sampleOrganization())
            .withSequence(1)
            .withRole(Role.CREATOR)
            .build();
    }

    public static Publication generateEmptyPublication() {
        return new Publication.Builder()
            .withOwner(OWNER)
            .withPublisher(samplePublisher())
            .build();
    }

    public static List<Publication> samplePublicationsOfDifferentOwners(int numberOfPublications,
                                                                        boolean withIdentifier) {
        return IntStream.range(0, numberOfPublications).boxed()
            .map(ignored -> PublicationGenerator.publicationWithoutIdentifier())
            .map(pub -> addIdentifier(pub, withIdentifier))
            .map(PublicationGenerator::changeOwner)
            .collect(Collectors.toList());
    }

    public static URI randomUri() {
        String uriString = "https://www.example.org/" + randomWord() + randomWord();
        return URI.create(uriString);
    }

    public static Publication randomPublication(){
        return randomPublication(randomPublicationInstanceType());
    }

    public static Publication randomPublication(Class<?> publicationInstanceClass) {

        return new Publication.Builder()
            .withIdentifier(SortableIdentifier.next())
            .withPublisher(randomOrganization())
            .withSubjects(List.of(randomUri()))
            .withStatus(randomArrayElement(PublicationStatus.values()))
            .withPublishedDate(randomInstant())
            .withModifiedDate(randomInstant())
            .withAdditionalIdentifiers(Set.of(randomAdditionalIdentifier()))
            .withProjects(randomProjects())
            .withOwner(randomString())
            .withLink(randomUri())
            .withIndexedDate(randomInstant())
            .withHandle(randomUri())
            .withDoi(randomDoi())
            .withCreatedDate(randomInstant())
            .withEntityDescription(randomEntityDescription(publicationInstanceClass))
            .withFileSet(FileSetGenerator.randomFileSet())
            .build();
    }

    private static EntityDescription randomEntityDescription(Class<?> publicationInstanceClass) {
        return EntityDescriptionBuilder.randomEntityDescription(publicationInstanceClass);
    }

    private static URI randomDoi() {
        return URI.create("https://doi.org/10.1234/" + randomWord());
    }

    private static List<ResearchProject> randomProjects() {
        return List.of(randomResearchProject());
    }

    private static ResearchProject randomResearchProject() {
        return new ResearchProject.Builder()
            .withId(randomUri())
            .withName(randomString())
            .withApprovals(randomApprovals())
            .withGrants(randomGrants())
            .build();
    }

    private static List<Grant> randomGrants() {
        return List.of(randomGrant());
    }

    private static Grant randomGrant() {
        return new Grant.Builder()
            .withId(randomString())
            .withSource(randomString())
            .build();
    }

    private static List<Approval> randomApprovals() {
        return List.of(randomApproval());
    }

    private static Approval randomApproval() {
        return new Approval.Builder()
            .withApprovalStatus(randomArrayElement(ApprovalStatus.values()))
            .withDate(randomInstant())
            .withApplicationCode(randomString())
            .withApprovedBy(randomArrayElement(ApprovalsBody.values()))
            .build();
    }

    private static AdditionalIdentifier randomAdditionalIdentifier() {
        return new AdditionalIdentifier(randomString(), randomString());
    }

    private static Organization randomOrganization() {
        return new Organization.Builder()
            .withId(randomUri())
            .withLabels(Map.of(randomString(), randomString()))
            .build();
    }

    private static List<Organization> sampleOrganization() {
        Organization organization = new Organization.Builder()
            .withId(URI.create("https://someOrganziation.example.com"))
            .withLabels(Map.of("someLabelKey", "someLabelValue"))
            .build();
        return List.of(organization);
    }

    private static Identity sampleIdentity() {
        return new Identity.Builder()
            .withName(OWNER)
            .withId(URI.create("https://someUserId.example.org"))
            .withArpId("someArpId")
            .withNameType(NameType.PERSONAL)
            .withOrcId("someOrcId")
            .build();
    }

    private static Organization samplePublisher() {
        return new Organization.Builder()
            .withId(URI.create(PUBLISHER_ID))
            .build();
    }

    private static FileSet sampleFileSet() {
        List<File> files = List.of(new File.Builder()
                .withIdentifier(UUID.randomUUID())
                .withLicense(new License.Builder()
                        .withIdentifier("licenseId")
                        .build())
                .build());
        return new FileSet(files);
    }

    private static EntityDescription createSampleEntityDescription() {
        Contributor contributor = attempt(PublicationGenerator::sampleContributor).orElseThrow();

        PublicationInstance<? extends Pages> publicationInstance =
            new JournalArticle.Builder()
                .withArticleNumber("1")
                .withIssue("2")
                .withVolume("Volume 1")
                .withPages(new Range("beginRange", "endRange"))
                .withContent(randomArrayElement(JournalArticleContentType.values()))
                .build();
        Reference reference = new Reference.Builder().withPublicationInstance(publicationInstance).build();

        return new EntityDescription.Builder()
            .withMainTitle(randomString())
            .withDate(new PublicationDate.Builder().withYear("2020").withMonth("2").withDay("31").build())
            .withReference(reference)
            .withContributors(List.of(contributor))
            .build();
    }

    private static <T> T randomArrayElement(T... array) {
        return array[RANDOM.nextInt(array.length)];
    }

    private static Publication addIdentifier(Publication pub, boolean addIdentifier) {
        if (addIdentifier) {
            pub.setIdentifier(SortableIdentifier.next());
        }
        return pub;
    }

    private static Publication changeOwner(Publication publication) {
        return publication.copy().withOwner(randomEmail()).build();
    }

    private static String randomEmail() {
        return FAKER.internet().emailAddress();
    }

    private static String randomWord() {
        return FAKER.lorem().word();
    }
}
