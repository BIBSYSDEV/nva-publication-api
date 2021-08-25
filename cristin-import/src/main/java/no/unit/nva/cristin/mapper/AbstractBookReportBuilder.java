package no.unit.nva.cristin.mapper;

import no.unit.nva.model.pages.MonographPages;

public abstract class AbstractBookReportBuilder implements PublicationInstanceBuilder {


    protected MonographPages createMonographPages() {
        return new MonographPages.Builder()
            .withPages(extractNumberOfPages())
            .build();
    }

    private String extractNumberOfPages() {
        return getCristinObject().getBookOrReportMetadata().getNumberOfPages();
    }

    protected abstract CristinObject getCristinObject();
}
