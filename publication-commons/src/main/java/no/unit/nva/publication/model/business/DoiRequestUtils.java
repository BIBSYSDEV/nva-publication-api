package no.unit.nva.publication.model.business;

public final class DoiRequestUtils {

    private DoiRequestUtils() {
    }

    static DoiRequest extractDataFromResource(DoiRequest doiRequest, Resource resource) {
        var copy = doiRequest.copy();
        copy.setResourceIdentifier(resource.getIdentifier());
        copy.setOwner(resource.getResourceOwner().getUser());
        copy.setCustomerId(resource.getCustomerId());
        copy.setResourceStatus(resource.getStatus());
        return copy;
    }

    static DoiRequest extractDataFromResource(Resource resource) {
        return extractDataFromResource(new DoiRequest(), resource);
    }
}
