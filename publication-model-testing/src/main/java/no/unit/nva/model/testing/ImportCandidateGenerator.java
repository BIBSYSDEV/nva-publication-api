package no.unit.nva.model.testing;

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
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.ScopusIdentifier;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;

public final class ImportCandidateGenerator {

    private ImportCandidateGenerator() {
    }

    public static ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder().withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(randomEntityDescription())
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withIdentifier(SortableIdentifier.next())
                   .withAdditionalIdentifiers(Set.of(ScopusIdentifier.fromValue(randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .build();
    }

    private static EntityDescription randomEntityDescription() {
        return new EntityDescription.Builder().withPublicationDate(
                new PublicationDate.Builder().withYear("2020").build())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withContributors(List.of(randomContributor()))
                   .withMainTitle(randomString())
                   .build();
    }

    private static Contributor randomContributor() {
        return new Contributor.Builder().withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .build();
    }
}
