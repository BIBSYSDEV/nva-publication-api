package no.unit.nva.publication.storage.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.storage.model.DoiRequest.DoiRequestBuilder;

class DoiRequestUtils {

    protected static Instant extractDoiRequestModifiedDate(no.unit.nva.model.DoiRequest doiRequestDto) {
        return Optional.ofNullable(doiRequestDto)
            .map(no.unit.nva.model.DoiRequest::getModifiedDate)
            .orElse(null);
    }

    protected static Instant extractDoiRequestCreatedDate(no.unit.nva.model.DoiRequest doiRequestDto) {
        return Optional.of(doiRequestDto)
            .map(no.unit.nva.model.DoiRequest::getCreatedDate)
            .orElse(null);
    }

    protected static DoiRequestBuilder extractDataFromResource(DoiRequestBuilder builder, Resource resource) {
        return builder
            .withResourceIdentifier(resource.getIdentifier())
            .withDoi(resource.getDoi())
            .withOwner(resource.getOwner())
            .withResourceModifiedDate(resource.getModifiedDate())
            .withResourcePublicationDate(extractPublicationDate(resource))
            .withCustomerId(resource.getCustomerId())
            .withResourcePublicationInstance(extractPublicationInstance(resource))
            .withResourcePublicationYear(extractPublicationYear(resource))
            .withResourceStatus(resource.getStatus())
            .withResourceTitle(extractMainTitle(resource))
            .withContributors(extractContributors(resource));
    }

    private static List<Contributor> extractContributors(Resource resource) {
        return
            Optional.ofNullable(resource.getEntityDescription())
                .map(EntityDescription::getContributors)
                .orElse(null);
    }

    private static PublicationInstance<? extends Pages> extractPublicationInstance(Resource resource) {
        return Optional.ofNullable(resource.getEntityDescription())
            .map(EntityDescription::getReference)
            .map(Reference::getPublicationInstance)
            .orElse(null);
    }

    private static PublicationDate extractPublicationDate(Resource resource) {
        return Optional.ofNullable(resource.getEntityDescription())
            .map(EntityDescription::getDate)
            .orElse(null);
    }

    private static String extractPublicationYear(Resource resource) {
        return Optional.ofNullable(resource.getEntityDescription())
            .map(EntityDescription::getDate)
            .map(PublicationDate::getYear)
            .orElse(null);
    }

    private static String extractMainTitle(Resource resource) {
        return Optional.of(resource)
            .map(Resource::getEntityDescription)
            .map(EntityDescription::getMainTitle)
            .orElse(null);
    }
}
