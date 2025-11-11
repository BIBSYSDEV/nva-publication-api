package no.unit.nva.model.testing;

import static no.unit.nva.model.testing.PublicationContextBuilder.randomPublicationContext;
import static no.unit.nva.model.testing.PublicationInstanceBuilder.randomPublicationInstance;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedLink;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportStatusFactory;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
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

    public static ImportCandidate randomImportCandidate(PublicationContext publicationContext) {
        return new ImportCandidate.Builder().withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(randomEntityDescription(publicationContext))
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

    public static Contributor randomContributor() {
            return new Contributor.Builder().withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .withAffiliations(List.of(Organization.fromUri(randomUri())))
                       .withSequence(1)
                   .build();
    }

    private static EntityDescription randomEntityDescription(PublicationContext publicationContext) {
        return new EntityDescription.Builder().withPublicationDate(
                new PublicationDate.Builder().withYear("2020").build())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withContributors(List.of(randomContributor()))
                   .withMainTitle(randomString())
                   .withReference(createReference(publicationContext))
                   .build();
    }

    private static Reference createReference(PublicationContext publicationContext) {
        return new Reference.Builder().withDoi(randomDoi()).withPublishingContext(publicationContext).build();
    }
}
