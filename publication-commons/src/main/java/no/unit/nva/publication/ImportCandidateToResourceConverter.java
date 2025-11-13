package no.unit.nva.publication;

import java.util.List;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportContributor;
import no.unit.nva.importcandidate.ImportEntityDescription;
import no.unit.nva.importcandidate.ImportOrganization;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.core.JacocoGenerated;

//TODO Remove annotation once method is moved to module where it belongs NP-47690
@JacocoGenerated
public final class ImportCandidateToResourceConverter {

    private ImportCandidateToResourceConverter() {
    }

    public static Resource convert(ImportCandidate importCandidate) {
        return Resource.builder()
                   .withIdentifier(importCandidate.getIdentifier())
                   .withAdditionalIdentifiers(importCandidate.getAdditionalIdentifiers())
                   .withAssociatedArtifactsList(importCandidate.getAssociatedArtifacts())
                   .withEntityDescription(toEntityDescription(importCandidate))
                   .build();
    }

    public static EntityDescription toEntityDescription(ImportCandidate enrichedCandidate) {
        var entityDescription = enrichedCandidate.getEntityDescription();
        return new EntityDescription.Builder().withAbstract(entityDescription.mainAbstract())
                   .withDescription(entityDescription.description())
                   .withAlternativeAbstracts(entityDescription.alternativeAbstracts())
                   .withMainTitle(entityDescription.mainTitle())
                   .withLanguage(entityDescription.language())
                   .withTags(entityDescription.tags().stream().toList())
                   .withPublicationDate(entityDescription.publicationDate())
                   .withReference(entityDescription.reference())
                   .withContributors(convertToEntityDescriptionForResource(entityDescription))
                   .build();
    }

    private static List<Contributor> convertToEntityDescriptionForResource(ImportEntityDescription entityDescription) {
        return entityDescription.contributors()
                   .stream()
                   .map(ImportCandidateToResourceConverter::toContributor)
                   .toList();
    }

    private static Contributor toContributor(ImportContributor importContributor) {
        return new Contributor.Builder().withIdentity(importContributor.identity())
                   .withSequence(importContributor.sequence())
                   .withCorrespondingAuthor(importContributor.correspondingAuthor())
                   .withRole(importContributor.role())
                   .withAffiliations(getCorporations(importContributor))
                   .build();
    }

    private static List<Corporation> getCorporations(ImportContributor importContributor) {
        return importContributor.affiliations().stream().map(ImportOrganization::corporation).toList();
    }
}
