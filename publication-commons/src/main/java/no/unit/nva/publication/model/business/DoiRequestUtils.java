package no.unit.nva.publication.model.business;

import java.util.List;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;

public final class DoiRequestUtils {
    
    private DoiRequestUtils() {
    }
    
    static DoiRequest extractDataFromResource(DoiRequest doiRequest, Resource resource) {
        var copy = doiRequest.copy();
        copy.setResourceIdentifier(resource.getIdentifier());
        copy.setOwner(resource.getResourceOwner().getUser().toString());
        copy.setResourceModifiedDate(resource.getModifiedDate());
        copy.setResourcePublicationDate(extractPublicationDate(resource));
        copy.setCustomerId(resource.getCustomerId());
        copy.setResourcePublicationInstance(extractPublicationInstance(resource));
        copy.setResourcePublicationYear(extractPublicationYear(resource));
        copy.setResourceStatus(resource.getStatus());
        copy.setResourceTitle(extractMainTitle(resource));
        copy.setContributors(extractContributors(resource));
        return copy;
    }
    
    static DoiRequest extractDataFromResource(Resource resource) {
        return extractDataFromResource(new DoiRequest(), resource);
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
