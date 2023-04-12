package no.unit.nva.publication.model.business;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class DoiRequestUtils {
    
    private DoiRequestUtils() {
    }
    
    static DoiRequest extractDataFromResource(DoiRequest doiRequest, Resource resource) {
        var copy = doiRequest.copy();
        copy.setPublicationDetails(PublicationDetails.create(resource));
        copy.setOwner(resource.getResourceOwner().getUser());
        copy.setCustomerId(resource.getCustomerId());
        copy.setResourceStatus(resource.getStatus());
        return copy;
    }
    
    static DoiRequest extractDataFromResource(Resource resource) {
        return extractDataFromResource(new DoiRequest(), resource);
    }
}
