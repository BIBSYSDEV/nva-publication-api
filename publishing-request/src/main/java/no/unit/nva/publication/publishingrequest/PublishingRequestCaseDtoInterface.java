package no.unit.nva.publication.publishingrequest;

public interface PublishingRequestCaseDtoInterface {

    default String getType() {
        return "PublishingRequestCase";
    }
}
