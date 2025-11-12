package no.unit.nva.model.testing;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static no.unit.nva.model.testing.PublicationContextBuilder.randomPublicationContext;
import static no.unit.nva.model.testing.PublicationInstanceBuilder.randomPublicationInstance;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedLink;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportContributor;
import no.unit.nva.importcandidate.ImportEntityDescription;
import no.unit.nva.importcandidate.ImportOrganization;
import no.unit.nva.importcandidate.ImportStatusFactory;
import no.unit.nva.importcandidate.ScopusAffiliation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.additionalidentifiers.ScopusIdentifier;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;

public final class ImportCandidateGenerator {

    private ImportCandidateGenerator() {
    }

    public static ImportCandidate randomImportCandidate() {
        return randomImportCandidate(randomPublicationContext(randomPublicationInstance().getClass()));
    }

    public static ImportCandidate randomImportCandidate(List<ImportContributor> contributors) {
        return new ImportCandidate.Builder().withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(
                       randomImportEntityDescription(randomPublicationContext(randomPublicationInstance().getClass()),
                                                     contributors))
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withIdentifier(SortableIdentifier.next())
                   .withAdditionalIdentifiers(Set.of(ScopusIdentifier.fromValue(randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of(randomOpenFile(), randomAssociatedLink()))
                   .withAssociatedCustomers(List.of(randomUri(), randomUri()))
                   .build();
    }

    public static ImportCandidate randomImportCandidate(PublicationContext publicationContext) {
        return new ImportCandidate.Builder().withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(
                       randomImportEntityDescription(publicationContext, List.of(randomImportContributor())))
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withIdentifier(SortableIdentifier.next())
                   .withAdditionalIdentifiers(Set.of(ScopusIdentifier.fromValue(randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of(randomOpenFile(), randomAssociatedLink()))
                   .withAssociatedCustomers(List.of(randomUri(), randomUri()))
                   .build();
    }

    public static ImportContributor randomImportContributor() {
        return randomImportContributorWithAffiliationId(randomUri());
    }

    public static ImportContributor randomImportContributorWithAffiliationId(URI affiliationId) {
        return new ImportContributor(randomIdentityForContributorFromScopus(),
                                     List.of(randomImportOrganization(affiliationId)), creatorRole(), 1,
                                     randomBoolean());
    }

    public static ImportEntityDescription randomImportEntityDescription(PublicationContext publicationContext,
                                                                        List<ImportContributor> contributors) {
        return new ImportEntityDescription(randomString(), randomUri(),
                                           new PublicationDate.Builder().withYear("2020").build(), contributors,
                                           randomString(), emptyMap(), emptyList(), randomString(),
                                           createReference(publicationContext));
    }

    public static ImportContributor randomImportContributorWithName(String name) {
        return new ImportContributor(new Identity.Builder().withName(name).build(), emptyList(), creatorRole(), 1,
                                     randomBoolean());
    }

    private static RoleType creatorRole() {
        return new RoleType(Role.CREATOR);
    }

    private static ImportOrganization randomImportOrganization(URI organizationId) {
        return new ImportOrganization(Organization.fromUri(organizationId), ScopusAffiliation.emptyAffiliation());
    }

    private static Identity randomIdentityForContributorFromScopus() {
        return new Identity.Builder().withId(randomUri())
                   .withAdditionalIdentifiers(List.of(contributorScopusIdentifier()))
                   .build();
    }

    private static AdditionalIdentifier contributorScopusIdentifier() {
        return new AdditionalIdentifier("scopus-auid", randomString());
    }

    private static Reference createReference(PublicationContext publicationContext) {
        return new Reference.Builder().withDoi(randomDoi()).withPublishingContext(publicationContext).build();
    }
}
